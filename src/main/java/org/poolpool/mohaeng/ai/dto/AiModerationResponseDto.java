package org.poolpool.mohaeng.ai.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiModerationResponseDto {

    @JsonProperty("risk_score")
    private BigDecimal riskScore;

    private List<String> reasons;

    private String summary;
}