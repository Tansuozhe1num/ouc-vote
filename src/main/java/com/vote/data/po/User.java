package com.vote.data.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private Long userId;
    private String name;
    private Integer status;
    private String description;
    private Integer sex;
    private Integer ctime;
    private String major;
    private String token;
    private String refreshtoken;
    private String loginName;
    private String passwordHash;
    private String wechatOpenid;
}
