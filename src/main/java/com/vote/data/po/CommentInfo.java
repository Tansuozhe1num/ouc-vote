package com.vote.data.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentInfo {

    private Integer commentId;

    /**
     * 关联的 snapId
     */
    private Integer snapId;

    private String text;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date ctime;

    /**
     * 1删除 0正常
     */
    private Integer status;

    /** 评论作者的用户 ID；历史评论可能保存的是昵称。 */
    private String userId;

    private String userName;
}
