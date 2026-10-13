package com.vote.data.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageInfo {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private Integer snapId;
    private String content;
    private Integer status;
    private Integer ctime;
    private String senderName;
    private String receiverName;
}
