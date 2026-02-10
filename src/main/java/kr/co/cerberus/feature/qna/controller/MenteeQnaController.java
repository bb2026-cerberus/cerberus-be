package kr.co.cerberus.feature.qna.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.cerberus.feature.member.Role;
import kr.co.cerberus.feature.qna.dto.QnaCreateRequestDto;
import kr.co.cerberus.feature.qna.dto.QnaResponseDto;
import kr.co.cerberus.feature.qna.dto.QnaUpdateRequestDto;
import kr.co.cerberus.feature.qna.service.QnaService;
import kr.co.cerberus.global.common.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Mentee Q&A Management", description = "멘티의 Q&A 관리 API")
@RestController
@RequestMapping("/api/mentees/qnas")
@RequiredArgsConstructor
public class MenteeQnaController {

	private final QnaService qnaService;

	@Operation(summary = "Q&A 조회", description = "특정 멘티의 특정 날짜 Q&A를 조회합니다.")
	@GetMapping
	public ResponseEntity<CommonResponse<List<QnaResponseDto>>> getQna(
			@Parameter(description = "멘티 ID", required = true, example = "2") @RequestParam(value = "menteeId") Long menteeId,
			@Parameter(description = "조회 날짜 (YYYY-MM-DD)", required = true, example = "2026-02-09") @RequestParam(value = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

		List<QnaResponseDto> response = qnaService.getQnaByMenteeIdAndDate(menteeId, date);
		return ResponseEntity.ok(CommonResponse.of(response));
	}

	@Operation(summary = "Q&A 질문 등록", description = "멘티가 멘토에게 Q&A 질문을 등록합니다. 멘토는 멘티-멘토 관계에서 자동 조회됩니다. 다중 파일 업로드를 지원합니다.")
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse<QnaResponseDto>> createQna(
			@Parameter(description = "멘티 ID", required = true, example = "2") @RequestParam(value = "menteeId") Long menteeId,
			@Parameter(description = "질문 내용", required = true, example = "이 문제 풀이 방법을 알려주세요.") @RequestParam(value = "questionContent") String questionContent,
			@Parameter(description = "질문 날짜 (YYYY-MM-DD)", required = true, example = "2026-02-09") @RequestParam(value = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@Parameter(description = "첨부 파일 (여러 개 가능)") @RequestPart(value = "files", required = false) List<MultipartFile> files) {

		QnaCreateRequestDto request = new QnaCreateRequestDto(menteeId, questionContent, date);
		QnaResponseDto response = qnaService.createQna(menteeId, Role.MENTEE, request, files);
		return ResponseEntity.ok(CommonResponse.of(response));
	}

	@Operation(summary = "Q&A 수정", description = "멘티가 등록한 Q&A 질문을 수정합니다. 답변이 완료된 Q&A는 수정할 수 없습니다. 다중 파일 업로드를 지원합니다.")
	@PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<CommonResponse<QnaResponseDto>> updateQna(
			@Parameter(description = "Q&A ID", required = true, example = "1") @RequestParam(value = "qnaId") Long qnaId,
			@Parameter(description = "질문 내용", example = "수정된 질문 내용입니다.") @RequestParam(value = "questionContent", required = false) String questionContent,
			@Parameter(description = "첨부 파일 (여러 개 가능)") @RequestPart(value = "files", required = false) List<MultipartFile> files) {

		QnaUpdateRequestDto request = new QnaUpdateRequestDto(qnaId, questionContent);
		QnaResponseDto response = qnaService.updateQna(request, files);
		return ResponseEntity.ok(CommonResponse.of(response));
	}

	@Operation(summary = "Q&A 삭제", description = "멘티가 등록한 Q&A를 삭제합니다.")
	@DeleteMapping
	public ResponseEntity<CommonResponse<Void>> deleteQna(
			@Parameter(description = "Q&A ID", required = true, example = "1") @RequestParam(value = "qnaId") Long qnaId) {

		qnaService.deleteQna(qnaId);
		return ResponseEntity.ok(CommonResponse.of(null));
	}
}
