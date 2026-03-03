package org.poolpool.mohaeng.event.participation.controller;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.list.entity.FileEntity;
import org.poolpool.mohaeng.event.list.repository.EventRepository;
import org.poolpool.mohaeng.event.participation.dto.BoothApplyRequestDto;
import org.poolpool.mohaeng.event.participation.dto.BoothListResponseDto;
import org.poolpool.mohaeng.event.participation.entity.EventParticipationEntity;
import org.poolpool.mohaeng.event.participation.entity.ParticipationBoothEntity;
import org.poolpool.mohaeng.event.participation.entity.ParticipationBoothFacilityEntity;
import org.poolpool.mohaeng.event.participation.repository.EventParticipationRepository;
import org.poolpool.mohaeng.event.participation.service.EventParticipationService;
import org.poolpool.mohaeng.payment.entity.PaymentEntity;
import org.poolpool.mohaeng.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
    private final PaymentRepository paymentRepository;
    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    @Value("${upload.path.pbooth:C:/upload_files/pbooth}")
    private String pboothUploadPath;

    // ══════════════════════════════════════════════════════════════════════════
    //   부스 신청 생성
    //   POST /api/eventParticipation/submitBoothApply?eventId={eventId}
    //   consumes = multipart/form-data
    //   - data: JSON 문자열
    //   - files: 첨부파일 (optional)
    //   - orderId: 유료 결제 시 포함 (결제 성공 후 호출)
    // ══════════════════════════════════════════════════════════════════════════

    @PostMapping(value = "/submitBoothApply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> submitBoothApply(
            @RequestParam("eventId") Long eventId,
            @RequestParam("data") String dataJson,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "orderId", required = false) String orderId) {

        log.info("[부스 신청] eventId={}, orderId={}", eventId, orderId);
        log.info("[부스 신청] dataJson={}", dataJson);

        // JSON 파싱
        BoothApplyRequestDto dto;
        try {
            dto = objectMapper.readValue(dataJson, BoothApplyRequestDto.class);
        } catch (Exception e) {
            log.error("[부스 신청] JSON 파싱 실패: {}", dataJson, e);
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "요청 데이터 형식이 올바르지 않습니다: " + e.getMessage()));
        }

        // 유료 결제인 경우 orderId로 APPROVED 결제 확인
        PaymentEntity payment = null;
        if (orderId != null && !orderId.isBlank()) {
            payment = paymentRepository.findByPaymentKey(orderId).orElse(null);
            if (payment == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "결제 정보를 찾을 수 없습니다."));
            }
            if (!"APPROVED".equals(payment.getPaymentStatus())) {
                return ResponseEntity.badRequest().body(Map.of("message", "결제가 완료되지 않았습니다."));
            }
        }

        // 1) ParticipationBoothEntity 저장
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
        booth.setStatus(payment != null ? "결제완료" : "신청");

        em.persist(booth);
        em.flush();

        Long pctBoothId = booth.getPctBoothId();
        log.info("[부스 신청] pctBoothId={} 생성, status={}", pctBoothId, booth.getStatus());

        // 결제 엔티티에 pctBoothId 연결
        if (payment != null) {
            payment.setPctBoothId(pctBoothId);
            paymentRepository.save(payment);
        }

        // 2) 부대시설 저장 + 재고 차감
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

        // 3) 부스 재고 차감
        em.createQuery(
            "UPDATE HostBoothEntity h " +
            "SET h.remainCount = h.remainCount - 1 " +
            "WHERE h.boothId = :id AND h.remainCount > 0")
            .setParameter("id", dto.getHostBoothId())
            .executeUpdate();

        // 4) 첨부파일 저장
        if (files != null && !files.isEmpty()) {
            File uploadDir = new File(pboothUploadPath);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            // ✅ FileEntity.event_id NOT NULL → event 조회 필요
            EventEntity eventEntity = eventRepository.findById(eventId).orElse(null);

            String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                String originalName = file.getOriginalFilename();
                String ext = (originalName != null && originalName.contains("."))
                        ? originalName.substring(originalName.lastIndexOf(".")) : "";
                String saveName = datePart + "_" + UUID.randomUUID().toString().replace("-", "") + ext;
                try {
                    file.transferTo(new File(uploadDir, saveName));
                    FileEntity fileEntity = FileEntity.builder()
                            .event(eventEntity)   // ✅ event_id NOT NULL 해결
                            .pctBooth(booth)
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

        log.info("[부스 신청 완료] pctBoothId={}", pctBoothId);
        return ResponseEntity.ok(Map.of("pctBoothId", pctBoothId));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //   일반 행사 참여 생성
    // ══════════════════════════════════════════════════════════════════════════

    @PostMapping("/submitParticipation")
    @Transactional
    public ResponseEntity<?> submitParticipation(
            @RequestParam("eventId") Long eventId,
            @RequestParam(value = "orderId", required = false) String orderId,
            @RequestBody Map<String, Object> formData) {

        log.info("[행사 참여 신청] eventId={}, orderId={}", eventId, orderId);

        PaymentEntity payment = null;
        if (orderId != null && !orderId.isBlank()) {
            // confirm과 submit이 동시에 실행될 수 있어 APPROVED 상태가 될 때까지 최대 3초 재시도
            for (int retry = 0; retry < 6; retry++) {
                payment = paymentRepository.findByOrderId(orderId).orElse(null);
                if (payment != null && "APPROVED".equals(payment.getPaymentStatus())) break;
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            }
            if (payment == null)
                return ResponseEntity.badRequest().body(Map.of("message", "결제 정보를 찾을 수 없습니다."));
            if (!"APPROVED".equals(payment.getPaymentStatus()))
                return ResponseEntity.badRequest().body(Map.of("message", "결제가 완료되지 않았습니다. 잠시 후 다시 시도해주세요."));
        }

        EventParticipationEntity pct = new EventParticipationEntity();
        pct.setEventId(eventId);
        pct.setUserId(getCurrentUserId());
        pct.setPctStatus(payment != null ? "결제완료" : "참여확정");

        if (formData != null) {
            if (formData.get("pctGender")    != null) pct.setPctGender(String.valueOf(formData.get("pctGender")));
            if (formData.get("pctAgeGroup")  != null) pct.setPctAgeGroup(String.valueOf(formData.get("pctAgeGroup")));
            if (formData.get("pctJob")       != null) pct.setPctJob(String.valueOf(formData.get("pctJob")));
            if (formData.get("pctRoot")      != null) pct.setPctRoot(String.valueOf(formData.get("pctRoot")));
            if (formData.get("pctGroup")     != null) pct.setPctGroup(String.valueOf(formData.get("pctGroup")));
            if (formData.get("pctRank")      != null) pct.setPctRank(String.valueOf(formData.get("pctRank")));
            if (formData.get("pctIntroduce") != null) pct.setPctIntroduce(String.valueOf(formData.get("pctIntroduce")));
            if (formData.get("pctDate")      != null) {
                try {
                    pct.setPctDate(java.time.LocalDate.parse(
                            String.valueOf(formData.get("pctDate"))).atStartOfDay());
                } catch (Exception ignored) {}
            }
        }

        em.persist(pct);
        em.flush();

        Long pctId = pct.getPctId();

        if (payment != null) {
            payment.setPctId(pctId);
            paymentRepository.save(payment);
        }

        log.info("[행사 참여 완료] pctId={}, status={}", pctId, pct.getPctStatus());
        return ResponseEntity.ok(Map.of("pctId", pctId));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //   취소
    // ══════════════════════════════════════════════════════════════════════════

    @DeleteMapping("/cancelParticipation")
    public ResponseEntity<?> cancelParticipation(@RequestParam("pctId") Long pctId) {
        participationService.cancelParticipation(pctId);
        return ResponseEntity.ok(Map.of("message", "참여 취소가 완료되었습니다."));
    }

    @PutMapping("/deleteParticipation")
    @Transactional
    public ResponseEntity<?> deleteParticipation(@RequestParam("pctId") Long pctId) {
        Long userId = getCurrentUserId();
        int updated = em.createQuery(
            "UPDATE EventParticipationEntity p SET p.pctStatus = '참여삭제' " +
            "WHERE p.pctId = :pctId AND p.userId = :userId")
            .setParameter("pctId", pctId)
            .setParameter("userId", userId)
            .executeUpdate();
        if (updated == 0)
            return ResponseEntity.badRequest().body(Map.of("message", "삭제할 수 없는 참여 내역입니다."));
        return ResponseEntity.ok(Map.of("message", "삭제되었습니다."));
    }

    @DeleteMapping("/cancelBoothParticipation")
    public ResponseEntity<?> cancelBoothParticipation(@RequestParam("pctBoothId") Long pctBoothId) {
        participationService.cancelBoothParticipation(pctBoothId);
        return ResponseEntity.ok(Map.of("message", "부스 취소가 완료되었습니다."));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //   주최자 부스 관리
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/boothList/{eventId}")
    public ResponseEntity<?> getBoothList(@PathVariable Long eventId) {
        List<ParticipationBoothEntity> list = participationRepository.findBoothsByEventId(eventId);
        return ResponseEntity.ok(list.stream().map(BoothListResponseDto::fromEntity).toList());
    }

    @PutMapping("/approveBooth")
    public ResponseEntity<?> approveBooth(@RequestParam Long pctBoothId) {
        participationService.approveBooth(pctBoothId);
        return ResponseEntity.ok(Map.of("message", "부스 신청이 승인되었습니다."));
    }

    @PutMapping("/rejectBooth")
    public ResponseEntity<?> rejectBooth(@RequestParam Long pctBoothId) {
        participationService.rejectBooth(pctBoothId);
        return ResponseEntity.ok(Map.of("message", "부스 신청이 반려되었습니다."));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //   마이페이지
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/myParticipations")
    public ResponseEntity<?> myParticipations() {
        Long userId = getCurrentUserId();
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        return ResponseEntity.ok(participationRepository.findParticipationsByUserId(userId));
    }

    @GetMapping("/myBoothParticipations")
    public ResponseEntity<?> myBoothParticipations() {
        Long userId = getCurrentUserId();
        if (userId == null) return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        return ResponseEntity.ok(participationRepository.findBoothsByUserId(userId));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //   유틸
    // ══════════════════════════════════════════════════════════════════════════

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
