package com.vote.controller;

import com.vote.data.po.SnapInfo;
import com.vote.data.vo.ResponseVO;
import com.vote.server.SnapServer;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/snap")
public class SnapController extends ABaseController {

    @Resource
    private SnapServer snapServer;

    @RequestMapping("/list")
    public ResponseVO getSnapList(Integer page, Integer size) {
        List<SnapInfo> snapInfoList = this.snapServer.getSnapInfoList(page, size);
        return getSuccessResponseVO(snapInfoList);
    }

    @RequestMapping("/save")
    public ResponseVO saveSnapList(
            @NotNull String name, @NotNull MultipartFile avator,
            @NotNull Integer age, @NotNull Integer sex,
            @NotNull String major, String qq, String wx, String desc) {
        this.snapServer.saveSnapInfo(name, avator, age, sex, major, qq, wx, desc);
        return getSuccessResponseVO(null);
    }

    public ResponseVO voteQQorWx(String qq, String wx) {

        return getSuccessResponseVO(null);
    }
}
