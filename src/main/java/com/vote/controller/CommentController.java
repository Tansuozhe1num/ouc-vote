package com.vote.controller;

import com.vote.auth.AuthContext;
import com.vote.data.po.CommentInfo;
import com.vote.data.po.User;
import com.vote.data.vo.ResponseVO;
import com.vote.server.CommentServer;
import com.vote.server.UserServer;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/comment")
public class CommentController extends ABaseController {

    @Resource
    private CommentServer commentServer;

    @Resource
    private UserServer userServer;

    /**
     * 按 snap 分页查询评论
     */
    @RequestMapping("/list")
    public ResponseVO getCommentList(@NotNull Integer snapId, Integer page, Integer size) {
        List<CommentInfo> list = this.commentServer.getCommentList(snapId, page, size);
        return getSuccessResponseVO(list);
    }

    /**
     * 兼容旧路径
     */
    @RequestMapping("/one_comment")
    public ResponseVO getCommentBySnapId(@NotNull Integer snapId, Integer page, Integer size) {
        return getCommentList(snapId, page, size);
    }

    @RequestMapping("/detail")
    public ResponseVO getCommentDetail(@NotNull Integer commentId) {
        return getSuccessResponseVO(this.commentServer.getCommentById(commentId));
    }

    /**
     * 发表评论。评论身份从当前登录用户读取，忽略客户端传入的昵称。
     */
    @RequestMapping("/save")
    public ResponseVO saveComment(@NotNull Integer snapId, String content, String text,
                                  HttpServletRequest request) {
        User user = userServer.getUser(AuthContext.requireUserId(request));
        String body = StringUtils.hasText(content) ? content : text;
        CommentInfo saved = this.commentServer.saveComment(snapId, user.getName(), body);
        return getSuccessResponseVO(saved);
    }

    @RequestMapping("/update")
    public ResponseVO updateComment(@NotNull Integer commentId, String content, String text) {
        String body = StringUtils.hasText(content) ? content : text;
        CommentInfo updated = this.commentServer.updateComment(commentId, body);
        return getSuccessResponseVO(updated);
    }

    /**
     * 软删除评论
     */
    @RequestMapping("/delete")
    public ResponseVO deleteComment(@NotNull Integer commentId) {
        this.commentServer.deleteComment(commentId);
        return getSuccessResponseVO(null);
    }
}
