package org.poolpool.mohaeng.event.mypage.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.poolpool.mohaeng.event.participation.dto.ParticipationBoothFacilityDto;

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
    private Long    pctBoothId;
    private Long    hostBoothId;
    private Long    userId;
    private String  homepageUrl;
    private String  boothTitle;
    private String  boothTopic;
    private String  mainItems;
    private String  description;
    private Integer boothCount;
    private Integer boothPrice;
    private Integer facilityPrice;
    private Integer totalPrice;
    private String  status;
    private LocalDateTime createdAt;
    private LocalDateTime approvedDate;

    // ✅ host_booth (선택한 부스 원본 정보)
    private String  boothName;      // 부스명
    private String  boothSize;      // 규격
    private String  boothNote;      // 부스 메모
    private Integer boothUnitPrice; // 부스 단가 (host 기준)
    private Integer boothTotal;     // 총 수량
    private Integer boothRemain;    // 잔여 수량

    // event
    private Long      eventId;
    private String    eventTitle;
    private String    eventThumbnail;
    private LocalDate startDate;
    private LocalDate endDate;

    // applicant(user)
    private String applicantName;
    private String applicantEmail;
    private String applicantPhone;
    private String applicantBusinessNum;

    // files
    private List<BoothApplicationFileResponse> files;

    // ✅ 선택한 부대시설 목록 (신청 정보 + 시설 원본 정보 전부)
    private List<ParticipationBoothFacilityDto> facilities;
}
