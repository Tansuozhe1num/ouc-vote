package com.vote.server.impl;

import com.vote.data.constants.Constants;
import com.vote.data.po.SnapInfo;
import com.vote.exception.BusinessException;
import com.vote.mappers.SnapInfoMapper;
import com.vote.query.SnapInfoQuery;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;

@Service
public class SnapScoreService {
    private static final Logger logger = LoggerFactory.getLogger(SnapScoreService.class);

    @Resource
    private SnapInfoMapper<SnapInfo, SnapInfoQuery> snapInfoMapper;

    @Resource
    private RedissonClient redissonClient;

    /** Persist the score change first, then update the Redis ranking after the SQL transaction commits. */
    public void adjustScore(Integer snapId, Integer delta) {
        if (delta == null || delta == 0) return;
        Integer changed = snapInfoMapper.addScore(snapId, delta);
        if (changed == null || changed == 0) throw new BusinessException("内容不存在或热度更新失败");

        afterCommit(() -> {
            RScoredSortedSet<String> ranks = ranks();
            String member = member(snapId);
            if (ranks.getScore(member) == null) {
                SnapInfo snap = snapInfoMapper.selectBySnapId(snapId);
                if (snap != null && snap.getScore() != null) ranks.add(snap.getScore(), member);
            } else {
                ranks.addScore(member, delta);
            }
        });
    }

    /** Keep the ranked-set membership and exact score aligned with the committed snap state. */
    public void syncRankAfterCommit(Integer snapId, Integer score, Integer status) {
        if (snapId == null || score == null) return;
        afterCommit(() -> {
            RScoredSortedSet<String> ranks = ranks();
            String member = member(snapId);
            if (status != null && status == 0) ranks.add(score, member);
            else ranks.remove(member);
        });
    }

    /** Apply the nightly decay to a ranked snap, repairing a missing member from its stored score. */
    public void applyAgedDecay(Integer snapId, Integer currentScore) {
        if (snapId == null || currentScore == null) return;
        RScoredSortedSet<String> ranks = ranks();
        String member = member(snapId);
        if (ranks.getScore(member) == null) ranks.add(currentScore, member);
        else ranks.addScore(member, -20);
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runRedisAction(action);
                }
            });
        } else {
            runRedisAction(action);
        }
    }

    private void runRedisAction(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            // MySQL remains the durable score source; the scheduled job can repair Redis later.
            logger.warn("同步 snap 排行分数到 Redis 失败", e);
        }
    }

    private RScoredSortedSet<String> ranks() {
        return redissonClient.getScoredSortedSet(Constants.VOTERANK);
    }

    private String member(Integer snapId) {
        return Constants.SNAPSCORE + snapId;
    }
}
