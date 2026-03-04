package org.poolpool.mohaeng.event.mypage.dto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoothMypageResponse {
    // participation_booth
    private Long pctBoothId;
    private Long hostBoothId;
    private String boothTitle;
    private String boothTopic;
    private Integer boothCount;
    private Integer totalPrice;
    private String status;
    private LocalDateTime createdAt;
    // event
    private Long eventId;
    private String eventTitle;
    private String eventThumbnail;
    private String eventDescription;
    private LocalDate startDate;
    private LocalDate endDate;
    private String eventStatus; // ✅ 추가: DELETED / REPORTDELETED 판별용
}
