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

    // 매일 자정(0시 0분 0초)에 실행
    // 빠른 테스트를 원하시면 @Scheduled(fixedDelay = 60000) (1분마다) 로 바꿔서 해보세요!
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void updateAllEventStatuses() {
        log.info("=== 행사 상태 자동 업데이트 스케줄러 시작 ===");
        
        LocalDate today = LocalDate.now();
        List<EventEntity> events = eventRepository.findAll();
        int updatedCount = 0;

        for (EventEntity event : events) {
            // 삭제된 행사는 스킵
            if ("DELETED".equals(event.getEventStatus())) {
                continue;
            }

            String currentStatus = event.getEventStatus();
            String newStatus = calculateStatusForScheduler(event, today);

            // 상태가 변경되어야 하는 경우에만 업데이트
            if (!newStatus.equals(currentStatus)) {
                event.setEventStatus(newStatus);
                updatedCount++;
                log.debug("행사 ID {}: 상태 변경 [{} -> {}]", event.getEventId(), currentStatus, newStatus);
            }
        }

        log.info("=== 행사 상태 업데이트 완료 (총 {}건 변경) ===", updatedCount);
    }

    // EventDto에 있던 로직을 스케줄러용으로 가져와 우선순위에 맞게 재정리
    private String calculateStatusForScheduler(EventEntity event, LocalDate today) {
        
        // 6. 행사가 종료된 경우
        if (event.getEndDate() != null && today.isAfter(event.getEndDate())) {
            return "행사종료";
        }
        
        // 5. 행사 중인 경우 (시작일 <= 오늘 <= 종료일)
        if (event.getStartDate() != null && event.getEndDate() != null &&
            !today.isBefore(event.getStartDate()) && !today.isAfter(event.getEndDate())) {
            return "행사중";
        }
        
        // 4. 행사 참여 마감된 경우 (모집 마감일 < 오늘) 
        // -> 단, 이미 '행사중'이거나 '행사종료'인 경우는 위에서 걸러지므로 안심
        if (event.getEndRecruit() != null && today.isAfter(event.getEndRecruit())) {
            return "행사참여마감";
        }
        
        // 3. 행사 참여 모집 기간일 경우
        if (event.getStartRecruit() != null && event.getEndRecruit() != null &&
            !today.isBefore(event.getStartRecruit()) && !today.isAfter(event.getEndRecruit())) {
            return "행사참여모집중";
        }
        
        // 2. 부스 모집 마감된 경우
        // -> 행사 모집이 시작되기 전이면서, 부스 모집은 끝난 상태
        if (event.getBoothEndRecruit() != null && today.isAfter(event.getBoothEndRecruit())) {
            return "부스모집마감";
        }
        
        // 1. 부스 모집 기간일 경우
        if (event.getBoothStartRecruit() != null && event.getBoothEndRecruit() != null &&
            !today.isBefore(event.getBoothStartRecruit()) && !today.isAfter(event.getBoothEndRecruit())) {
            return "부스모집중";
        }

        // 7. 그 외의 경우 (아직 아무 일정도 시작 안 한 상태)
        return "행사예정";
    }
}