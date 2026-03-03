package org.poolpool.mohaeng.event.participation.controller;

import java.util.Map;

import org.poolpool.mohaeng.event.participation.service.EventParticipationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * ✅ Issue 6: 행사/부스 중복 신청 여부 확인 엔드포인트
 * GET /api/eventParticipation/check/{eventId}
 * → { "alreadyApplied": true/false, "alreadyBoothApplied": true/false }
 */
@RestController
@RequestMapping("/api/eventParticipation")
@RequiredArgsConstructor
public class EventParticipationCheckController {

    private final EventParticipationService participationService;

    @GetMapping("/check/{eventId}")
    public ResponseEntity<Map<String, Boolean>> checkParticipation(
            @PathVariable("eventId") Long eventId) {
        boolean alreadyApplied = false;
        boolean alreadyBoothApplied = false;
        try {
            alreadyApplied = participationService.hasActiveParticipation(eventId);
            alreadyBoothApplied = participationService.hasActiveBooth(eventId);
        } catch (Exception ignored) {
            // 비로그인 시 false 반환
        }
        return ResponseEntity.ok(Map.of(
                "alreadyApplied", alreadyApplied,
                "alreadyBoothApplied", alreadyBoothApplied
        ));
    }
}
