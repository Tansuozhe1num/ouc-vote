package com.vote.mappers;

import com.vote.data.po.UserFollow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserFollowMapper {
    UserFollow select(@Param("userId") Long userId, @Param("followerId") Long followerId);
    Integer selectStatus(@Param("userId") Long userId, @Param("followerId") Long followerId);
    int insert(UserFollow follow);
    int updateStatus(@Param("userId") Long userId, @Param("followerId") Long followerId, @Param("status") Integer status);
    List<Long> selectFollowingIds(@Param("userId") Long userId);
    List<Long> selectFollowerIds(@Param("userId") Long userId);
}
