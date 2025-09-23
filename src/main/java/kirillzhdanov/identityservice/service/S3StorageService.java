package kirillzhdanov.identityservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

@Service
public class S3StorageService {

    private final S3Client s3;
    private final S3Presigner presigner;

    @Value("${s3.bucket}")
    private String bucket;

    @Value("${s3.publicBaseUrl:}")
    private String publicBaseUrl; // e.g. https://storage.yandexcloud.net/{bucket}

    @Value("${s3.cache.maxAgeSeconds:2592000}") // 30 days
    private long cacheMaxAge;

    public S3StorageService(
            @Value("${s3.endpoint:https://storage.yandexcloud.net}") String endpoint,
            @Value("${s3.region:ru-central1}") String region,
            @Value("${s3.accessKey}") String accessKey,
            @Value("${s3.secretKey}") String secretKey
    ) {
        AwsBasicCredentials creds = AwsBasicCredentials.create(accessKey, secretKey);
        var credsProvider = StaticCredentialsProvider.create(creds);
        this.s3 = S3Client.builder()
                .credentialsProvider(credsProvider)
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .serviceConfiguration(S3Configuration.builder()
                        .checksumValidationEnabled(false)
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();

        this.presigner = S3Presigner.builder()
                .credentialsProvider(credsProvider)
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .build();
    }

    public void upload(String key, byte[] data, String contentType, boolean makePublic) {
        try {
            String cacheControl = "public, max-age=" + cacheMaxAge;
            PutObjectRequest.Builder b = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .cacheControl(cacheControl);
            if (makePublic) {
                b.acl(ObjectCannedACL.PUBLIC_READ);
            }
            PutObjectRequest req = b.build();
            s3.putObject(req, RequestBody.fromBytes(data));
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to upload to S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    public Optional<String> buildPublicUrl(String key) {
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) return Optional.empty();
        String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        return Optional.of(base + "/" + key);
    }

    public void delete(String key) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to delete from S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    public java.util.List<String> listKeysByPrefix(String prefix) {
        try {
            ListObjectsV2Request req = ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build();
            ListObjectsV2Response resp = s3.listObjectsV2(req);
            java.util.List<String> keys = new java.util.ArrayList<>();
            for (S3Object obj : resp.contents()) {
                keys.add(obj.key());
            }
            return keys;
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to list S3 objects: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    public void deleteByPrefix(String prefix) {
        java.util.List<String> keys = listKeysByPrefix(prefix);
        if (keys.isEmpty()) return;
        if (keys.size() == 1) {
            delete(keys.getFirst());
            return;
        }
        try {
            java.util.List<ObjectIdentifier> ids = new java.util.ArrayList<>();
            for (String k : keys) ids.add(ObjectIdentifier.builder().key(k).build());
            DeleteObjectsRequest delReq = DeleteObjectsRequest.builder()
                    .bucket(bucket)
                    .delete(Delete.builder().objects(ids).build())
                    .build();
            s3.deleteObjects(delReq);
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to bulk delete S3 objects: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    public byte[] getObjectBytes(String key) {
        try {
            GetObjectRequest req = GetObjectRequest.builder().bucket(bucket).key(key).build();
            return s3.getObjectAsBytes(req).asByteArray();
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to get S3 object: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    public Optional<String> buildPresignedGetUrl(String key, Duration ttl) {
        try {
            GetObjectRequest getReq = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                    .signatureDuration(ttl)
                    .getObjectRequest(getReq)
                    .build();
            PresignedGetObjectRequest presigned = presigner.presignGetObject(presignReq);
            return Optional.of(presigned.url().toString());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
