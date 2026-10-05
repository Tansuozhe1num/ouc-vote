package com.vote.data.enums;

public enum CommentStatusEnum {

    Normal(0, "正常"),
    Deleted(1, "删除");

    private final Integer status;
    private final String desc;

    CommentStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public Integer getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }
}
