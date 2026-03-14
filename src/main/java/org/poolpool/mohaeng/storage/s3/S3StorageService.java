package org.poolpool.mohaeng.storage.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.region}")
    private String region;

    public String upload(MultipartFile file, String dir) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalName = file.getOriginalFilename();
        String ext = "";

        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }

        String savedName = UUID.randomUUID() + ext;
        String key = dir + "/" + savedName;

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, file.getSize()));
            return key;
        } catch (Exception e) {
            throw new RuntimeException("S3 업로드 실패", e);
        }
    }

    public String uploadFromUrl(String fileUrl, String dir) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return null;
        }

        try {
            URL url = new URL(fileUrl);
            URLConnection connection = url.openConnection();
            String contentType = connection.getContentType();

            String ext = "";
            String cleanUrl = fileUrl;
            int queryIdx = cleanUrl.indexOf('?');
            if (queryIdx >= 0) {
                cleanUrl = cleanUrl.substring(0, queryIdx);
            }
            int dotIdx = cleanUrl.lastIndexOf('.');
            if (dotIdx >= 0 && dotIdx > cleanUrl.lastIndexOf('/')) {
                ext = cleanUrl.substring(dotIdx);
            } else if (contentType != null) {
                if (contentType.contains("jpeg") || contentType.contains("jpg")) ext = ".jpg";
                else if (contentType.contains("png")) ext = ".png";
                else if (contentType.contains("gif")) ext = ".gif";
                else if (contentType.contains("webp")) ext = ".webp";
            }

            String savedName = UUID.randomUUID() + ext;
            String key = dir + "/" + savedName;

            try (InputStream inputStream = connection.getInputStream()) {
                byte[] bytes = inputStream.readAllBytes();

                PutObjectRequest request = PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build();

                s3Client.putObject(request, RequestBody.fromBytes(bytes));
                return key;
            }
        } catch (Exception e) {
            throw new RuntimeException("외부 URL S3 업로드 실패: " + fileUrl, e);
        }
    }

    public String getFileUrl(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    public void delete(String key) {
        if (key == null || key.isBlank()) {
            return;
        }

        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }

    public void delete(String dir, String fileName) {
        String key = resolveKey(dir, fileName);
        if (key == null || key.isBlank()) {
            return;
        }

        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }

    public byte[] getBytes(String key) {
        ResponseBytes<?> bytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
        return bytes.asByteArray();
    }

    public String getContentType(String key) {
        return s3Client.headObject(HeadObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()).contentType();
    }

    public boolean exists(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    public String resolveKey(String dir, String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            return null;
        }

        String value = storedValue.trim();

        if (value.startsWith("http://") || value.startsWith("https://")) {
            try {
                URI uri = URI.create(value);
                value = uri.getPath();
            } catch (Exception ignored) {
            }
        }

        value = value.replace('\\', '/');
        while (value.startsWith("/")) {
            value = value.substring(1);
        }

        if (value.startsWith("upload_files/")) {
            value = value.substring("upload_files/".length());
        }

        if (value.startsWith(dir + "/")) {
            return value;
        }

        int idx = value.lastIndexOf('/');
        String filenameOnly = idx >= 0 ? value.substring(idx + 1) : value;
        return dir + "/" + filenameOnly;
    }
}
