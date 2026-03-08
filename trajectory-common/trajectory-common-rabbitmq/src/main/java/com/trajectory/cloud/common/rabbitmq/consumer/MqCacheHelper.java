package com.trajectory.cloud.common.rabbitmq.consumer;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * MQ 专用缓存辅助工具
 * <p>
 * 集成了基于 Redis 的分布式幂等锁、重试计数器等核心能力。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class MqCacheHelper {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 尝试获取幂等锁（去重）
     *
     * @param key    唯一的幂等 Key
     * @param expire 过期时间（秒）
     * @return true 代表加锁成功（即未重复消费），false 代表已存在（重复）
     */
    public boolean setIfAbsent(String key, int expire) {
        // 利用 Redis 的 SETNX 特性，并增加随机偏移防止过期时间雪崩
        int finalExpire = expire + RandomUtil.randomInt(0, 600);
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        return BooleanUtil.isTrue(bucket.setIfAbsent("1", java.time.Duration.ofSeconds(finalExpire)));
    }

    /**
     * 增加重试次数
     *
     * @param key 重试计数的 Key
     * @return 增加后的计数值
     */
    public long incrementRetryCount(String key) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        return atomicLong.incrementAndGet();
    }

    /**
     * 获取重试次数
     *
     * @param key 重试计数的 Key
     * @return 当前计数值
     */
    public int getRetryCount(String key) {
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        String val = bucket.get();
        return val != null ? Integer.parseInt(val) : 0;
    }

    /**
     * 清理缓存（如重试成功后清理计数）
     *
     * @param key 缓存 Key
     */
    public void delete(String key) {
        redissonClient.getBucket(key).delete();
    }
}
