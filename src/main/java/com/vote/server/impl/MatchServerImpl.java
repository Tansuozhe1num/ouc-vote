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
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MatchServerImpl implements MatchServer {
    private static final int BATCH_SIZE = 10;
    @Resource private StringRedisTemplate redis;
    @Resource private SnapInfoMapper<SnapInfo, SnapInfoQuery> snapMapper;

    @Override
    public SnapInfo draw(Long userId) {
        String key = "vote:UserContact:" + userId;
        for (int round = 0; round < 2; round++) {
            String id = redis.opsForList().leftPop(key);
            if (id == null) {
            Integer maxId = snapMapper.selectMaxSnapId();
            if (maxId == null || maxId <= 0) throw new BusinessException("暂时还没有可匹配的搭子");
            List<Integer> ids = new ArrayList<>();
            int desired = Math.min(BATCH_SIZE, maxId);
            for (int tries = 0; ids.size() < desired && tries < desired * 8; tries++) {
                int candidate = ThreadLocalRandom.current().nextInt(1, maxId + 1);
                if (!ids.contains(candidate)) ids.add(candidate);
            }
            List<SnapInfo> candidates = snapMapper.selectByIds(ids);
            for (SnapInfo candidate : candidates) {
                if (!userId.equals(candidate.getUserId())) redis.opsForList().leftPush(key, String.valueOf(candidate.getSnapId()));
            }
            id = redis.opsForList().leftPop(key);
            if (id == null) throw new BusinessException("暂时没有其他同学正在找搭子");
            }
            SnapInfo result = snapMapper.selectBySnapId(Integer.valueOf(id));
            if (result != null && !userId.equals(result.getUserId())) return result;
        }
        throw new BusinessException("暂时没有其他同学正在找搭子");
    }
}
