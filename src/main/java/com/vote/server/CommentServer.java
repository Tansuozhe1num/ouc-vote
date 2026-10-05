package com.vote.server;

import com.vote.data.po.CommentInfo;

import java.util.List;

public interface CommentServer {

    List<CommentInfo> getCommentList(Integer snapId, Integer page, Integer size);

    CommentInfo getCommentById(Integer commentId);

    CommentInfo saveComment(Integer snapId, String userId, String text);

    CommentInfo updateComment(Integer commentId, String text);

    void deleteComment(Integer commentId);
}
