package kr.co.cerberus.feature.todo.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class GroupedTodosResponseDto {
    private LocalDate date;
    private List<TodoListResponseDto> todos;
}
