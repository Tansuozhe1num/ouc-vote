package com.vote.data.enums;

public enum SnapInfoStatusEnum {

    Normal(0, "正常"),
    Deprecate(1, "删除"),
    Hidden(2, "仅自己可见");

    private Integer status;
    private String desc;

    SnapInfoStatusEnum(Integer status, String desc) {
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
