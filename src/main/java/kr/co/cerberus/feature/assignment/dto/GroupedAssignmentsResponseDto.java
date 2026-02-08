package kr.co.cerberus.feature.assignment.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
public class GroupedAssignmentsResponseDto {
    private LocalDate date;
    private List<AssignmentListResponseDto> assignments;
}
