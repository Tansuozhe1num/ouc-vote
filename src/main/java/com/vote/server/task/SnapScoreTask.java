package com.vote.server.task;

import com.vote.data.constants.Constants;
import com.vote.data.po.SnapInfo;
import com.vote.mappers.SnapInfoMapper;
import com.vote.query.SnapInfoQuery;
import com.vote.server.impl.SnapScoreService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class SnapScoreTask {
    private static final int DECAY_SCORE = 20;
    private static final int BATCH_SIZE = 500;

    @Resource
    private SnapInfoMapper<SnapInfo, SnapInfoQuery> snapInfoMapper;

    @Resource
    private SnapScoreService snapScoreService;

    @Resource
    private RedissonClient redissonClient;

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Shanghai")
    public void refreshScore() {
        RLock lock = redissonClient.getLock(Constants.VOTERANK + ":daily-decay-lock");
        boolean acquired = false;
        try {
            acquired = lock.tryLock(0, TimeUnit.SECONDS);
            if (!acquired) {
                log.info("snap 分数衰减任务已由其他实例执行，跳过本次");
                return;
            }

            long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3);
            int affected = snapInfoMapper.decreaseAgedScores(cutoff, DECAY_SCORE);
            if (affected == 0) {
                log.info("没有超过三天的 snap 需要衰减");
                return;
            }

            long lastCtime = Long.MIN_VALUE;
            int lastSnapId = 0;
            long synced = 0;
            while (true) {
                List<SnapInfo> batch = snapInfoMapper.selectAgedActiveSnaps(cutoff, lastCtime, lastSnapId, BATCH_SIZE);
                if (batch == null || batch.isEmpty()) break;

                for (SnapInfo snap : batch) {
                    snapScoreService.applyAgedDecay(snap.getSnapId(), snap.getScore());
                    lastCtime = snap.getCtime();
                    lastSnapId = snap.getSnapId();
                    synced++;
                }
                if (batch.size() < BATCH_SIZE) break;
            }
            log.info("snap 分数日衰减完成，MySQL 更新 {} 条，Redis 同步 {} 条", affected, synced);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("snap 分数衰减任务被中断", e);
        } catch (RuntimeException e) {
            log.error("snap 分数日衰减任务失败", e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) lock.unlock();
        }
    }
}
