package com.vote.controller;

import com.vote.data.po.ChildCommentInfo;
import com.vote.data.vo.ResponseVO;
import com.vote.server.ChildCommentServer;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/childComment")
public class ChildCommentController extends ABaseController {

    @Resource
    private ChildCommentServer childCommentServer;

    /**
     * 按父评论分页查询子评论
     */
    @RequestMapping("/list")
    public ResponseVO getChildCommentList(@NotNull Integer parentId, Integer page, Integer size) {
        List<ChildCommentInfo> list = this.childCommentServer.getChildCommentList(parentId, page, size);
        return getSuccessResponseVO(list);
    }

    /**
     * 按 snap 分页查询子评论
     */
    @RequestMapping("/listBySnap")
    public ResponseVO getChildCommentListBySnap(@NotNull Integer snapId, Integer page, Integer size) {
        List<ChildCommentInfo> list = this.childCommentServer.getChildCommentListBySnapId(snapId, page, size);
        return getSuccessResponseVO(list);
    }

    @RequestMapping("/detail")
    public ResponseVO getChildCommentDetail(@NotNull Integer cId) {
        return getSuccessResponseVO(this.childCommentServer.getChildCommentById(cId));
    }

    /**
     * 发表子评论。兼容 nickname/content 与 userId/text。
     */
    @RequestMapping("/save")
    public ResponseVO saveChildComment(@NotNull Integer snapId,
                                       @NotNull Integer parentId,
                                       String nickname,
                                       String userId,
                                       String content,
                                       String text) {
        String nick = StringUtils.hasText(nickname) ? nickname : userId;
        String body = StringUtils.hasText(content) ? content : text;
        ChildCommentInfo saved = this.childCommentServer.saveChildComment(snapId, parentId, nick, body);
        return getSuccessResponseVO(saved);
    }

    @RequestMapping("/update")
    public ResponseVO updateChildComment(@NotNull Integer cId, String content, String text) {
        String body = StringUtils.hasText(content) ? content : text;
        ChildCommentInfo updated = this.childCommentServer.updateChildComment(cId, body);
        return getSuccessResponseVO(updated);
    }

    /**
     * 软删除子评论
     */
    @RequestMapping("/delete")
    public ResponseVO deleteChildComment(@NotNull Integer cId) {
        this.childCommentServer.deleteChildComment(cId);
        return getSuccessResponseVO(null);
    }
}
