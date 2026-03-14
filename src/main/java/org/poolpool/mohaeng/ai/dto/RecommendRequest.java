package org.poolpool.mohaeng.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class RecommendRequest {
    @JsonProperty("user_text")
    private String userText;

    @JsonProperty("user_region_ids")
    private List<Long> userRegionIds;

    private List<EventEmbedding> events;
}