package com.vote.data.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SnapInfo {

    private Integer snapId;

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
