package oss;

import com.aliyun.sdk.service.oss2.credentials.CredentialsProvider;
import com.aliyun.sdk.service.oss2.credentials.EnvironmentVariableCredentialsProvider;
import com.aliyun.sdk.service.oss2.vectors.OSSVectorsClient;
import com.aliyun.sdk.service.oss2.vectors.models.ListVectorBucketsRequest;
import com.aliyun.sdk.service.oss2.vectors.models.ListVectorBucketsResult;

public class ListVectorBucketsSample {
    public static void main(String[] args) throws Exception {
        CredentialsProvider provider = new EnvironmentVariableCredentialsProvider();
        try (OSSVectorsClient client = OSSVectorsClient.newBuilder()
                .region("cn-hangzhou")
                .accountId("1609438938050927")
                .credentialsProvider(provider)
                .build()) {
            ListVectorBucketsRequest request = ListVectorBucketsRequest.newBuilder()
                    .build();
            ListVectorBucketsResult result = client.listVectorBuckets(request);
            System.out.printf("status code: %d, request id: %s%n", result.statusCode(), result.requestId());
            System.out.println(result);
        }
    }
}