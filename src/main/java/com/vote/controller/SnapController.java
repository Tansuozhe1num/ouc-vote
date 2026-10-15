package com.vote.controller;

import com.vote.auth.AuthContext;
import com.vote.data.po.SnapInfo;
import com.vote.data.vo.ResponseVO;
import com.vote.server.MatchServer;
import com.vote.server.SnapServer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/snap")
public class SnapController extends ABaseController {
    @Resource private SnapServer snapServer;
    @Resource private MatchServer matchServer;

    @GetMapping("/list")
    public ResponseVO list(Integer page, Integer size, Integer type) {
        List<SnapInfo> snaps = snapServer.getSnapInfoList(page, size, type);
        return getSuccessResponseVO(snaps);
    }

    @GetMapping("/detail")
    public ResponseVO detail(@RequestParam Integer snapId) { return getSuccessResponseVO(snapServer.getSnapInfoById(snapId)); }

    @GetMapping("/mine")
    public ResponseVO mine(Integer page, Integer size, HttpServletRequest request) {
        return getSuccessResponseVO(snapServer.getMySnapInfoList(AuthContext.requireUserId(request), page, size));
    }

    @PostMapping("/save")
    public ResponseVO save(@RequestParam(value = "avatar", required = false) MultipartFile avatar,
                           @RequestParam String desc,
                           @RequestParam Integer type,
                           @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                           HttpServletRequest request) {
        String requestKey = StringUtils.hasText(idempotencyKey)
                ? idempotencyKey.trim()
                : UUID.randomUUID().toString();
        return getSuccessResponseVO(snapServer.saveSnapInfo(AuthContext.requireUserId(request), avatar, desc, type, requestKey));
    }

    @PostMapping("/status")
    public ResponseVO status(@RequestParam Integer snapId, @RequestParam Integer status, HttpServletRequest request) {
        return getSuccessResponseVO(snapServer.updateStatus(AuthContext.requireUserId(request), snapId, status));
    }

    @GetMapping("/draw")
    public ResponseVO draw(HttpServletRequest request) { return getSuccessResponseVO(matchServer.draw(AuthContext.requireUserId(request))); }
}
