package org.poolpool.mohaeng.event.participation.controller;

import org.poolpool.mohaeng.event.list.entity.EventEntity;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.poolpool.mohaeng.event.list.entity.FileEntity;
import org.poolpool.mohaeng.event.participation.dto.BoothApplyRequestDto;
import org.poolpool.mohaeng.event.participation.dto.BoothListResponseDto;
import org.poolpool.mohaeng.event.participation.entity.EventParticipationEntity;
import org.poolpool.mohaeng.event.participation.entity.ParticipationBoothEntity;
import org.poolpool.mohaeng.event.participation.entity.ParticipationBoothFacilityEntity;
import org.poolpool.mohaeng.event.participation.repository.EventParticipationRepository;
import org.poolpool.mohaeng.event.participation.service.EventParticipationService;

//  부스 알림 추가
import org.poolpool.mohaeng.notification.service.NotificationService;
import org.poolpool.mohaeng.notification.type.NotiTypeId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/eventParticipation")
@RequiredArgsConstructor
public class EventParticipationController {

    private final EventParticipationService participationService;
    private final EventParticipationRepository participationRepository;
    private final ObjectMapper objectMapper;

    //  알림 서비스 주입
    private final NotificationService notificationService;

    @PersistenceContext
    private EntityManager em;

    @Value("${upload.path.pbooth:C:/upload_files/pbooth}")
    private String pboothUploadPath;

