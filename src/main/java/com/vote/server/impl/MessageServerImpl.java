package com.vote.server.impl;

import com.vote.data.po.MessageInfo;
import com.vote.data.po.MessageConversation;
import com.vote.exception.BusinessException;
import com.vote.mappers.MessageInfoMapper;
import com.vote.mappers.UserMapper;
import com.vote.server.MessageServer;
import com.vote.mappers.SnapInfoMapper;
import com.vote.data.po.SnapInfo;
import com.vote.query.SnapInfoQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;

@Service
public class MessageServerImpl implements MessageServer {
    @Resource private MessageInfoMapper messageMapper;
    @Resource private UserMapper userMapper;
    @Resource private SnapInfoMapper<SnapInfo, SnapInfoQuery> snapMapper;

    @Override
    public MessageInfo send(Long senderId, Long receiverId, Integer snapId, String content) {
        if (receiverId == null || senderId.equals(receiverId)) {
            throw new BusinessException("聊天对象无效");
        }
        com.vote.data.po.User receiver = userMapper.selectByUserId(receiverId);
        if (receiver == null || receiver.getStatus() == null || receiver.getStatus() != 0)  {
            throw new BusinessException("用户不存在");
        }
        if (snapId != null) {
            SnapInfo snap = snapMapper.selectBySnapId(snapId);
            if (snap == null || !receiverId.equals(snap.getUserId())) throw new BusinessException("搭子动态不存在");
        }
        if (!StringUtils.hasText(content)) throw new BusinessException("消息不能为空");
        if (content.length() > 1000) content = content.substring(0, 1000);
        MessageInfo message = MessageInfo.builder().senderId(senderId).receiverId(receiverId).snapId(snapId)
                .content(content.trim()).status(0).ctime((int) (System.currentTimeMillis() / 1000)).build();
        messageMapper.insert(message);
        return message;
    }

    @Override public List<MessageInfo> conversation(Long userId, Long peerId) {
        if (peerId == null) throw new BusinessException("聊天对象无效");
        messageMapper.markRead(userId, peerId);
        return messageMapper.selectConversation(userId, peerId);
    }
    @Override public List<MessageConversation> inbox(Long userId) { return messageMapper.selectInboxConversations(userId); }
}
