package org.poolpool.mohaeng.event.participation.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.poolpool.mohaeng.common.config.UploadProperties;
import org.poolpool.mohaeng.common.util.FileNameChange;
import org.poolpool.mohaeng.event.host.repository.FileRepository;
import org.poolpool.mohaeng.event.list.dto.EventDetailDto;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.list.entity.FileEntity;
import org.poolpool.mohaeng.event.list.repository.EventRepository;
import org.poolpool.mohaeng.event.list.service.EventService;
import org.poolpool.mohaeng.event.participation.dto.EventParticipationDto;
import org.poolpool.mohaeng.event.participation.dto.ParticipationBoothDto;
import org.poolpool.mohaeng.event.participation.dto.ParticipationBoothFacilityDto;
import org.poolpool.mohaeng.event.participation.entity.EventParticipationEntity;
import org.poolpool.mohaeng.event.participation.entity.ParticipationBoothEntity;
import org.poolpool.mohaeng.event.participation.entity.ParticipationBoothFacilityEntity;
import org.poolpool.mohaeng.event.participation.repository.EventParticipationRepository;
import org.poolpool.mohaeng.auth.security.principal.CustomUserPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventParticipationServiceImpl implements EventParticipationService {

    private final EventParticipationRepository repo;
    private final FileRepository fileRepository;
    private final UploadProperties uploadProperties;
    private final EventRepository eventRepository;
    private final EventService eventService;

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof CustomUserPrincipal cup) {
            return Long.parseLong(cup.getUserId());
        }
        if (principal instanceof UserDetails ud) {
            return Long.parseLong(ud.getUsername());
        }
        if (principal instanceof String s) {
            return Long.parseLong(s);
        }

        throw new IllegalStateException("인증 정보를 찾을 수 없습니다. principal=" + principal);
    }

    // =========================
    // 행사 참여(신청)
    // =========================
    @Override
    @Transactional(readOnly = true)
    public List<EventParticipationDto> getParticipationList(Long userId) {
        return repo.findParticipationByUserId(userId)
                .stream()
                .map(EventParticipationDto::fromEntity)
                .toList();
    }

    /**
     * 행사 참가 신청 제출
     * - 무료 행사: 바로 '결제완료' (통계에 즉시 반영)
     * - 유료 행사: '결제대기' → 결제 승인 후 PaymentService에서 '결제완료'로 업데이트
     */
    @Override
    @Transactional
    public Long submitParticipation(EventParticipationDto dto) {
        Long userId = getCurrentUserId();
        dto.setUserId(userId);

        // ✅ Issue 6: 행사 참여 중복 신청 방지
        if (repo.existsActiveParticipation(userId, dto.getEventId())) {
            throw new IllegalStateException("이미 신청한 행사입니다.");
        }

        // 행사 가격 확인
        EventEntity event = eventRepository.findById(dto.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 행사입니다."));

        EventParticipationEntity e = dto.toEntity();

        boolean isFree = (event.getPrice() == null || event.getPrice() == 0);
        // 무료: 결제완료 / 유료: 결제대기 (결제 성공 시 PaymentService가 결제완료로 변경)
        e.setPctStatus(isFree ? "결제완료" : "결제대기");

        EventParticipationEntity saved = repo.saveParticipation(e);
        return saved.getPctId();
    }

    @Override
    @Transactional(readOnly = true)
    public EventDetailDto getEventDetail(Long eventId) {
        return eventService.getEventDetail(eventId, false);
    }

    // ✅ Issue 6: 현재 유저의 행사/부스 신청 여부 확인
    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveParticipation(Long eventId) {
        Long userId = getCurrentUserId();
        return repo.existsActiveParticipation(userId, eventId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveBooth(Long eventId) {
        Long userId = getCurrentUserId();
        return repo.existsActiveBooth(userId, eventId);
    }

    @Override
    @Transactional
    public void cancelParticipation(Long pctId) {
        EventParticipationEntity e = repo.findParticipationById(pctId)
                .orElseThrow(() -> new IllegalArgumentException("참여 신청 없음"));
        e.setPctStatus("취소");
        repo.saveParticipation(e);
    }

    @Override
    @Transactional
    public void deleteParticipation(Long pctId) {
        deleteParticipation(pctId, getCurrentUserId());
    }

    @Override
    @Transactional
    public void deleteParticipation(Long pctId, Long userId) {
        EventParticipationEntity e = repo.findParticipationById(pctId)
                .orElseThrow(() -> new IllegalArgumentException("참여 신청 없음"));

        if (e.getUserId() == null || !e.getUserId().equals(userId)) {
            throw new IllegalStateException("본인 참여내역만 삭제할 수 있습니다.");
        }

        e.setPctStatus("참여삭제");
        repo.saveParticipation(e);
    }

    // =========================
    // 부스 참여 신청
    // =========================
    @Override
    @Transactional(readOnly = true)
    public List<ParticipationBoothDto> getParticipationBoothList(Long userId) {
        return repo.findBoothByUserId(userId)
                .stream()
                .map(ParticipationBoothDto::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public void cancelBoothParticipation(Long pctBoothId) {
        ParticipationBoothEntity booth = repo.findBoothById(pctBoothId)
                .orElseThrow(() -> new IllegalArgumentException("부스 신청 없음"));
        booth.setStatus("취소");
        booth.setUpdatedAt(LocalDateTime.now());
        repo.saveBooth(booth);
    }

    /**
     * 부스 참가 신청 제출
     * - 무료(boothPrice=0): 바로 '결제완료'
     * - 유료: '신청' → 결제 승인 후 PaymentService에서 '결제완료'로 변경
     */
    @Override
    @Transactional
    public Long submitBoothApply(Long eventId, ParticipationBoothDto dto, List<MultipartFile> files) {
        validateEventId(eventId, dto.getHostBoothId());

        Long userId = getCurrentUserId();

        // ✅ Issue 6: 부스 중복 신청 방지
        if (repo.existsActiveBooth(userId, eventId)) {
            throw new IllegalStateException("이미 신청한 부스입니다.");
        }

        ParticipationBoothEntity booth = dto.toEntity();
        booth.setUserId(userId);

        // 부스 가격 확인 (무료면 바로 결제완료)
        boolean isFree = (dto.getBoothPrice() == null || dto.getBoothPrice() == 0)
                      && (dto.getTotalPrice() == null || dto.getTotalPrice() == 0);
        booth.setStatus(isFree ? "결제완료" : "신청");

        ParticipationBoothEntity savedBooth = repo.saveBooth(booth);

        saveFacilities(savedBooth.getPctBoothId(), dto.getFacilities());
        saveFiles(savedBooth, files, eventId);

        // 부스 잔여수량 차감
        repo.decreaseBoothRemainCount(dto.getHostBoothId());

        // 부대시설 잔여수량 차감
        if (dto.getFacilities() != null) {
            for (ParticipationBoothFacilityDto faci : dto.getFacilities()) {
                repo.decreaseFacilityRemainCount(faci.getHostBoothFaciId(), faci.getFaciCount());
            }
        }

        return savedBooth.getPctBoothId();
    }

    // =========================
    // 내부 유틸
    // =========================
    private void validateEventId(Long eventId, Long hostBoothId) {
        Long realEventId = repo.findEventIdByHostBoothId(hostBoothId)
                .orElseThrow(() -> new IllegalArgumentException("HOST_BOOTH 없음"));
        if (!realEventId.equals(eventId)) {
            throw new IllegalArgumentException("hostBoothId가 eventId에 속하지 않습니다.");
        }
    }

    private void saveFiles(ParticipationBoothEntity pctBooth, List<MultipartFile> files, Long eventId) {
        if (files == null || files.isEmpty()) return;

        Path pboothDir = uploadProperties.pboothDir();

        try {
            if (!Files.exists(pboothDir)) {
                Files.createDirectories(pboothDir);
            }

            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                if (file.isEmpty()) continue;

                String originalName = file.getOriginalFilename();
                String renameName = FileNameChange.change(originalName, FileNameChange.RenameStrategy.DATETIME_UUID);
                Path filePath = pboothDir.resolve(renameName);

                EventEntity event = eventRepository.findById(eventId)
                        .orElseThrow(() -> new IllegalArgumentException("행사 없음"));

                file.transferTo(filePath.toFile());

                FileEntity fileEntity = FileEntity.builder()
                        .pctBooth(pctBooth)
                        .event(event)
                        .fileType("P_BOOTH")
                        .originalFileName(originalName)
                        .renameFileName(renameName)
                        .sortOrder(i + 1)
                        .createdAt(LocalDateTime.now())
                        .build();

                fileRepository.save(fileEntity);
            }
        } catch (IOException e) {
            throw new RuntimeException("참여 부스 첨부파일 업로드 중 오류가 발생했습니다.", e);
        }
    }

    private void saveFacilities(Long pctBoothId, List<ParticipationBoothFacilityDto> facilities) {
        repo.deleteFacilitiesByPctBoothId(pctBoothId);
        if (facilities == null || facilities.isEmpty()) return;

        List<ParticipationBoothFacilityEntity> entities = facilities.stream()
                .map(f -> f.toEntity(pctBoothId))
                .toList();

        repo.saveFacilities(entities);
    }
}