    // ══════════════════════════════════════
    //   부스 신청 제출
    //   POST /api/eventParticipation/submitBoothApply?eventId={eventId}
    // ══════════════════════════════════════
    @PostMapping("/submitBoothApply")
    @Transactional
    public ResponseEntity<?> submitBoothApply(
            @RequestParam("eventId") Long eventId,
            @RequestParam("data") String dataJson, // 프론트에서 보낸 'data' (JSON String)
            @RequestParam(value = "files", required = false) List<MultipartFile> files) { // 첨부파일 직접 받기

        log.info("[부스 신청] eventId={}, dataJson={}", eventId, dataJson);

        if (dataJson == null || dataJson.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "data 파라미터가 없습니다."));
        }

        // ── 1) JSON 파싱 ──────────────────────────────
        BoothApplyRequestDto dto;
        try {
            dto = objectMapper.readValue(dataJson, BoothApplyRequestDto.class);
        } catch (Exception e) {
            log.error("[부스 신청] JSON 파싱 실패: {}", dataJson, e);
            return ResponseEntity.badRequest().body(Map.of("message", "요청 데이터 형식이 올바르지 않습니다: " + e.getMessage()));
        }

        // ── 2) ParticipationBoothEntity 저장 ─────────
        ParticipationBoothEntity booth = new ParticipationBoothEntity();
        booth.setHostBoothId(dto.getHostBoothId());
        booth.setUserId(getCurrentUserId());
        booth.setHomepageUrl(dto.getHomepageUrl());
        booth.setBoothTitle(dto.getBoothTitle());
        booth.setBoothTopic(dto.getBoothTopic());
        booth.setMainItems(dto.getMainItems());
        booth.setDescription(dto.getDescription() != null ? dto.getDescription() : "");
        booth.setBoothCount(dto.getBoothCount() != null ? dto.getBoothCount() : 1);
        booth.setBoothPrice(dto.getBoothPrice() != null ? dto.getBoothPrice() : 0);
        booth.setFacilityPrice(dto.getFacilityPrice() != null ? dto.getFacilityPrice() : 0);
        booth.setTotalPrice(dto.getTotalPrice() != null ? dto.getTotalPrice() : 0);
        booth.setStatus(booth.getTotalPrice() > 0 ? "결제대기" : "신청");

        em.persist(booth);
        em.flush();

        Long pctBoothId = booth.getPctBoothId();
        log.info("[부스 신청] pctBoothId={} 생성됨", pctBoothId);

        // ── 3) 부대시설 저장 + 재고 차감 ──────────────
        if (dto.getFacilities() != null) {
            for (BoothApplyRequestDto.FacilityItem fi : dto.getFacilities()) {
                if (fi.getHostBoothFaciId() == null) continue;

                ParticipationBoothFacilityEntity faci = new ParticipationBoothFacilityEntity();
                faci.setHostBoothFaciId(fi.getHostBoothFaciId());
                faci.setPctBoothId(pctBoothId);
                faci.setFaciCount(fi.getFaciCount() != null ? fi.getFaciCount() : 1);
                faci.setFaciPrice(fi.getFaciPrice() != null ? fi.getFaciPrice() : 0);
                em.persist(faci);

                em.createQuery(
                    "UPDATE HostFacilityEntity h " +
                    "SET h.remainCount = h.remainCount - :cnt " +
                    "WHERE h.hostBoothfaciId = :id AND h.remainCount >= :cnt")
                    .setParameter("id", fi.getHostBoothFaciId())
                    .setParameter("cnt", faci.getFaciCount())
                    .executeUpdate();
            }
        }

        // ── 4) 부스 재고 차감 ─────────────────────────
        em.createQuery(
            "UPDATE HostBoothEntity h " +
            "SET h.remainCount = h.remainCount - 1 " +
            "WHERE h.boothId = :id AND h.remainCount > 0")
            .setParameter("id", dto.getHostBoothId())
            .executeUpdate();

        // ── 5) 첨부파일 저장 ──────────────────────────
        if (files != null && !files.isEmpty()) {
            File uploadDir = new File(pboothUploadPath);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            String datePart = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            EventEntity eventRef = em.getReference(EventEntity.class, eventId);

            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;

                String originalName = file.getOriginalFilename();
                String ext = "";
                if (originalName != null && originalName.contains(".")) {
                    ext = originalName.substring(originalName.lastIndexOf("."));
                }
                String saveName = datePart + "_"
                        + UUID.randomUUID().toString().replace("-", "") + ext;

                try {
                    file.transferTo(new File(uploadDir, saveName));

                    FileEntity fileEntity = FileEntity.builder()
                            .pctBooth(booth)
                            .event(eventRef)
                            .fileType("P_BOOTH")
                            .originalFileName(originalName)
                            .renameFileName(saveName)
                            .sortOrder(0)
                            .createdAt(LocalDateTime.now())
                            .build();

                    em.persist(fileEntity);
                    log.info("[부스파일 저장] {} → {}", originalName, saveName);

                } catch (IOException e) {
                    log.error("[부스파일 저장 실패] {}", originalName, e);
                }
            }
        }

        //  6) 부스 신청 알림(8): 주최자에게
        try {
            EventEntity event = em.find(EventEntity.class, eventId);
            Long hostId = (event != null && event.getHost() != null) ? event.getHost().getUserId() : null;
            Long applicantId = booth.getUserId();

            if (hostId != null && applicantId != null && !hostId.equals(applicantId)) {
                notificationService.create(hostId, NotiTypeId.BOOTH_RECEIVER, eventId, null);
                log.info("[BOOTH_NOTI] created type=8 hostId={} eventId={} pctBoothId={}", hostId, eventId, pctBoothId);
            } else {
                log.info("[BOOTH_NOTI] skipped type=8 hostId={} applicantId={} eventId={}", hostId, applicantId, eventId);
            }
        } catch (Exception e) {
            log.error("[BOOTH_NOTI] failed type=8 eventId={} pctBoothId={}", eventId, pctBoothId, e);
        }

        log.info("[부스 신청 완료] pctBoothId={}, userId={}, eventId={}",
                pctBoothId, getCurrentUserId(), eventId);
        return ResponseEntity.ok(Map.of("pctBoothId", pctBoothId));
    }

    // ══════════════════════════════════════
    //   일반 행사 참여 취소
    // ══════════════════════════════════════
    @DeleteMapping("/cancelParticipation")
    public ResponseEntity<?> cancelParticipation(@RequestParam Long pctId) {
        participationService.cancelParticipation(pctId);
        return ResponseEntity.ok(Map.of("message", "참여 취소가 완료되었습니다."));
    }

    // ══════════════════════════════════════
    //   부스 취소
    // ══════════════════════════════════════
    @DeleteMapping("/cancelBoothParticipation")
    public ResponseEntity<?> cancelBoothParticipation(@RequestParam("pctBoothId") Long pctBoothId) {
        participationService.cancelBoothParticipation(pctBoothId);
        return ResponseEntity.ok(Map.of("message", "부스 취소가 완료되었습니다."));
    }

    // ══════════════════════════════════════
    //   주최자 부스 관리
    // ══════════════════════════════════════
    @GetMapping("/boothList/{eventId}")
    public ResponseEntity<?> getBoothList(@PathVariable Long eventId) {
        List<ParticipationBoothEntity> list =
                participationRepository.findBoothsByEventId(eventId);
        List<BoothListResponseDto> response = list.stream()
                .map(BoothListResponseDto::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    //  승인 + 알림(9)
    @PutMapping("/approveBooth")
    @Transactional
    public ResponseEntity<?> approveBooth(@RequestParam("pctBoothId") Long pctBoothId) {
        participationService.approveBooth(pctBoothId);

        try {
            ParticipationBoothEntity booth = participationRepository.findBoothById(pctBoothId).orElse(null);
            Long eventId = participationRepository.findEventIdByPctBoothId(pctBoothId);

            if (booth != null && booth.getUserId() != null && eventId != null) {
                notificationService.create(booth.getUserId(), NotiTypeId.BOOTH_ACCEPT, eventId, null);
                log.info("[BOOTH_NOTI] created type=9 applicantId={} eventId={} pctBoothId={}",
                        booth.getUserId(), eventId, pctBoothId);
            } else {
                log.info("[BOOTH_NOTI] skipped type=9 boothNull={} eventId={} pctBoothId={}",
                        (booth == null), eventId, pctBoothId);
            }
        } catch (Exception e) {
            log.error("[BOOTH_NOTI] failed type=9 pctBoothId={}", pctBoothId, e);
        }

        return ResponseEntity.ok(Map.of("message", "부스 신청이 승인되었습니다."));
    }

    //  반려 + 알림(10)
    @PutMapping("/rejectBooth")
    @Transactional
    public ResponseEntity<?> rejectBooth(@RequestParam("pctBoothId") Long pctBoothId) {
        participationService.rejectBooth(pctBoothId);

        try {
            ParticipationBoothEntity booth = participationRepository.findBoothById(pctBoothId).orElse(null);
            Long eventId = participationRepository.findEventIdByPctBoothId(pctBoothId);

            if (booth != null && booth.getUserId() != null && eventId != null) {
                notificationService.create(booth.getUserId(), NotiTypeId.BOOTH_REJECT, eventId, null);
                log.info("[BOOTH_NOTI] created type=10 applicantId={} eventId={} pctBoothId={}",
                        booth.getUserId(), eventId, pctBoothId);
            } else {
                log.info("[BOOTH_NOTI] skipped type=10 boothNull={} eventId={} pctBoothId={}",
                        (booth == null), eventId, pctBoothId);
            }
        } catch (Exception e) {
            log.error("[BOOTH_NOTI] failed type=10 pctBoothId={}", pctBoothId, e);
        }

        return ResponseEntity.ok(Map.of("message", "부스 신청이 반려되었습니다. 결제금액이 전액 환불됩니다."));
    }

    // ══════════════════════════════════════
    //   마이페이지
    // ══════════════════════════════════════
    @GetMapping("/myParticipations")
    public ResponseEntity<?> myParticipations(@RequestParam Long userId) {
        List<EventParticipationEntity> list =
                participationRepository.findParticipationsByUserId(userId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/myBoothParticipations")
    public ResponseEntity<?> myBoothParticipations(@RequestParam Long userId) {
        List<ParticipationBoothEntity> list =
                participationRepository.findBoothsByUserId(userId);
        return ResponseEntity.ok(list);
    }

    // ══════════════════════════════════════
    //   유틸
    // ══════════════════════════════════════
    private Long getCurrentUserId() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return null;
            return Long.parseLong(auth.getName());
        } catch (Exception e) {
            return null;
        }
    }
}