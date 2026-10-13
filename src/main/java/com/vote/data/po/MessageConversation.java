package com.vote.data.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One row in the inbox: the latest message exchanged with a peer. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageConversation {
    private Long peerId;
    private String peerName;
    private String peerMajor;
    private Long lastMessageId;
    private Long lastSenderId;
    private Integer lastSnapId;
    private String lastContent;
    private Integer lastCtime;
}
