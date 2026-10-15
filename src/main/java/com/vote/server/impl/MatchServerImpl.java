package com.vote.server.impl;

import com.vote.data.po.SnapInfo;
import com.vote.exception.BusinessException;
import com.vote.mappers.SnapInfoMapper;
import com.vote.query.SnapInfoQuery;
import com.vote.server.MatchServer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MatchServerImpl implements MatchServer {
    private static final int BATCH_SIZE = 10;
    private static final int RANDOM_ATTEMPTS_PER_SLOT = 8;
    @Resource private StringRedisTemplate redis;
    @Resource private SnapInfoMapper<SnapInfo, SnapInfoQuery> snapMapper;

    @Override
    public SnapInfo draw(Long userId) {
        if (userId == null) throw new BusinessException("用户不能为空");
        String key = "vote:UserContact:" + userId;
        for (int round = 0; round < 2; round++) {
            String id = redis.opsForList().leftPop(key);
            if (id == null) {
                Integer maxId = snapMapper.selectMaxSnapId();
                if (maxId == null || maxId <= 0) throw new BusinessException("暂时还没有可匹配的搭子");
                List<Integer> ids = randomIds(maxId, Math.min(BATCH_SIZE, maxId));
                for (SnapInfo candidate : snapMapper.selectByIds(ids)) {
                    if (!userId.equals(candidate.getUserId())) {
                        redis.opsForList().leftPush(key, String.valueOf(candidate.getSnapId()));
                    }
                }
                id = redis.opsForList().leftPop(key);
                if (id == null) throw new BusinessException("暂时没有其他同学正在找搭子");
            }
            SnapInfo result;
            try {
                result = snapMapper.selectBySnapId(Integer.valueOf(id));
            } catch (NumberFormatException ignored) {
                result = null;
            }
            if (result != null && !userId.equals(result.getUserId())) return result;
        }
        throw new BusinessException("暂时没有其他同学正在找搭子");
    }

    private List<Integer> randomIds(int maxId, int desired) {
        Set<Integer> unique = new HashSet<>(desired * 2);
        int attempts = 0;
        int maxAttempts = desired * RANDOM_ATTEMPTS_PER_SLOT;
        while (unique.size() < desired && attempts++ < maxAttempts) {
            unique.add(ThreadLocalRandom.current().nextInt(maxId) + 1);
        }
        return new ArrayList<>(unique);
    }
}
