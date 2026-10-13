package com.vote.server.impl;

import com.vote.data.po.User;
import com.vote.data.po.UserFollow;
import com.vote.exception.BusinessException;
import com.vote.mappers.UserFollowMapper;
import com.vote.mappers.UserMapper;
import com.vote.server.FollowServer;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

@Service
public class FollowServerImpl implements FollowServer {
    @Resource private UserFollowMapper followMapper;
    @Resource private UserMapper userMapper;

    @Override
    public boolean toggle(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) throw new BusinessException("不能关注自己");
        User target = userMapper.selectByUserId(targetUserId);
        if (target == null || target.getStatus() == null || target.getStatus() != 0) throw new BusinessException("用户不存在");
        UserFollow relation = followMapper.select(targetUserId, userId);
        if (relation == null) {
            followMapper.insert(UserFollow.builder().userId(targetUserId).followerId(userId).status(0)
                    .ctime((int) (System.currentTimeMillis() / 1000)).build());
            return true;
        }
        int next = relation.getStatus() != null && relation.getStatus() == 0 ? 1 : 0;
        followMapper.updateStatus(targetUserId, userId, next);
        return next == 0;
    }

    @Override
    public boolean isFollowing(Long userId, Long targetUserId) {
        Integer status = followMapper.selectStatus(targetUserId, userId);
        return status != null && status == 0;
    }

    @Override public List<User> following(Long userId) { return users(followMapper.selectFollowingIds(userId)); }
    @Override public List<User> followers(Long userId) { return users(followMapper.selectFollowerIds(userId)); }

    private List<User> users(List<Long> ids) {
        return ids == null || ids.isEmpty() ? Collections.emptyList() : userMapper.selectByIds(ids);
    }
}
