package com.vote.server;

import com.vote.data.po.ChildCommentInfo;

import java.util.List;

public interface ChildCommentServer {

    List<ChildCommentInfo> getChildCommentList(Integer parentId, Integer page, Integer size);

    List<ChildCommentInfo> getChildCommentListBySnapId(Integer snapId, Integer page, Integer size);

    ChildCommentInfo getChildCommentById(Integer cId);

    ChildCommentInfo saveChildComment(Integer snapId, Integer parentId, String userId, String text);

    ChildCommentInfo updateChildComment(Integer cId, String text);

    void deleteChildComment(Integer cId);
}
