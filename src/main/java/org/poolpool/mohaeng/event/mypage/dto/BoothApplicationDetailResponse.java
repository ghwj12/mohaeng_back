package org.poolpool.mohaeng.event.mypage.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoothApplicationDetailResponse {

    // participation_booth
    private Long pctBoothId;
    private Long hostBoothId;
    private Long userId;
    private String homepageUrl;
    private String boothTitle;
    private String boothTopic;
    private String mainItems;
    private String description;
    private Integer boothCount;
    private Integer boothPrice;
    private Integer facilityPrice;
    private Integer totalPrice;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime approvedDate;

    // event
    private Long eventId;
    private String eventTitle;
    private String eventThumbnail;
    private LocalDate startDate;
    private LocalDate endDate;

    // applicant(user)
    private String applicantName;
    private String applicantEmail;
    private String applicantPhone;
    private String applicantBusinessNum;

    // files
    private List<BoothApplicationFileResponse> files;
}
