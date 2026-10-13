package com.vote.server;

import com.wf.captcha.SpecCaptcha;
import com.vote.exception.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class CaptchaServer {
    @Resource private StringRedisTemplate redis;

    public CaptchaResult issue(String clientKey) {
        String rateKey = "vote:CaptchaRate:" + clientKey;
        Long count = redis.opsForValue().increment(rateKey);
        if (count != null && count == 1) redis.expire(rateKey, 60, TimeUnit.SECONDS);
        if (count != null && count > 30) throw new BusinessException("验证码请求过于频繁，请稍后再试");
        SpecCaptcha captcha = new SpecCaptcha(140, 48, 4);
        String key = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set("vote:Captcha:" + key, captcha.text().toLowerCase(Locale.ROOT), 5, TimeUnit.MINUTES);
        return new CaptchaResult(key, captcha.toBase64());
    }

    public void verify(String key, String answer) {
        if (key == null || answer == null) throw new BusinessException("请先完成图形验证码");
        String redisKey = "vote:Captcha:" + key;
        String attemptsKey = "vote:CaptchaAttempts:" + key;
        Long attempts = redis.opsForValue().increment(attemptsKey);
        if (attempts != null && attempts == 1) redis.expire(attemptsKey, 5, TimeUnit.MINUTES);
        if (attempts != null && attempts > 5) { redis.delete(redisKey); throw new BusinessException("验证码尝试次数过多，请刷新"); }
        String expected = redis.opsForValue().get(redisKey);
        redis.delete(redisKey);
        if (expected == null || !expected.equals(answer.trim().toLowerCase(Locale.ROOT))) {
            throw new BusinessException("验证码错误或已过期");
        }
    }

    public static class CaptchaResult {
        private final String key;
        private final String image;
        public CaptchaResult(String key, String image) { this.key = key; this.image = image; }
        public String getKey() { return key; }
        public String getImage() { return image; }
    }
}
