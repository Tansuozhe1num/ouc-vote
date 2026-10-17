package com.vote.server;

import com.vote.data.po.SnapInfo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SnapServer {
    List<SnapInfo> getSnapInfoList(Integer page, Integer size, Integer type);

    SnapInfo getSnapInfoById(Integer snapId);

    SnapInfo saveSnapInfo(Long userId, MultipartFile avatar, String desc, Integer type, String idempotencyKey);

    List<SnapInfo> getMySnapInfoList(Long userId, Integer page, Integer size);

    boolean updateStatus(Long userId, Integer snapId, Integer status);

    List<SnapInfo> getHotRank();
}
