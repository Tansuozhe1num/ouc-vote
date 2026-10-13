package com.vote.controller;

import com.vote.auth.AuthContext;
import com.vote.data.vo.ResponseVO;
import com.vote.server.UserServer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/user")
public class UserController extends ABaseController {
    @Resource private UserServer userServer;

    @GetMapping("/profile")
    public ResponseVO profile(@RequestParam Long userId) {
        return getSuccessResponseVO(publicUser(userServer.getUser(userId)));
    }

    @PostMapping("/profile")
    public ResponseVO updateProfile(String name, String description, Integer sex, String major, HttpServletRequest request) {
        return getSuccessResponseVO(publicUser(userServer.updateProfile(AuthContext.requireUserId(request), name, description, sex, major)));
    }

    private com.vote.data.po.User publicUser(com.vote.data.po.User user) {
        user.setToken(null); user.setRefreshtoken(null); user.setPasswordHash(null); user.setLoginName(null); user.setWechatOpenid(null);
        return user;
    }
}
