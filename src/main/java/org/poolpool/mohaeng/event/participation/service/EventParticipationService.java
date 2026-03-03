package org.poolpool.mohaeng.event.participation.service;

import java.util.List;

import org.poolpool.mohaeng.event.participation.dto.EventParticipationDto;

public interface EventParticipationService {

    // ─── 일반 행사 참여 ───
    Long submitParticipation(Long userId, Long eventId);
    void cancelParticipation(Long pctId);

    // ─── 부스 신청 ───
    Long submitBoothParticipation(Long userId, Long eventId, Object dto);
    void cancelBoothParticipation(Long pctBoothId);
    void approveBooth(Long pctBoothId);
    void rejectBooth(Long pctBoothId);

    // ─── 마이페이지 (MypageEventController 호출) ───
    List<EventParticipationDto> getParticipationList(Long userId);

    // ─── 중복 신청 여부 확인 (EventParticipationCheckController 호출) ───
    boolean hasActiveParticipation(Long eventId);
    boolean hasActiveBooth(Long eventId);
}
