package org.poolpool.mohaeng.ai.chat.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiChatSourceDto {
    private String type;
    private String title;
    private String snippet;
}
