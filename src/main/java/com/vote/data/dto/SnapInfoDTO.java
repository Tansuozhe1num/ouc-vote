package com.vote.data.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SnapInfoDTO {

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
}
