package org.poolpool.mohaeng.storage.s3;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.poolpool.mohaeng.common.config.UploadProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class S3FileController {

    private final S3StorageService s3StorageService;
    private final UploadProperties uploadProperties;

    @GetMapping("/upload_files/event/**")
    public ResponseEntity<?> event(HttpServletRequest request) {
        return serve(request, "event", uploadProperties.boardDir());
    }

    @GetMapping("/upload_files/hbooth/**")
    public ResponseEntity<?> hbooth(HttpServletRequest request) {
        return serve(request, "host-booth", uploadProperties.hboothDir());
    }

    @GetMapping("/upload_files/pbooth/**")
    public ResponseEntity<?> pbooth(HttpServletRequest request) {
        return serve(request, "participant-booth", uploadProperties.pboothDir());
    }

    @GetMapping("/upload_files/photo/**")
    public ResponseEntity<?> photo(HttpServletRequest request) {
        return serve(request, "profile", uploadProperties.photoDir());
    }

    private ResponseEntity<?> serve(HttpServletRequest request, String s3Dir, Path localDir) {
        String storedValue = extractStoredValue(request);
        if (!StringUtils.hasText(storedValue)) {
            return ResponseEntity.notFound().build();
        }

        String key = s3StorageService.resolveKey(s3Dir, storedValue);
        if (StringUtils.hasText(key) && s3StorageService.exists(key)) {
            byte[] bytes = s3StorageService.getBytes(key);
            String contentType = s3StorageService.getContentType(key);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                    .contentType(parseMediaType(contentType))
                    .body(new ByteArrayResource(bytes));
        }

        Path localPath = resolveLocalPath(localDir, storedValue);
        if (localPath != null && Files.exists(localPath)) {
            try {
                String contentType = Files.probeContentType(localPath);
                byte[] bytes = Files.readAllBytes(localPath);
                return ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                        .contentType(parseMediaType(contentType))
                        .body(new ByteArrayResource(bytes));
            } catch (IOException e) {
                return ResponseEntity.internalServerError().body("파일 읽기 실패");
            }
        }

        return ResponseEntity.notFound().build();
    }

    private String extractStoredValue(HttpServletRequest request) {
        String uri = request.getRequestURI().replace('\\', '/');
        String marker = "/upload_files/";
        int markerIdx = uri.indexOf(marker);
        if (markerIdx < 0) {
            return null;
        }

        String after = uri.substring(markerIdx + marker.length());
        int firstSlash = after.indexOf('/');
        if (firstSlash < 0 || firstSlash == after.length() - 1) {
            return null;
        }

        return after.substring(firstSlash + 1);
    }

    private Path resolveLocalPath(Path baseDir, String storedValue) {
        if (!StringUtils.hasText(storedValue)) {
            return null;
        }

        String normalized = storedValue.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        int idx = normalized.lastIndexOf('/');
        String filenameOnly = idx >= 0 ? normalized.substring(idx + 1) : normalized;
        return baseDir.resolve(Paths.get(filenameOnly)).normalize();
    }

    private MediaType parseMediaType(String contentType) {
        try {
            return StringUtils.hasText(contentType) ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
