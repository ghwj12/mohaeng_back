package org.poolpool.mohaeng.ai.client;

import org.poolpool.mohaeng.ai.dto.AiModerationRequestDto;
import org.poolpool.mohaeng.ai.dto.AiModerationResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AiModerationClient {

    @Value("${ai.base-url}")
    private String baseUrl;

    @Value("${ai.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public AiModerationResponseDto moderateEvent(AiModerationRequestDto requestDto) {
        String url = baseUrl + "/ai/moderation/event";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", apiKey);

        HttpEntity<AiModerationRequestDto> requestEntity = new HttpEntity<>(requestDto, headers);

        ResponseEntity<AiModerationResponseDto> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                AiModerationResponseDto.class
        );

        return response.getBody();
    }
}