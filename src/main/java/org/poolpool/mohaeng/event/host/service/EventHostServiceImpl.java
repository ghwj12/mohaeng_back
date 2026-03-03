package org.poolpool.mohaeng.event.host.service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.poolpool.mohaeng.common.config.UploadProperties;
import org.poolpool.mohaeng.common.util.FileNameChange;
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
import org.poolpool.mohaeng.user.entity.UserEntity;
import org.poolpool.mohaeng.user.repository.UserRepository;
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

    private final EventRepository       eventRepository;
    private final HostBoothRepository   hostBoothRepository;
    private final HostFacilityRepository hostFacilityRepository;
    private final FileRepository        fileRepository;
    private final EventCategoryRepository eventCategoryRepository;
    private final EventRegionRepository   eventRegionRepository;
    private final UploadProperties       uploadProperties;
    private final UserRepository         userRepository;

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

        // ✅ 문제 5: hasBooth / hasFacility 는 실제 데이터(리스트 비어있는지 여부) 기준으로 override
        // 사용자가 체크박스를 체크했다가 해제해도 폼 상태에 데이터가 남아있을 수 있으므로
        // 실제 전달된 부스/시설 목록이 비어 있으면 false로 강제 지정
        boolean actualHasBooth = createDto.getBooths() != null && !createDto.getBooths().isEmpty();
        boolean actualHasFacility = createDto.getFacilities() != null && !createDto.getFacilities().isEmpty();
        eventEntity.setHasBooth(actualHasBooth);
        eventEntity.setHasFacility(actualHasFacility);

        // ✅ 문제 5: 부스가 없으면 부스 모집 기간도 null로 초기화 (DB에 날짜가 남아 상태가 바뀌는 것 방지)
        if (!actualHasBooth) {
            eventEntity.setBoothStartRecruit(null);
            eventEntity.setBoothEndRecruit(null);
        }

        // 썸네일 저장
        if (thumbnail != null && !thumbnail.isEmpty()) {
            String original = thumbnail.getOriginalFilename();
            String rename   = FileNameChange.change(original, FileNameChange.RenameStrategy.DATETIME_UUID);
            File saveDir    = uploadProperties.boardDir().toFile();
            if (!saveDir.exists()) saveDir.mkdirs();
            try {
                thumbnail.transferTo(new File(saveDir, rename));
                eventEntity.setThumbnail(rename);
            } catch (Exception e) {
                throw new RuntimeException("썸네일 업로드 실패", e);
            }
        }

        // 행사 저장
        EventEntity savedEvent = eventRepository.save(eventEntity);
        Long eventId = savedEvent.getEventId();

        // 상세 이미지 저장
        if (detailFiles != null && !detailFiles.isEmpty()) {
            File saveDir = uploadProperties.boardDir().toFile();
            if (!saveDir.exists()) saveDir.mkdirs();
            saveMultiFiles(detailFiles, saveDir, savedEvent, "EVENT");
        }

        // 부스 첨부파일 저장 (✅ 문제 5: actualHasBooth가 true일 때만)
        if (actualHasBooth && boothFiles != null && !boothFiles.isEmpty()) {
            File saveDir = uploadProperties.hboothDir().toFile();
            if (!saveDir.exists()) saveDir.mkdirs();
            saveMultiFiles(boothFiles, saveDir, savedEvent, "HBOOTH");
        }

        // ✅ 문제 5: 부스 저장 (actualHasBooth가 true일 때만)
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

        // ✅ 문제 5: 부대시설 저장 (actualHasFacility가 true일 때만)
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

    private void saveMultiFiles(List<MultipartFile> files, File saveDir, EventEntity event, String fileType) {
        int sortOrder = 1;
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String original = file.getOriginalFilename();
            String rename   = FileNameChange.change(original, FileNameChange.RenameStrategy.DATETIME_UUID);
            try {
                file.transferTo(new File(saveDir, rename));
                fileRepository.save(FileEntity.builder()
                        .event(event)
                        .fileType(fileType)
                        .originalFileName(original)
                        .renameFileName(rename)
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
                hostId, List.of("행사종료", "DELETED"));
    }
}
