package com.vote.controller;

import com.vote.auth.AuthContext;
import com.vote.data.vo.ResponseVO;
import com.vote.server.FollowServer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/follow")
public class FollowController extends ABaseController {
    @Resource private FollowServer followServer;

    @PostMapping("/toggle")
    public ResponseVO toggle(@RequestParam Long userId, HttpServletRequest request) {
        return getSuccessResponseVO(followServer.toggle(AuthContext.requireUserId(request), userId));
    }

    @GetMapping("/status")
    public ResponseVO status(@RequestParam Long userId, HttpServletRequest request) {
        return getSuccessResponseVO(followServer.isFollowing(AuthContext.requireUserId(request), userId));
    }

    @GetMapping("/following")
    public ResponseVO following(HttpServletRequest request) {
        return getSuccessResponseVO(publicUsers(followServer.following(AuthContext.requireUserId(request))));
    }

    @GetMapping("/followers")
    public ResponseVO followers(HttpServletRequest request) {
        return getSuccessResponseVO(publicUsers(followServer.followers(AuthContext.requireUserId(request))));
    }

    private java.util.List<com.vote.data.po.User> publicUsers(java.util.List<com.vote.data.po.User> users) {
        for (com.vote.data.po.User user : users) {
            user.setToken(null); user.setRefreshtoken(null); user.setPasswordHash(null); user.setLoginName(null); user.setWechatOpenid(null);
        }
        return users;
    }
}
