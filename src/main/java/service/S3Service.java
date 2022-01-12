package service;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class S3Service {
    private static S3Client INSTANCE;

    public static S3Client getInstance() {
        if (INSTANCE == null)
            INSTANCE = S3Client.builder()
                    .region(Region.US_EAST_1)
                    .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                    .build();

        return INSTANCE;
    }
}
