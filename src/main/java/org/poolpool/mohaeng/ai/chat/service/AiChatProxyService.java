package org.poolpool.mohaeng.ai.chat.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.poolpool.mohaeng.ai.chat.dto.AiChatRequest;
import org.poolpool.mohaeng.ai.chat.dto.AiChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Service
public class AiChatProxyService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.base-url}")
    private String aiBaseUrl;

    @Value("${ai.api-key}")
    private String aiApiKey;

    public AiChatResponse chat(AiChatRequest request, String authorizationHeader) {
        String url = aiBaseUrl + "/ai/chat";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", aiApiKey);

        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", request.getMessage());
        body.put("authorization", authorizationHeader);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<AiChatResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    AiChatResponse.class
            );

            if (response.getBody() != null) {
                return response.getBody();
            }

        } catch (HttpStatusCodeException e) {
            System.out.println("[AI CHAT ERROR] status=" + e.getStatusCode());
            System.out.println("[AI CHAT ERROR] body=" + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.out.println("[AI CHAT ERROR] " + e.getMessage());
        }

        AiChatResponse fallback = new AiChatResponse();
        fallback.setIntent("fallback");
        fallback.setAnswer("지금은 AI 서버와 연결하는 중 문제가 생겼어요. 잠시 후 다시 시도해 주세요.");
        return fallback;
    }
}