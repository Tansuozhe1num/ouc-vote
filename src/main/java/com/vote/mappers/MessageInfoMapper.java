package com.vote.mappers;

import com.vote.data.po.MessageInfo;
import com.vote.data.po.MessageConversation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MessageInfoMapper {
    int insert(MessageInfo message);
    List<MessageInfo> selectConversation(@Param("userId") Long userId, @Param("peerId") Long peerId);
    List<MessageInfo> selectInbox(@Param("userId") Long userId);
    List<MessageConversation> selectInboxConversations(@Param("userId") Long userId);
    int markRead(@Param("userId") Long userId, @Param("peerId") Long peerId);
}
