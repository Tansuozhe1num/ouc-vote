package com.vote.data.po;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SnapInfo {

    private String snapId;

    private String snapImage;

    private Integer sex;

    private String name;

    private String qq;

    private String wx;

    private Integer age;

    private String major;

    private Long ctime;

    private Long utime;

    private Integer status;

    private String desc;
}