package com.vote.auth;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class JwtService {
    private final String secret;
    private final long ttlSeconds;

    public JwtService(@Value("${campus.jwt.secret}") String secret,
                      @Value("${campus.jwt.ttl-seconds:7200}") long ttlSeconds) {
        this.secret = secret;
        this.ttlSeconds = ttlSeconds;
    }

    public String createToken(Long userId) {
        long exp = System.currentTimeMillis() / 1000 + ttlSeconds;
        String header = base64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String body = base64(JSON.toJSONString(new JwtPayload(String.valueOf(userId), exp)));
        return header + "." + body + "." + sign(header + "." + body);
    }

    public Long parseUserId(String token) {
        try {
            if (token == null) return null;
            String[] pieces = token.split("\\.");
            if (pieces.length != 3 || !sign(pieces[0] + "." + pieces[1]).equals(pieces[2])) return null;
            JSONObject body = JSON.parseObject(new String(Base64.getUrlDecoder().decode(pieces[1]), StandardCharsets.UTF_8));
            long exp = body.getLongValue("exp");
            if (exp < System.currentTimeMillis() / 1000) return null;
            return Long.valueOf(body.getString("sub"));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String base64(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("cannot sign token", e);
        }
    }

    private static class JwtPayload {
        public final String sub;
        public final long exp;
        JwtPayload(String sub, long exp) { this.sub = sub; this.exp = exp; }
    }
}
