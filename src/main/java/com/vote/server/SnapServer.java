package com.vote.server;

import com.vote.data.dto.SnapInfoDTO;
import com.vote.data.po.SnapInfo;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.util.List;

public interface SnapServer {
     List<SnapInfo> getSnapInfoList(Integer page, Integer size);

     void saveSnapInfo(String name, MultipartFile avator, Integer age, Integer sex, String major, String qq, String wx, String desc);
}
