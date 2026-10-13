package com.vote.controller;

import com.vote.auth.AuthContext;
import com.vote.data.po.User;
import com.vote.data.vo.ResponseVO;
import com.vote.server.CaptchaServer;
import com.vote.server.UserServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/auth")
public class AuthController extends ABaseController {
    @Resource private UserServer userServer;
    @Resource private CaptchaServer captchaServer;
    @Value("${campus.wechat.app-id:}") private String wechatAppId;
    @Value("${campus.wechat.redirect-uri:http://localhost:5050/auth/wechat/callback}") private String wechatRedirectUri;
    @Value("${campus.jwt.ttl-seconds:7200}") private int ttlSeconds;

    @GetMapping("/captcha")
    public ResponseVO captcha(HttpServletRequest request) {
        return getSuccessResponseVO(captchaServer.issue(clientKey(request)));
    }

    @PostMapping("/register")
    public ResponseVO register(@RequestParam String loginName, @RequestParam String password,
                               String name, String description, Integer sex, String major,
                               @RequestParam String captchaKey, @RequestParam String captchaCode,
                               HttpServletRequest request, HttpServletResponse response) {
        captchaServer.verify(captchaKey, captchaCode);
        return session(userServer.register(loginName, password, name, description, sex, major), request, response);
    }

    @PostMapping("/login")
    public ResponseVO login(@RequestParam String loginName, @RequestParam String password,
                            @RequestParam String captchaKey, @RequestParam String captchaCode,
                            HttpServletRequest request, HttpServletResponse response) {
        captchaServer.verify(captchaKey, captchaCode);
        return session(userServer.login(loginName, password), request, response);
    }

    @PostMapping("/wechat/login")
    public ResponseVO wechatLogin(@RequestParam String code, @RequestParam String captchaKey,
                                  @RequestParam String captchaCode, HttpServletRequest request, HttpServletResponse response) {
        captchaServer.verify(captchaKey, captchaCode);
        return session(userServer.wechatLogin(code), request, response);
    }

    @GetMapping("/wechat/start")
    public void wechatStart(@RequestParam String captchaKey, @RequestParam String captchaCode, HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
        captchaServer.verify(captchaKey, captchaCode);
        if (wechatAppId == null || wechatAppId.trim().isEmpty()) throw new com.vote.exception.BusinessException("微信登录尚未配置");
        String state = java.util.UUID.randomUUID().toString().replace("-", "");
        request.getSession(true).setAttribute("wechatState", state);
        String url = "https://open.weixin.qq.com/connect/qrconnect?appid=" + UriUtils.encodeQueryParam(wechatAppId, StandardCharsets.UTF_8.name())
                + "&redirect_uri=" + UriUtils.encodeQueryParam(wechatRedirectUri, StandardCharsets.UTF_8.name())
                + "&response_type=code&scope=snsapi_login&state=" + state + "#wechat_redirect";
        response.sendRedirect(url);
    }

    @GetMapping("/wechat/callback")
    public void wechatCallback(@RequestParam String code, @RequestParam String state, HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
        HttpSession session = request.getSession(false);
        Object expected = session == null ? null : session.getAttribute("wechatState");
        if (expected == null || !expected.equals(state)) throw new com.vote.exception.BusinessException("微信登录状态验证失败");
        session.removeAttribute("wechatState");
        session(userServer.wechatLogin(code), request, response);
        response.sendRedirect("/#/feed");
    }

    @PostMapping("/wechat/bind")
    public ResponseVO bindWechat(@RequestParam String code, HttpServletRequest request) {
        userServer.bindWechat(AuthContext.requireUserId(request), code);
        return getSuccessResponseVO(null);
    }

    @GetMapping("/me")
    public ResponseVO me(HttpServletRequest request) {
        return getSuccessResponseVO(publicUser(userServer.getUser(AuthContext.requireUserId(request))));
    }

    @PostMapping("/logout")
    public ResponseVO logout(HttpServletRequest request, HttpServletResponse response) {
        clearCookie(request, response);
        return getSuccessResponseVO(null);
    }

    private ResponseVO session(User user, HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie("campus_token", user.getToken());
        cookie.setHttpOnly(true); cookie.setSecure(request.isSecure()); cookie.setPath("/"); cookie.setMaxAge(ttlSeconds);
        response.addCookie(cookie);
        return getSuccessResponseVO(publicUser(user));
    }

    private User publicUser(User user) {
        if (user != null) {
            user.setToken(null); user.setRefreshtoken(null); user.setPasswordHash(null);
            user.setLoginName(null); user.setWechatOpenid(null);
        }
        return user;
    }

    private void clearCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie("campus_token", "");
        cookie.setHttpOnly(true); cookie.setSecure(request.isSecure()); cookie.setPath("/"); cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String clientKey(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.trim().isEmpty()) ip = request.getRemoteAddr();
        if (ip != null && ip.contains(",")) ip = ip.substring(0, ip.indexOf(","));
        return ip == null ? "unknown" : ip.trim();
    }
}
