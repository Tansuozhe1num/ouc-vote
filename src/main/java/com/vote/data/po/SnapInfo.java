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

    private Long userId;

    private String snapImage;

    private Integer type;

    private Long ctime;

    private Integer status;

    private String desc;

    private String userName;

    private String userDescription;

    private String major;

    private Integer sex;
}
