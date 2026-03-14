package org.poolpool.mohaeng.ai.chat.controller;

import org.poolpool.mohaeng.ai.chat.dto.AiChatRequest;
import org.poolpool.mohaeng.ai.chat.dto.AiChatResponse;
import org.poolpool.mohaeng.ai.chat.service.AiChatProxyService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatProxyService aiChatProxyService;

    @PostMapping("/chat")
    public AiChatResponse chat(
            @RequestBody AiChatRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader
    ) {
        return aiChatProxyService.chat(request, authorizationHeader);
    }
}