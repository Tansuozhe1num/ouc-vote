package com.vote.oss;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.models.PutObjectRequest;
import com.aliyun.sdk.service.oss2.models.PutObjectResult;
import com.aliyun.sdk.service.oss2.transport.BinaryData;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class OssServer {

    private final OSSClient ossClient;

    private static final String BUCKET_NAME = "ouc-vote";
    private static final String DIR = "vote/";

    public OssServer(OSSClient ossClient) {
        this.ossClient = ossClient;
    }

    public String upload(MultipartFile picture) throws IOException {
        if (picture == null || picture.isEmpty()) {
            throw new RuntimeException("图片不能为空");
        }
        if (picture.getSize() > 4 * 1024 * 1024) {
            throw new RuntimeException("图片不能超过4MB");
        }
        String originalFilename = picture.getOriginalFilename();

        String objectKey = DIR + UUID.randomUUID();

        PutObjectRequest request = PutObjectRequest.newBuilder()
                .bucket(BUCKET_NAME)
                .key(objectKey)
                .body(BinaryData.fromBytes(picture.getBytes()))
                .build();

        PutObjectResult result = ossClient.putObject(request);

        if (result.statusCode() != 200) {
            throw new RuntimeException("OSS上传失败");
        }

        return "https://" + BUCKET_NAME
                + ".oss-cn-beijing.aliyuncs.com/"
                + objectKey;
    }
}
