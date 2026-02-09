package kr.co.cerberus.feature.todo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter

public class TodoTimerSessionCreateRequestDto {

    @Schema(example = "2026-02-09T10:00:00")
    private LocalDateTime startAt;

    @Schema(example = "2026-02-09T10:25:10")
    private LocalDateTime endAt;
}
