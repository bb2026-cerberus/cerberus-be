package kr.co.cerberus.feature.notification.scheduler;
import kr.co.cerberus.feature.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {
    private final NotificationService notificationService;

    /**
     * 매일 22시 미완료 Todo 리마인드
     */
    @Scheduled(cron = "0 0 22 * * *")
    public void remindIncompleteTodos() {
        log.info("[Scheduler] 미완료 Todo 리마인드 시작");
        notificationService.notifyIncompleteTodos(LocalDate.now());
    }
}
