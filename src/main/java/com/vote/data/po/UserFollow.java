package com.vote.data.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFollow {
    private Long id;
    private Long userId;
    private Long followerId;
    private Integer status;
    private Integer ctime;
}
