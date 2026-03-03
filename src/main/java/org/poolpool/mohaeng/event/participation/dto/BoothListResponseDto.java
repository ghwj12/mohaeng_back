package org.poolpool.mohaeng.event.participation.dto;

import org.poolpool.mohaeng.event.participation.entity.ParticipationBoothEntity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoothListResponseDto {

    private Long pctBoothId;
    private Long hostBoothId;
    private Long userId;
    private String boothTitle;
    private String status;       // 신청, 결제완료, 승인, 반려, 취소

    /**
     * ✅ 문제 2: status가 "취소"/"반려"/"승인"이면 승인/반려 버튼 숨기기
     */
    private boolean canApprove;
    private boolean canReject;

    private String approvedDate;

    public static BoothListResponseDto fromEntity(ParticipationBoothEntity entity) {
        String status = entity.getStatus();

        // ✅ 문제 2: 신청/결제완료 상태만 승인/반려 가능
        boolean canApprove = "신청".equals(status) || "결제완료".equals(status);
        boolean canReject  = canApprove;

        return BoothListResponseDto.builder()
                .pctBoothId(entity.getPctBoothId())
                .hostBoothId(entity.getHostBoothId())
                .userId(entity.getUserId())
                .boothTitle(entity.getBoothTitle())
                .status(status)
                .canApprove(canApprove)
                .canReject(canReject)
                .approvedDate(entity.getApprovedDate() != null
                        ? entity.getApprovedDate().toString() : null)
                .build();
    }
}
