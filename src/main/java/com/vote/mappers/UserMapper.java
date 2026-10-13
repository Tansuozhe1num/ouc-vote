package com.vote.mappers;

import com.vote.data.po.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserMapper {
    User selectByUserId(@Param("userId") Long userId);
    User selectByLoginName(@Param("loginName") String loginName);
    User selectByWechatOpenid(@Param("openid") String openid);
    List<User> selectByIds(@Param("ids") List<Long> ids);
    int insert(User user);
    int updateToken(@Param("userId") Long userId, @Param("token") String token, @Param("refreshtoken") String refreshtoken);
    int updateProfile(User user);
    int bindWechat(@Param("userId") Long userId, @Param("openid") String openid);
}
