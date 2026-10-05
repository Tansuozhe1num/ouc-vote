package com.vote.server.impl;

import com.vote.data.enums.CommentStatusEnum;
import com.vote.data.po.ChildCommentInfo;
import com.vote.data.po.CommentInfo;
import com.vote.data.po.SnapInfo;
import com.vote.exception.BusinessException;
import com.vote.mappers.ChildCommentInfoMapper;
import com.vote.mappers.CommentInfoMapper;
import com.vote.mappers.SnapInfoMapper;
import com.vote.query.ChildCommentInfoQuery;
import com.vote.query.CommentInfoQuery;
import com.vote.query.SnapInfoQuery;
import com.vote.server.ChildCommentServer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
public class ChildCommentServerImpl implements ChildCommentServer {

    @Resource
    private ChildCommentInfoMapper<ChildCommentInfo, ChildCommentInfoQuery> childCommentInfoMapper;

    @Resource
    private CommentInfoMapper<CommentInfo, CommentInfoQuery> commentInfoMapper;

    @Resource
    private SnapInfoMapper<SnapInfo, SnapInfoQuery> snapInfoMapper;

    @Override
    public List<ChildCommentInfo> getChildCommentList(Integer parentId, Integer page, Integer size) {
        if (parentId == null || parentId <= 0) {
            throw new BusinessException("parentId不能为空");
        }
        int[] pageInfo = normalizePage(page, size);
        return this.childCommentInfoMapper.selectByParentId(parentId, pageInfo[1], pageInfo[0]);
    }

    @Override
    public List<ChildCommentInfo> getChildCommentListBySnapId(Integer snapId, Integer page, Integer size) {
        if (snapId == null || snapId <= 0) {
            throw new BusinessException("snapId不能为空");
        }
        int[] pageInfo = normalizePage(page, size);
        return this.childCommentInfoMapper.selectBySnapId(snapId, pageInfo[1], pageInfo[0]);
    }

    @Override
    public ChildCommentInfo getChildCommentById(Integer cId) {
        if (cId == null || cId <= 0) {
            throw new BusinessException("cId不能为空");
        }
        ChildCommentInfo child = this.childCommentInfoMapper.selectByCId(cId);
        if (child == null || CommentStatusEnum.Deleted.getStatus().equals(child.getStatus())) {
            throw new BusinessException("子评论不存在");
        }
        return child;
    }

    @Override
    public ChildCommentInfo saveChildComment(Integer snapId, Integer parentId, String userId, String text) {
        if (snapId == null || snapId <= 0) {
            throw new BusinessException("snapId不能为空");
        }
        if (parentId == null || parentId <= 0) {
            throw new BusinessException("parentId不能为空");
        }
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException("昵称不能为空");
        }
        if (!StringUtils.hasText(text)) {
            throw new BusinessException("评论内容不能为空");
        }

        String nick = userId.trim();
        String content = text.trim();
        if (nick.length() > 20) {
            throw new BusinessException("昵称过长");
        }
        if (content.length() > 200) {
            throw new BusinessException("评论内容过长");
        }

        SnapInfo snapInfo = this.snapInfoMapper.selectBySnapId(snapId);
        if (snapInfo == null) {
            throw new BusinessException("内容不存在");
        }

        CommentInfo parent = this.commentInfoMapper.selectByCommentId(parentId);
        if (parent == null || CommentStatusEnum.Deleted.getStatus().equals(parent.getStatus())) {
            throw new BusinessException("父评论不存在");
        }
        if (!snapId.equals(parent.getSnapId())) {
            throw new BusinessException("父评论与snap不匹配");
        }

        ChildCommentInfo child = ChildCommentInfo.builder()
                .snapId(snapId)
                .parentId(parentId)
                .userId(nick)
                .text(content)
                .ctime(new Date())
                .status(CommentStatusEnum.Normal.getStatus())
                .build();
        this.childCommentInfoMapper.insert(child);
        return child;
    }

    @Override
    public ChildCommentInfo updateChildComment(Integer cId, String text) {
        if (!StringUtils.hasText(text)) {
            throw new BusinessException("评论内容不能为空");
        }
        String content = text.trim();
        if (content.length() > 200) {
            throw new BusinessException("评论内容过长");
        }
        ChildCommentInfo exist = getChildCommentById(cId);
        Integer rows = this.childCommentInfoMapper.updateText(exist.getCId(), content);
        if (rows == null || rows == 0) {
            throw new BusinessException("更新失败");
        }
        exist.setText(content);
        return exist;
    }

    @Override
    public void deleteChildComment(Integer cId) {
        ChildCommentInfo exist = getChildCommentById(cId);
        Integer rows = this.childCommentInfoMapper.updateStatus(
                exist.getCId(),
                CommentStatusEnum.Deleted.getStatus()
        );
        if (rows == null || rows == 0) {
            throw new BusinessException("删除失败");
        }
    }

    /**
     * @return int[]{offset, size}
     */
    private int[] normalizePage(Integer page, Integer size) {
        if (page == null || page <= 0) {
            page = 1;
        }
        if (size == null || size <= 0 || size > 50) {
            size = 20;
        }
        return new int[]{(page - 1) * size, size};
    }
}
