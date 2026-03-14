package org.poolpool.mohaeng.ai.chat.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiChatCardDto {
    private Long eventId;
    private String title;
    private String description;
    private String region;
    private String startDate;
    private String endDate;
    private String thumbnail;
    private String eventStatus;
    private String detailUrl;
    private String applyUrl;
}