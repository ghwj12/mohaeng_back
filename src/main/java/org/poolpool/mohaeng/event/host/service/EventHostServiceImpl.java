package org.poolpool.mohaeng.event.host.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.poolpool.mohaeng.ai.client.AiModerationClient;
import org.poolpool.mohaeng.ai.dto.AiModerationRequestDto;
import org.poolpool.mohaeng.ai.dto.AiModerationResponseDto;
import org.poolpool.mohaeng.event.host.dto.EventCreateDto;
import org.poolpool.mohaeng.event.host.dto.HostEventMypageResponse;
import org.poolpool.mohaeng.event.host.dto.HostEventSummaryDto;
import org.poolpool.mohaeng.event.host.entity.HostBoothEntity;
import org.poolpool.mohaeng.event.host.entity.HostFacilityEntity;
import org.poolpool.mohaeng.event.host.repository.FileRepository;
import org.poolpool.mohaeng.event.host.repository.HostBoothRepository;
import org.poolpool.mohaeng.event.host.repository.HostFacilityRepository;
import org.poolpool.mohaeng.event.list.dto.EventDto;
import org.poolpool.mohaeng.event.list.entity.EventCategoryEntity;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.list.entity.EventRegionEntity;
import org.poolpool.mohaeng.event.list.entity.FileEntity;
import org.poolpool.mohaeng.event.list.repository.EventCategoryRepository;
import org.poolpool.mohaeng.event.list.repository.EventRegionRepository;
import org.poolpool.mohaeng.event.list.repository.EventRepository;
import org.poolpool.mohaeng.storage.s3.S3StorageService;
import org.poolpool.mohaeng.user.entity.UserEntity;
import org.poolpool.mohaeng.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventHostServiceImpl implements EventHostService {

    private final EventRepository eventRepository;
    private final HostBoothRepository hostBoothRepository;
    private final HostFacilityRepository hostFacilityRepository;
    private final FileRepository fileRepository;
    private final EventCategoryRepository eventCategoryRepository;
    private final EventRegionRepository eventRegionRepository;
    private final S3StorageService s3StorageService;
    private final UserRepository userRepository;
    private final AiModerationClient aiModerationClient;

    @Value("${ai.moderation.threshold:0.50}")
    private BigDecimal moderationThreshold;

    @Override
    @Transactional
    public Long createEventWithDetails(EventCreateDto createDto, Long hostId,
            MultipartFile thumbnail,
            List<MultipartFile> detailFiles,
            List<MultipartFile> boothFiles) {

        EventDto eventDto = createDto.getEventInfo();
        EventEntity eventEntity = eventDto.toEntity();

        // 카테고리
        if (eventDto.getCategory() != null && eventDto.getCategory().getCategoryId() != null) {
            EventCategoryEntity category = eventCategoryRepository.findById(eventDto.getCategory().getCategoryId())
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 카테고리입니다."));
            eventEntity.setCategory(category);
        } else {
            throw new RuntimeException("카테고리 정보가 누락되었습니다.");
        }

        // 지역
        if (eventDto.getRegion() != null && eventDto.getRegion().getRegionId() != null) {
            EventRegionEntity region = eventRegionRepository.findById(eventDto.getRegion().getRegionId())
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 지역입니다."));
            eventEntity.setRegion(region);
        }

        // 주최자
        UserEntity host = userRepository.findById(hostId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 유저입니다."));
        eventEntity.setHost(host);

        // 실제 전달된 부스/시설 기준으로 override
        boolean actualHasBooth = createDto.getBooths() != null && !createDto.getBooths().isEmpty();
        boolean actualHasFacility = createDto.getFacilities() != null && !createDto.getFacilities().isEmpty();
        eventEntity.setHasBooth(actualHasBooth);
        eventEntity.setHasFacility(actualHasFacility);

        // 부스가 없으면 모집 기간 null 처리
        if (!actualHasBooth) {
            eventEntity.setBoothStartRecruit(null);
            eventEntity.setBoothEndRecruit(null);
        }

        // 썸네일 저장(S3)
        if (thumbnail != null && !thumbnail.isEmpty()) {
            try {
                String savedName = s3StorageService.upload(thumbnail, "event");
                eventEntity.setThumbnail(savedName);
            } catch (Exception e) {
                throw new RuntimeException("썸네일 업로드 실패", e);
            }
        }

        // AI 검수
        applyAiModeration(eventEntity);

        // 행사 저장
        EventEntity savedEvent = eventRepository.save(eventEntity);
        Long eventId = savedEvent.getEventId();

        // 상세 이미지 저장(S3)
        if (detailFiles != null && !detailFiles.isEmpty()) {
            saveMultiFiles(detailFiles, savedEvent, "EVENT", "event");
        }

        // 부스 첨부파일 저장
        if (actualHasBooth && boothFiles != null && !boothFiles.isEmpty()) {
            saveMultiFiles(boothFiles, savedEvent, "HBOOTH", "host-booth");
        }

        // 부스 저장
        if (actualHasBooth) {
            List<HostBoothEntity> boothEntities = createDto.getBooths().stream()
                    .filter(dto -> dto.getBoothName() != null && !dto.getBoothName().isBlank())
                    .map(dto -> HostBoothEntity.builder()
                            .eventId(eventId)
                            .boothName(dto.getBoothName())
                            .boothPrice(dto.getBoothPrice())
                            .boothSize(dto.getBoothSize())
                            .boothNote(dto.getBoothNote())
                            .totalCount(dto.getTotalCount())
                            .remainCount(dto.getTotalCount())
                            .build())
                    .collect(Collectors.toList());
            if (!boothEntities.isEmpty()) {
                hostBoothRepository.saveAll(boothEntities);
            }
        }

        // 부대시설 저장
        if (actualHasFacility) {
            List<HostFacilityEntity> facilityEntities = createDto.getFacilities().stream()
                    .filter(dto -> dto.getFaciName() != null && !dto.getFaciName().isBlank())
                    .map(dto -> HostFacilityEntity.builder()
                            .eventId(eventId)
                            .faciName(dto.getFaciName())
                            .faciPrice(dto.getFaciPrice())
                            .faciUnit(dto.getFaciUnit())
                            .hasCount(dto.getHasCount())
                            .totalCount(dto.getHasCount() ? dto.getTotalCount() : null)
                            .remainCount(dto.getHasCount() ? dto.getTotalCount() : null)
                            .build())
                    .collect(Collectors.toList());
            if (!facilityEntities.isEmpty()) {
                hostFacilityRepository.saveAll(facilityEntities);
            }
        }

        return eventId;
    }

    private void applyAiModeration(EventEntity eventEntity) {
        try {
            AiModerationRequestDto requestDto = AiModerationRequestDto.builder()
                    .title(eventEntity.getTitle())
                    .simpleExplain(eventEntity.getSimpleExplain())
                    .description(eventEntity.getDescription())
                    .lotNumberAdr(eventEntity.getLotNumberAdr())
                    .detailAdr(eventEntity.getDetailAdr())
                    .topicIds(eventEntity.getTopicIds())
                    .hashtagIds(eventEntity.getHashtagIds())
                    .build();

            AiModerationResponseDto responseDto = aiModerationClient.moderateEvent(requestDto);

            BigDecimal riskScore = responseDto != null && responseDto.getRiskScore() != null
                    ? responseDto.getRiskScore()
                    : BigDecimal.ZERO;

            eventEntity.setAiRiskScore(riskScore);
            eventEntity.setAiCheckedAt(LocalDateTime.now());

            if (riskScore.compareTo(moderationThreshold) >= 0) {
                eventEntity.changeModerationStatusToPending();
            } else {
                eventEntity.changeModerationStatusToApproved();
            }

        } catch (Exception e) {
            // AI 실패 시 안전하게 관리자 검수로 보냄
            eventEntity.setAiRiskScore(null);
            eventEntity.setAiCheckedAt(LocalDateTime.now());
            eventEntity.changeModerationStatusToPending();
        }
    }

    private void saveMultiFiles(List<MultipartFile> files, EventEntity event, String fileType, String dir) {
        int sortOrder = 1;
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String original = file.getOriginalFilename();
            try {
                String savedName = s3StorageService.upload(file, dir);
                fileRepository.save(FileEntity.builder()
                        .event(event)
                        .fileType(fileType)
                        .originalFileName(original)
                        .renameFileName(savedName)
                        .sortOrder(sortOrder++)
                        .createdAt(LocalDateTime.now())
                        .build());
            } catch (Exception e) {
                throw new RuntimeException(fileType + " 다중 파일 업로드 실패", e);
            }
        }
    }

    @Override
    @Transactional
    public void deleteEvent(Long eventId, Long currentUserId) {
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("해당 행사를 찾을 수 없습니다."));

        if (!event.getHost().getUserId().equals(currentUserId)) {
            throw new RuntimeException("본인이 생성한 행사만 삭제할 수 있습니다.");
        }

        if (!"행사종료".equals(event.getEventStatus())) {
            throw new RuntimeException("진행 중이거나 예정된 행사는 삭제할 수 없습니다. 종료 후 시도해주세요.");
        }

        event.changeStatusToDeleted();
    }

    @Override
    @Transactional(readOnly = true)
    public HostEventMypageResponse myEvents(Long hostId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(50, Math.max(1, size));

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<EventEntity> p = eventRepository.findByHost_UserIdAndEventStatusNot(hostId, "DELETED", pageable);

        List<HostEventSummaryDto> items = p.getContent().stream()
                .map(HostEventSummaryDto::fromEntity)
                .toList();

        return HostEventMypageResponse.builder()
                .items(items)
                .page(safePage)
                .size(safeSize)
                .totalPages(p.getTotalPages())
                .totalElements(p.getTotalElements())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveEvent(Long hostId) {
        return eventRepository.existsByHost_UserIdAndEventStatusNotIn(
                hostId,
                List.of("행사종료", "DELETED", "REPORT_DELETED", "report_deleted", "행사삭제")
        );
    }
}