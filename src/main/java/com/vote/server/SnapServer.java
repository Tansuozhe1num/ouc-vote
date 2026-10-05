package com.vote.server;

import com.vote.data.po.SnapInfo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SnapServer {
    List<SnapInfo> getSnapInfoList(Integer page, Integer size);

    SnapInfo getSnapInfoById(Integer snapId);

    void saveSnapInfo(String name, MultipartFile avator, Integer age, Integer sex, String major, String qq, String wx, String desc);
}
