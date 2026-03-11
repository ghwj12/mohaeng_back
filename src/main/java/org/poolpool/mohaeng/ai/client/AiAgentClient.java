package org.poolpool.mohaeng.ai.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Service
public class AiAgentClient {

    private final WebClient webClient;

    public AiAgentClient(
            @Value("${ai.base-url}") String baseUrl,
            @Value("${ai.api-key}") String apiKey
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-API-KEY", apiKey)
                .build();
    }

    /** FastAPI POST 호출 - 단일 객체 반환 */
    public <T> Mono<T> post(String uri, Object body, Class<T> responseType) {
        return webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(responseType);
    }

    /** FastAPI POST 호출 - Long 리스트 반환 (빈 배열 [] 도 정상 처리) */
    public Mono<List<Long>> postLongList(String uri, Object body) {
        return webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Long>>() {});
    }

    /** FastAPI POST 호출 - multipart/form-data (AI 태그 추천용) */
    public <T> Mono<T> postMultipart(String uri, String title, String description,
                                      MultipartFile thumbnail, Class<T> responseType) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("title", title);
        builder.part("simple_explain", description);

        if (thumbnail != null && !thumbnail.isEmpty()) {
            try {
                byte[] bytes = thumbnail.getBytes();
                String filename = thumbnail.getOriginalFilename() != null
                        ? thumbnail.getOriginalFilename() : "thumbnail.jpg";
                builder.part("thumbnail", new ByteArrayResource(bytes) {
                    @Override
                    public String getFilename() { return filename; }
                }).contentType(MediaType.IMAGE_JPEG);
            } catch (Exception e) {
                System.out.println("썸네일 변환 실패: " + e.getMessage());
            }
        }

        return webClient.post()
                .uri(uri)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(responseType);
    }

    /** FastAPI GET 호출 */
    public <T> Mono<T> get(String uri, Class<T> responseType) {
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(responseType);
    }
}
