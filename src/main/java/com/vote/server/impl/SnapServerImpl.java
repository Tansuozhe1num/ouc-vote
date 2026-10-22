package com.vote.server.impl;

import com.vote.data.constants.Constants;
import com.vote.data.enums.SnapInfoStatusEnum;
import com.vote.data.po.SnapInfo;
import com.vote.data.po.User;
import com.vote.exception.BusinessException;
import com.vote.mappers.SnapInfoMapper;
import com.vote.mappers.UserMapper;
import com.vote.oss.OssServer;
import com.vote.query.SnapInfoQuery;
import com.vote.server.SnapServer;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class SnapServerImpl implements SnapServer {
    private static final int MAX_PAGE_NUMBER = 100000;
    private static final String IDEMPOTENCY_PREFIX = "vote:SnapSaveIdempotency:";
    private static final String PROCESSING = "PROCESSING";
    private static final long IDEMPOTENCY_TTL_MINUTES = 10L;

    @Resource private SnapInfoMapper<SnapInfo, SnapInfoQuery> snapInfoMapper;
    @Resource private OssServer ossServer;
    @Resource private UserMapper userMapper;
    @Resource private StringRedisTemplate redis;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private SnapScoreService snapScoreService;

    @Override
    public List<SnapInfo> getSnapInfoList(Integer page, Integer size, Integer type) {
        if (page == null || page <= 0 || page > 100) page = 1;
        if (size == null || size <= 0 || size > 30) size = 12;
        int offset = (page - 1) * size;
        return type == null || type < 0 || type > 4
                ? snapInfoMapper.selectSnapInfoList(size, offset)
                : snapInfoMapper.selectSnapInfoListByType(size, offset, type);
    }

    @Override
    public SnapInfo getSnapInfoById(Integer snapId) {
        if (snapId == null || snapId <= 0) throw new BusinessException("snapId不能为空");
        SnapInfo snapInfo = snapInfoMapper.selectBySnapId(snapId);
        if (snapInfo == null) throw new BusinessException("内容不存在");
        return snapInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SnapInfo saveSnapInfo(Long userId, MultipartFile avatar, String title, String desc, Integer type, String idempotencyKey) {
        User user = userMapper.selectByUserId(userId);
        if (userId == null || user == null || user.getStatus() == null || user.getStatus() != 0) {
            throw new BusinessException("用户不存在");
        }
        if (!StringUtils.hasText(title)) {
            throw new BusinessException("请填写标题");
        }
        if (title.trim().length() > 100) {
            throw new BusinessException("标题不能超过 100 字");
        }
        if (!StringUtils.hasText(desc)) {
            throw new BusinessException("请写下你想找的搭子");
        }
        if (desc.length() > 500)  {
            throw new BusinessException("内容不能超过 500 字");
        }
        if (type == null || type < 0 || type > 4) {
            throw new BusinessException("搭子类型无效");
        }

        String requestKey = StringUtils.hasText(idempotencyKey) ? idempotencyKey.trim() : UUID.randomUUID().toString();
        if (requestKey.length() > 128) {
            throw new BusinessException("发布请求标识无效");
        }
        String redisKey = IDEMPOTENCY_PREFIX + userId + ":" + requestKey;
        Boolean firstRequest = redis.opsForValue().setIfAbsent(redisKey, PROCESSING, IDEMPOTENCY_TTL_MINUTES, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(firstRequest)) {
            String savedSnapId = redis.opsForValue().get(redisKey);
            if (savedSnapId != null && !PROCESSING.equals(savedSnapId)) {
                try {
                    SnapInfo existing = snapInfoMapper.selectBySnapId(Integer.valueOf(savedSnapId));
                    if (existing != null && userId.equals(existing.getUserId())) {
                        return existing;
                    }
                } catch (NumberFormatException ignored) {
                    // Treat an invalid cached value as an in-flight request.
                }
            }
            throw new BusinessException("发布请求正在处理，请勿重复提交");
        }

        try {
            String url = null;
            if (avatar != null && !avatar.isEmpty()) {
                try {
                    url = ossServer.upload(avatar);
                } catch (Exception e) {
                    throw new BusinessException("图片上传失败", e);
                }
            }
            SnapInfo snapInfo = SnapInfo.builder()
                    .userId(userId)
                    .snapImage(url)
                    .title(title.trim())
                    .desc(desc.trim())
                    .type(type)
                    .ctime(System.currentTimeMillis())
                    .score(Constants.DefaultRankScore)
                    .status(SnapInfoStatusEnum.Normal.getStatus()).build();
            snapInfoMapper.insert(snapInfo);

            snapScoreService.syncRankAfterCommit(snapInfo.getSnapId(), snapInfo.getScore(), snapInfo.getStatus());

            redis.opsForValue().set(redisKey, String.valueOf(snapInfo.getSnapId()), IDEMPOTENCY_TTL_MINUTES, TimeUnit.MINUTES);
            return snapInfo;
        } catch (RuntimeException e) {
            // Allow a retry when validation, upload, or database insertion failed.
            redis.delete(redisKey);
            throw e;
        }
    }

    @Override
    public List<SnapInfo> getMySnapInfoList(Long userId, Integer page, Integer size) {
        if (page == null || page < 1) page = 1;
        else page = Math.min(page, MAX_PAGE_NUMBER);
        if (size == null || size < 1 || size > 50) size = 30;
        return snapInfoMapper.selectMine(userId, size, (page - 1) * size);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long userId, Integer snapId, Integer status) {
        if (status == null || status < 0 || status > 2) throw new BusinessException("状态值无效");
        SnapInfo snapInfo = snapInfoMapper.adminSelectBySnapId(snapId);
        if (snapInfo == null || !userId.equals(snapInfo.getUserId())) throw new BusinessException("内容不存在或无权修改");
        Integer changed = snapInfoMapper.updateStatus(snapId, userId, status);
        if (changed == null || changed == 0) throw new BusinessException("内容不存在或无权修改");
        snapScoreService.syncRankAfterCommit(snapId, snapInfo.getScore(), status);
        return true;
    }

    @Override
    public List<SnapInfo> getHotRank() {
        RScoredSortedSet<String> set = redissonClient.getScoredSortedSet(Constants.VOTERANK);
        Collection<ScoredEntry<String>> top10 =
                set.entryRangeReversed(0, 9);

        List<Integer> ids = new ArrayList<>();
        for (ScoredEntry<String> entry : top10) {
            String member = entry.getValue();
            if (member == null || !member.startsWith(Constants.SNAPSCORE)) continue;
            try {
                ids.add(Integer.valueOf(member.substring(Constants.SNAPSCORE.length())));
            } catch (NumberFormatException ignored) {
                // Ignore stale or malformed members left in Redis.
            }
        }

        if (ids.isEmpty()) return Collections.emptyList();
        List<SnapInfo> snapInfos = this.snapInfoMapper.selectByIds(ids);
        return snapInfos;
    }
}
