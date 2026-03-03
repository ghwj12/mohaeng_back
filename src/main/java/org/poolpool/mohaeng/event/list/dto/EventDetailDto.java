package org.poolpool.mohaeng.event.list.dto;

import lombok.*;
import org.poolpool.mohaeng.event.host.dto.HostBoothDto;
import org.poolpool.mohaeng.event.host.dto.HostFacilityDto;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class EventDetailDto {
    // 1. 행사 메인 정보
    private EventDto eventInfo;

    // 주최자 정보
    private Long hostId;
    private String hostName;
    private String hostEmail;
    private String hostPhone;
    private String hostPhoto;          // ✅ Issue 2: 주최자 프로필 사진

    // 2. 부스 목록
    private List<HostBoothDto> booths;

    // 3. 부대시설 목록
    private List<HostFacilityDto> facilities;

    // ✅ Issue 6: 현재 로그인 유저의 중복 신청 여부
    private Boolean alreadyApplied;      // 행사 참여 신청 중복 여부
    private Boolean alreadyBoothApplied; // 부스 신청 중복 여부
}
