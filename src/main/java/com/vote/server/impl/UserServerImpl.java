package com.vote.server.impl;

import com.vote.auth.JwtService;
import com.vote.data.po.User;
import com.vote.exception.BusinessException;
import com.vote.mappers.UserMapper;
import com.vote.server.UserServer;
import com.vote.util.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import javax.annotation.Resource;
import java.util.UUID;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

@Service
public class UserServerImpl implements UserServer {
    @Resource private UserMapper userMapper;
    @Resource private SnowflakeIdGenerator idGenerator;
    @Resource private JwtService jwtService;
    @Value("${campus.wechat.app-id:}") private String wechatAppId;
    @Value("${campus.wechat.app-secret:}") private String wechatAppSecret;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public User register(String loginName, String password, String name, String description, Integer sex, String major) {
        if (!StringUtils.hasText(loginName) || !StringUtils.hasText(password)) throw new BusinessException("账号和密码不能为空");
        if (password.length() < 6) {
            throw new BusinessException("密码至少 6 位");
        }
        if (userMapper.selectByLoginName(loginName.trim()) != null) {
            throw new BusinessException("账号已注册");
        }
        User u = this.userMapper.selectByName(name);
        if (u != null) {
            throw new BusinessException("昵称已被使用");
        }
        User user = User.builder().userId(idGenerator.nextId()).loginName(loginName.trim())
                .passwordHash(hashPassword(password)).name(StringUtils.hasText(name) ? name.trim() : loginName.trim())
                .description(description).sex(sex).major(major).status(0)
                .ctime((int) (System.currentTimeMillis() / 1000)).build();
        userMapper.insert(user);
        return createSession(user);
    }

    @Override
    public User login(String loginName, String password) {
        User user = userMapper.selectByLoginName(loginName == null ? null : loginName.trim());
        if (user == null || user.getStatus() == null || user.getStatus() != 0 || !StringUtils.hasText(user.getPasswordHash())
                || !matchesPassword(password == null ? "" : password, user.getPasswordHash())) {
            throw new BusinessException("账号或密码错误");
        }
        return createSession(user);
    }

    @Override
    public User wechatLogin(String code) {
        String openid = resolveWechatOpenid(code);
        if (!StringUtils.hasText(openid)) throw new BusinessException("微信授权码无效");
        User user = userMapper.selectByWechatOpenid(openid);
        if (user == null) {
            user = User.builder().userId(idGenerator.nextId()).loginName("wx_" + openid)
                    .wechatOpenid(openid).name("微信同学").description("在校内找一个聊得来的人")
                    .sex(0).status(0).ctime((int) (System.currentTimeMillis() / 1000)).build();
            userMapper.insert(user);
        }
        return createSession(user);
    }

    @Override
    public void bindWechat(Long userId, String code) {
        User user = require(userId);
        String openid = resolveWechatOpenid(code);
        if (!StringUtils.hasText(openid)) throw new BusinessException("微信授权码无效");
        User existing = userMapper.selectByWechatOpenid(openid);
        if (existing != null && !existing.getUserId().equals(userId)) throw new BusinessException("该微信已绑定其他账号");
        userMapper.bindWechat(userId, openid);
    }

    @Override public User getUser(Long userId) { return require(userId); }

    @Override
    public User updateProfile(Long userId, String name, String description, Integer sex, String major) {
        User user = require(userId);
        if (sex != null && sex != -1 && sex != 0 && sex != 1) throw new BusinessException("性别选项无效");
        if (StringUtils.hasText(name)) user.setName(name.trim());
        if (description != null) user.setDescription(description.length() > 500 ? description.substring(0, 500) : description);
        if (sex != null) user.setSex(sex == -1 ? null : sex);
        if (major != null) user.setMajor(major);
        userMapper.updateProfile(user);
        return user;
    }

    private User require(Long userId) {
        User user = userMapper.selectByUserId(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 0) throw new BusinessException("用户不存在或已注销");
        return user;
    }

    private User createSession(User user) {
        String token = jwtService.createToken(user.getUserId());
        String refresh = UUID.randomUUID().toString().replace("-", "");
        user.setToken(token); user.setRefreshtoken(refresh);
        userMapper.updateToken(user.getUserId(), token, refresh);
        return user;
    }

    private String resolveWechatOpenid(String code) {
        if (!StringUtils.hasText(code)) return null;
        if (!StringUtils.hasText(wechatAppId) || !StringUtils.hasText(wechatAppSecret)) {
            throw new BusinessException("微信登录尚未配置");
        }
        try {
            String url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + wechatAppId
                    + "&secret=" + wechatAppSecret + "&code=" + code + "&grant_type=authorization_code";
            String body = new RestTemplate().getForObject(url, String.class);
            JSONObject json = JSON.parseObject(body);
            return json == null ? null : json.getString("openid");
        } catch (Exception e) {
            throw new BusinessException("微信服务暂时不可用", e);
        }
    }

    private String hashPassword(String password) {
        try {
            byte[] salt = new byte[16]; secureRandom.nextBytes(salt);
            byte[] hash = derive(password, salt);
            return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) { throw new IllegalStateException("密码加密失败", e); }
    }

    private boolean matchesPassword(String password, String encoded) {
        try {
            String[] parts = encoded == null ? new String[0] : encoded.split(":");
            if (parts.length != 2) return false;
            byte[] actual = derive(password, Base64.getDecoder().decode(parts[0]));
            return MessageDigest.isEqual(actual, Base64.getDecoder().decode(parts[1]));
        } catch (Exception e) { return false; }
    }

    private byte[] derive(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 120000, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        finally { spec.clearPassword(); }
    }
}
