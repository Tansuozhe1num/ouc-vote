package com.vote.server;

import com.vote.data.po.User;
import java.util.List;

public interface FollowServer {
    boolean toggle(Long userId, Long targetUserId);
    boolean isFollowing(Long userId, Long targetUserId);
    List<User> following(Long userId);
    List<User> followers(Long userId);
}
