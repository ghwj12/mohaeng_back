package org.poolpool.mohaeng.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiModerationRequestDto {

    private String title;

    @JsonProperty("simple_explain")
    private String simpleExplain;

    private String description;

    @JsonProperty("lot_number_adr")
    private String lotNumberAdr;

    @JsonProperty("detail_adr")
    private String detailAdr;

    @JsonProperty("topic_ids")
    private String topicIds;

    @JsonProperty("hashtag_ids")
    private String hashtagIds;
}