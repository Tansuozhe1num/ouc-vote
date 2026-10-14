package com.vote.admin;

import com.vote.auth.AuthContext;
import com.vote.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Admin access is deliberately configured outside the user profile schema.
 * This keeps existing installations compatible while allowing an operator to
 * grant access through CAMPUS_ADMIN_API_KEY or CAMPUS_ADMIN_USER_IDS.
 */
@Service
public class AdminAuthService {
    public static final String API_KEY_HEADER = "X-Admin-Key";

    @Value("${campus.admin.api-key:}")
    private String apiKey;

    @Value("${campus.admin.user-ids:}")
    private String userIds;

    public boolean isKeyConfigured() {
        return StringUtils.hasText(apiKey);
    }

    public boolean isAuthenticated(HttpServletRequest request) {
        String supplied = request == null ? null : request.getHeader(API_KEY_HEADER);
        if (isValidKey(supplied)) return true;
        Long userId = AuthContext.userId(request);
        return userId != null && adminIds().contains(userId);
    }

    public boolean isValidKey(String supplied) {
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(supplied)) return false;
        byte[] expected = apiKey.trim().getBytes(StandardCharsets.UTF_8);
        byte[] actual = supplied.trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    public void require(HttpServletRequest request) {
        if (!isAuthenticated(request)) {
            throw new BusinessException(403, "没有管理员权限");
        }
    }

    public Set<Long> adminIds() {
        if (!StringUtils.hasText(userIds)) return Collections.emptySet();
        Set<Long> ids = new HashSet<>();
        for (String value : userIds.split(",")) {
            try {
                if (StringUtils.hasText(value)) ids.add(Long.valueOf(value.trim()));
            } catch (NumberFormatException ignored) {
                // Ignore malformed configuration entries and keep valid IDs.
            }
        }
        return ids;
    }
}
