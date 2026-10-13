package com.vote.server;

import com.vote.data.po.User;

public interface UserServer {
    User register(String loginName, String password, String name, String description, Integer sex, String major);
    User login(String loginName, String password);
    User wechatLogin(String code);
    void bindWechat(Long userId, String code);
    User getUser(Long userId);
    User updateProfile(Long userId, String name, String description, Integer sex, String major);
}
