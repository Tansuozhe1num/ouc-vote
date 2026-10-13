package com.vote.auth;

import com.vote.exception.BusinessException;

import javax.servlet.http.HttpServletRequest;

public final class AuthContext {
    public static final String USER_ID_ATTRIBUTE = "campusUserId";

    private AuthContext() { }

    public static Long userId(HttpServletRequest request) {
        Object value = request == null ? null : request.getAttribute(USER_ID_ATTRIBUTE);
        return value instanceof Long ? (Long) value : null;
    }

    public static Long requireUserId(HttpServletRequest request) {
        Long userId = userId(request);
        if (userId == null) throw new BusinessException(401, "请先登录");
        return userId;
    }
}
