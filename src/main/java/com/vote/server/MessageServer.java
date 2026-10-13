package com.vote.server;

import com.vote.data.po.MessageInfo;
import com.vote.data.po.MessageConversation;
import java.util.List;

public interface MessageServer {
    MessageInfo send(Long senderId, Long receiverId, Integer snapId, String content);
    List<MessageInfo> conversation(Long userId, Long peerId);
    List<MessageConversation> inbox(Long userId);
}
