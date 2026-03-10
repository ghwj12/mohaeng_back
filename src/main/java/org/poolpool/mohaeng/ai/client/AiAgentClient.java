package org.poolpool.mohaeng.ai.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
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

    /**
     * FastAPI POST 호출 공통 메소드
     */
    public <T> Mono<T> post(String uri, Object body, Class<T> responseType) {

        return webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(responseType);
    }

    /**
     * FastAPI GET 호출 공통 메소드
     */
    public <T> Mono<T> get(String uri, Class<T> responseType) {

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(responseType);
    }
}