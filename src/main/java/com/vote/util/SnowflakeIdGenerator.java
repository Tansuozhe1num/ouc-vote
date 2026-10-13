package com.vote.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Small, process-safe Snowflake generator. Worker ids must be unique per node. */
@Component
public class SnowflakeIdGenerator {
    private static final long EPOCH = 1704067200000L;
    private static final long SEQUENCE_MASK = 4095L;
    private final long workerId;
    private long lastTimestamp = -1L;
    private long sequence;

    public SnowflakeIdGenerator(@Value("${campus.snowflake.worker-id:1}") long workerId) {
        if (workerId < 0 || workerId > 1023) {
            throw new IllegalArgumentException("campus.snowflake.worker-id must be between 0 and 1023");
        }
        this.workerId = workerId;
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("system clock moved backwards");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitNextMillis(timestamp);
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << 22) | (workerId << 12) | sequence;
    }

    private long waitNextMillis(long timestamp) {
        long current = System.currentTimeMillis();
        while (current <= timestamp) {
            Thread.yield();
            current = System.currentTimeMillis();
        }
        return current;
    }
}
