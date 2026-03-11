package org.poolpool.mohaeng.ai.client;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
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

    public <T> Mono<T> post(String uri, Object body, Class<T> responseType) {
        return post(uri, body, responseType, null);
    }

    public <T> Mono<T> post(String uri, Object body, Class<T> responseType, Map<String, String> headers) {
        return webClient.post()
                .uri(uri)
                .headers(httpHeaders -> applyHeaders(httpHeaders, headers))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(responseType);
    }

    public Mono<List<Long>> postLongList(String uri, Object body) {
        return webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Long>>() {});
    }

    public <T> Mono<T> get(String uri, Class<T> responseType) {
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(responseType);
    }

    private void applyHeaders(HttpHeaders httpHeaders, Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        headers.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                httpHeaders.set(key, value);
            }
        });
    }
}
