package com.vote.server.impl;

import com.vote.data.constants.Constants;
import com.vote.data.enums.CommentStatusEnum;
import com.vote.data.po.CommentInfo;
import com.vote.data.po.SnapInfo;
import com.vote.exception.BusinessException;
import com.vote.mappers.CommentInfoMapper;
import com.vote.mappers.SnapInfoMapper;
import com.vote.query.CommentInfoQuery;
import com.vote.query.SnapInfoQuery;
import com.vote.server.CommentServer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
public class CommentServerImpl implements CommentServer {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_PAGE_NUMBER = 100000;

    @Resource
    private CommentInfoMapper<CommentInfo, CommentInfoQuery> commentInfoMapper;

    @Resource
    private SnapInfoMapper<SnapInfo, SnapInfoQuery> snapInfoMapper;

    @Resource
    private SnapScoreService snapScoreService;

    @Override
    public List<CommentInfo> getCommentList(Integer snapId, Integer page, Integer size) {
        if (snapId == null || snapId <= 0) {
            throw new BusinessException("snapId不能为空");
        }
        page = page == null || page < 1 ? 1 : Math.min(page, MAX_PAGE_NUMBER);
        size = size == null || size < 1 || size > MAX_PAGE_SIZE ? DEFAULT_PAGE_SIZE : size;
        int offset = (page - 1) * size;
        return this.commentInfoMapper.selectBySnapId(snapId, size, offset);
    }

    @Override
    public CommentInfo getCommentById(Integer commentId) {
        if (commentId == null) {
            throw new BusinessException("commentId不能为空");
        }
        CommentInfo commentInfo = this.commentInfoMapper.selectByCommentId(commentId);
        if (commentInfo == null || CommentStatusEnum.Deleted.getStatus().equals(commentInfo.getStatus())) {
            throw new BusinessException("评论不存在");
        }
        return commentInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentInfo saveComment(Integer snapId, Long userId, String text) {
        if (snapId == null || snapId <= 0) {
            throw new BusinessException("snapId不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException("评论用户不能为空");
        }
        if (!StringUtils.hasText(text)) {
            throw new BusinessException("评论内容不能为空");
        }
        String content = text.trim();
        if (content.length() > 200) {
            throw new BusinessException("评论内容过长");
        }

        SnapInfo snapInfo = this.snapInfoMapper.selectBySnapId(snapId);
        if (snapInfo == null) {
            throw new BusinessException("内容不存在");
        }

        CommentInfo commentInfo = CommentInfo.builder()
                .snapId(snapId)
                .userId(String.valueOf(userId))
                .text(content)
                .ctime(new Date())
                .status(CommentStatusEnum.Normal.getStatus())
                .build();
        this.commentInfoMapper.insert(commentInfo);

        snapScoreService.adjustScore(snapId, Constants.CommentRankScore);
        return commentInfo;
    }

    @Override
    public CommentInfo updateComment(Integer commentId, String text) {
        if (!StringUtils.hasText(text)) {
            throw new BusinessException("评论内容不能为空");
        }
        String content = text.trim();
        if (content.length() > 200) {
            throw new BusinessException("评论内容过长");
        }
        CommentInfo exist = getCommentById(commentId);
        Integer rows = this.commentInfoMapper.updateText(exist.getCommentId(), content);
        if (rows == null || rows == 0) {
            throw new BusinessException("更新失败");
        }
        exist.setText(content);
        return exist;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Integer commentId) {
        CommentInfo exist = getCommentById(commentId);
        Integer rows = this.commentInfoMapper.updateStatus(
                exist.getCommentId(),
                CommentStatusEnum.Deleted.getStatus()
        );
        if (rows == null || rows == 0) {
            throw new BusinessException("删除失败");
        }
        snapScoreService.adjustScore(exist.getSnapId(), -Constants.CommentRankScore);
    }
}
