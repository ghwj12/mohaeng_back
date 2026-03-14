package org.poolpool.mohaeng.ai.chat.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiChatResponse {
    private String answer;
    private String intent;
    private List<AiChatCardDto> cards = new ArrayList<>();
}