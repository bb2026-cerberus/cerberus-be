package kr.co.cerberus.feature.assignment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.cerberus.feature.assignment.dto.MentorAssignmentCreateRequestDto;
import kr.co.cerberus.feature.assignment.dto.MentorSolutionResponseDto;
import kr.co.cerberus.feature.assignment.service.AssignmentService;
import kr.co.cerberus.global.common.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Mentor Assignment", description = "멘토용 과제 관리 API")
@RestController
@RequestMapping("/api/mentors/assignments")
@RequiredArgsConstructor
public class MentorAssignmentController {

    private final AssignmentService assignmentService;

    @Operation(summary = "멘티의 솔루션 목록 조회", description = "특정 할일(todoId)을 통해 멘티를 식별하고, 해당 멘티의 모든 약점 솔루션과 포함된 파일명을 조회합니다.")
    @GetMapping("/solutions")
    public ResponseEntity<CommonResponse<List<MentorSolutionResponseDto>>> getMenteeSolutions(
            @Parameter(description = "할일 ID", example = "1") @RequestParam Long todoId) {
        List<MentorSolutionResponseDto> solutions = assignmentService.findSolutionsByTodoId(todoId);
        return ResponseEntity.ok(CommonResponse.of(solutions));
    }

    @Operation(summary = "과제 생성 및 임시저장", description = "멘토가 멘티에게 과제를 배정합니다. 다수의 날짜를 지정할 수 있으며, 학습지 파일을 업로드할 수 있습니다. isDraft 필드로 임시저장 여부를 결정합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<Void>> createAssignment(
            @RequestPart("request") MentorAssignmentCreateRequestDto request,
            @RequestPart(value = "workbooks", required = false) List<MultipartFile> workbooks) {
        
        assignmentService.createAssignmentByMentor(request, workbooks);
        return ResponseEntity.ok(CommonResponse.of(null));
    }
}
