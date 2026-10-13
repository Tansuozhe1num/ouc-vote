package com.vote.controller;

import com.vote.auth.AuthContext;
import com.vote.data.vo.ResponseVO;
import com.vote.server.MessageServer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/message")
public class MessageController extends ABaseController {
    @Resource private MessageServer messageServer;

    @PostMapping("/send")
    public ResponseVO send(@RequestParam Long receiverId, Integer snapId, @RequestParam String content, HttpServletRequest request) {
        return getSuccessResponseVO(messageServer.send(AuthContext.requireUserId(request), receiverId, snapId, content));
    }

    @GetMapping("/conversation")
    public ResponseVO conversation(@RequestParam Long peerId, HttpServletRequest request) {
        return getSuccessResponseVO(messageServer.conversation(AuthContext.requireUserId(request), peerId));
    }

    @GetMapping("/inbox")
    public ResponseVO inbox(HttpServletRequest request) {
        return getSuccessResponseVO(messageServer.inbox(AuthContext.requireUserId(request)));
    }
}
