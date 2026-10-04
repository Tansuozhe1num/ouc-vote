package com.vote.server.impl;

import com.vote.data.enums.SnapInfoStatusEnum;
import com.vote.data.po.SnapInfo;
import com.vote.exception.BusinessException;
import com.vote.mappers.SnapInfoMapper;
import com.vote.oss.OssServer;
import com.vote.query.SnapInfoQuery;
import com.vote.server.SnapServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;
import java.util.UUID;
@Service
public class SnapServerImpl implements SnapServer {

    @Resource
    private SnapInfoMapper<SnapInfo, SnapInfoQuery> snapInfoMapper;

    @Resource
    private OssServer ossServer;

    private static final Logger logger = LoggerFactory.getLogger(SnapServerImpl.class);

    @Override
    public List<SnapInfo> getSnapInfoList(Integer page, Integer size) {
        if (page == null || page <= 0 || page > 10) {
            page = 1;
        }
        if (size == null || size <= 0 || size > 30) {
            size = 10;
        }

        Integer offset = (page - 1) * size;
        return this.snapInfoMapper.selectSnapInfoList(size, offset);
    }

    @Override
    public void saveSnapInfo(String name, MultipartFile avatar, Integer age, Integer sex,
                             String major, String qq, String wx, String desc) {

        SnapInfo exist = this.snapInfoMapper.selectByName(name);
        if (exist != null) {
            throw new BusinessException("已存在昵称");
        }

        String url = null;

        for (int i = 0; i < 3; i++) {
            try {
                url = ossServer.upload(avatar);
                break;
            } catch (Exception e) {
                logger.error("图片上传失败，第{}次重试", i + 1, e);
            }
        }

        if (url == null) {
            throw new BusinessException("图片上传失败");
        }

        long now = System.currentTimeMillis();

        SnapInfo snapInfo = SnapInfo.builder()
                .snapId(UUID.randomUUID().toString())
                .name(name)
                .snapImage(url)
                .age(age)
                .sex(sex)
                .major(major)
                .qq(qq)
                .wx(wx)
                .desc(desc)
                .ctime(now)
                .utime(now)
                .status(SnapInfoStatusEnum.Normal.getStatus())
                .build();

        this.snapInfoMapper.insert(snapInfo);
    }

}
