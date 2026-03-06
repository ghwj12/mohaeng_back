package org.poolpool.mohaeng.event.list.scheduler;
import java.time.LocalDate;
import java.util.List;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.list.repository.EventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Component
@RequiredArgsConstructor
public class EventStatusScheduler {
    private final EventRepository eventRepository;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void updateAllEventStatuses() {
        log.info("=== 행사 상태 자동 업데이트 스케줄러 시작 ===");
        
        LocalDate today = LocalDate.now();
        List<EventEntity> events = eventRepository.findAllForScheduler();
        int updatedCount = 0;
        for (EventEntity event : events) {
            // 수동 삭제 상태(DELETED, report_deleted 등)는 스킵 — 대소문자 무관
            if (isPreservedStatus(event.getEventStatus())) {
                continue;
            }
            String currentStatus = event.getEventStatus();
            String newStatus = calculateStatusForScheduler(event, today);
            if (!newStatus.equals(currentStatus)) {
                event.setEventStatus(newStatus);
                updatedCount++;
                log.debug("행사 ID {}: 상태 변경 [{} -> {}]", event.getEventId(), currentStatus, newStatus);
            }
        }
        log.info("=== 행사 상태 업데이트 완료 (총 {}건 변경) ===", updatedCount);
    }

    /**
     * 스케줄러가 덮어쓰면 안 되는 상태 여부 확인
     * - 대소문자 무관하게 "deleted" 포함 여부로 판단
     * - "행사삭제"도 보존
     */
    private boolean isPreservedStatus(String status) {
        if (status == null) return false;
        String lower = status.toLowerCase();
        return lower.equals("deleted") || lower.equals("report_deleted") || lower.contains("deleted") || "행사삭제".equals(status);
    }

    private String calculateStatusForScheduler(EventEntity event, LocalDate today) {
        if (event.getEndDate() != null && today.isAfter(event.getEndDate())) {
            return "행사종료";
        }
        if (event.getStartDate() != null && event.getEndDate() != null &&
            !today.isBefore(event.getStartDate()) && !today.isAfter(event.getEndDate())) {
            return "행사중";
        }
        if (event.getEndRecruit() != null && today.isAfter(event.getEndRecruit())) {
            return "행사참여마감";
        }
        if (event.getStartRecruit() != null && event.getEndRecruit() != null &&
            !today.isBefore(event.getStartRecruit()) && !today.isAfter(event.getEndRecruit())) {
            return "행사참여모집중";
        }
        if (event.getBoothEndRecruit() != null && today.isAfter(event.getBoothEndRecruit())) {
            return "부스모집마감";
        }
        if (event.getBoothStartRecruit() != null && event.getBoothEndRecruit() != null &&
            !today.isBefore(event.getBoothStartRecruit()) && !today.isAfter(event.getBoothEndRecruit())) {
            return "부스모집중";
        }
        return "행사예정";
    }
}
