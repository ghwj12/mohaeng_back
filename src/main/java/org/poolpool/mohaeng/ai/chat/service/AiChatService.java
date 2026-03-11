package org.poolpool.mohaeng.ai.chat.service;

import lombok.RequiredArgsConstructor;
import org.poolpool.mohaeng.ai.chat.dto.AiChatRequest;
import org.poolpool.mohaeng.ai.chat.dto.AiChatResponse;
import org.poolpool.mohaeng.ai.client.AiAgentClient;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AiChatService {

    private static final Pattern GREETING_PATTERN = Pattern.compile(
            "^(안녕+|하이+|ㅎㅇ+|hello+|hi+|반가워+|안녕하세요+)[!.?\\s]*$",
            Pattern.CASE_INSENSITIVE
    );

    private final AiAgentClient aiAgentClient;

    public AiChatResponse chat(AiChatRequest request, String authorizationHeader) {
        String message = request != null && request.getMessage() != null
                ? request.getMessage().trim()
                : "";

        AiChatResponse localResponse = createLocalConversationalResponse(message);
        if (localResponse != null) {
            return localResponse;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", message);
        payload.put("authorization", authorizationHeader);

        Map<String, String> forwardedHeaders = new LinkedHashMap<>();
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            forwardedHeaders.put(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }

        AiChatResponse response = aiAgentClient
                .post("/ai/chat", payload, AiChatResponse.class, forwardedHeaders)
                .block();

        if (response == null) {
            return createFallbackResponse();
        }

        boolean noAnswer = response.getAnswer() == null || response.getAnswer().isBlank();
        boolean noCards = response.getCards() == null || response.getCards().isEmpty();

        if (noAnswer && noCards) {
            return createFallbackResponse();
        }

        return response;
    }

    private AiChatResponse createLocalConversationalResponse(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        String normalized = message.replaceAll("\\s+", "").toLowerCase();

        if (GREETING_PATTERN.matcher(message).matches()) {
            return buildSimpleResponse(
                    "greeting",
                    "안녕하세요. 모행 AI 챗봇이에요. 행사 추천, 환불 규정, 문의 내역, 일정, 지역별 행사 검색까지 도와드릴게요."
            );
        }

        if (normalized.equals("고마워")
                || normalized.equals("감사")
                || normalized.equals("감사해")
                || normalized.equals("thanks")) {
            return buildSimpleResponse(
                    "smalltalk",
                    "천만에요. 필요한 내용을 이어서 물어보시면 바로 도와드릴게요."
            );
        }

        if (normalized.contains("뭐할수있")
                || normalized.contains("무엇을할수있")
                || normalized.contains("도움")
                || normalized.contains("기능")) {
            return buildSimpleResponse(
                    "help",
                    "행사 추천, 지역별 행사 검색, 신청 가능 행사 안내, 환불 규정 안내, 내 문의 내역 확인 같은 요청을 처리할 수 있어요."
            );
        }

        return null;
    }

    private AiChatResponse buildSimpleResponse(String intent, String answer) {
        AiChatResponse response = new AiChatResponse();
        response.setIntent(intent);
        response.setAnswer(answer);
        return response;
    }

    private AiChatResponse createFallbackResponse() {
        AiChatResponse fallback = new AiChatResponse();
        fallback.setIntent("fallback");
        fallback.setAnswer("지금은 챗봇 응답을 안정적으로 가져오지 못했어요. 잠시 후 다시 시도해 주세요.");
        return fallback;
    }
}
