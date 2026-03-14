package org.poolpool.mohaeng.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventEmbedding {
    @JsonProperty("event_id")
    private Long eventId;

    private String embedding;

    @JsonProperty("region_id")
    private Long regionId;
}