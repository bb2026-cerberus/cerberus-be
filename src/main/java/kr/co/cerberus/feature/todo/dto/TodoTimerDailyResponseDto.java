package kr.co.cerberus.feature.todo.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class TodoTimerDailyResponseDto {
    private Long menteeId;
    private LocalDate date;

    // 하루 총 공부시간(분)
    private long totalMinutes;

    // 세션 평균시간(분) = totalMinutes / 세션개수
    private long averageMinutes;

    // 당일 타임블록(할일별)
    private List<TodoTimerItem> items;

    @Getter
    @Builder
    public static class TodoTimerItem {
        private Long todoId;
        private String title;
        private String subject;
        private String note;

        // 해당 Todo의 총 시간(분)
        private long totalMinutes;

        // 세션들 (startAt/endAt은 ISO 문자열로 내려줌)
        private List<TimerSession> sessions;
    }

    @Getter
    @Builder
    public static class TimerSession {
        private String startAt; // "2026-02-09T10:00:00"
        private String endAt;   // "2026-02-09T10:25:10"
        private long minutes;   // 세션 길이(분)
    }
}
