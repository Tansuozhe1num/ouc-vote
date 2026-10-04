package com.vote.config;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.credentials.CredentialsProvider;
import com.aliyun.sdk.service.oss2.credentials.EnvironmentVariableCredentialsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OssConfig {

    @Bean(destroyMethod = "close")
    public OSSClient ossClient() {

        CredentialsProvider provider =
                new EnvironmentVariableCredentialsProvider();

        return OSSClient.newBuilder()
                .region("cn-beijing")
                .credentialsProvider(provider)
                .build();
    }
}