package com.vote.query;

import java.util.List;

public class CommentInfoQuery extends BaseParam {

    private Integer commentId;

    private Integer snapId;

    private List<Integer> snapIdList;

    private String userId;

    private String userIdFuzzy;

    private Integer status;

    private String textFuzzy;

    public Integer getCommentId() {
        return commentId;
    }

    public void setCommentId(Integer commentId) {
        this.commentId = commentId;
    }

    public Integer getSnapId() {
        return snapId;
    }

    public void setSnapId(Integer snapId) {
        this.snapId = snapId;
    }

    public List<Integer> getSnapIdList() {
        return snapIdList;
    }

    public void setSnapIdList(List<Integer> snapIdList) {
        this.snapIdList = snapIdList;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserIdFuzzy() {
        return userIdFuzzy;
    }

    public void setUserIdFuzzy(String userIdFuzzy) {
        this.userIdFuzzy = userIdFuzzy;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getTextFuzzy() {
        return textFuzzy;
    }

    public void setTextFuzzy(String textFuzzy) {
        this.textFuzzy = textFuzzy;
    }
}
