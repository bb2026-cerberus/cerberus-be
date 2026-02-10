package kr.co.cerberus.feature.todo.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class TodoDailyOverviewResponseDto {
    private Long menteeId;
    private LocalDate date;

    // 오늘 todo 목록 상세
    private TodoDailyDetailResponseDto todoDetail;

    // 오늘 타이머 요약
    private TodoTimerDailyResponseDto timerSummary;
}
