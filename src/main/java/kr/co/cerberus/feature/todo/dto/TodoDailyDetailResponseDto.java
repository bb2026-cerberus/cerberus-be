package kr.co.cerberus.feature.todo.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class TodoDailyDetailResponseDto {
    private Long menteeId;
    private LocalDate date;
    private int totalCount;
    private List<TodoItem> items;

    @Getter
    @Builder
    public static class TodoItem {
        private Long todoId;

        // 핵심 정보
        private String menteeId;
        private String subject;     // todoSubjects
        private String note;
        private String name;
        private String title;

        // 상태/구분
        private String assignYn;    // 'Y'/'N'
        private String type;        // 변환값: 할일/과제
        private String completeYn;  // 'Y'/'N'

        // 날짜
        private LocalDate todoDate;

    }
}
