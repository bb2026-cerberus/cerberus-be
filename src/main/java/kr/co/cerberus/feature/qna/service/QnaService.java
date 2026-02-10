package kr.co.cerberus.feature.qna.service;

import kr.co.cerberus.feature.member.Role;
import kr.co.cerberus.feature.member.repository.MemberRepository;
import kr.co.cerberus.feature.qna.Qna;
import kr.co.cerberus.feature.qna.dto.QnaAnswerRequestDto;
import kr.co.cerberus.feature.qna.dto.QnaCreateRequestDto;
import kr.co.cerberus.feature.qna.dto.QnaResponseDto;
import kr.co.cerberus.feature.qna.dto.QnaUpdateRequestDto;
import kr.co.cerberus.feature.qna.repository.QnaRepository;
import kr.co.cerberus.feature.relation.Relation;
import kr.co.cerberus.feature.relation.repository.RelationRepository;
import kr.co.cerberus.global.error.CustomException;
import kr.co.cerberus.global.error.ErrorCode;
import kr.co.cerberus.global.jsonb.FileInfo;
import kr.co.cerberus.global.util.FileStorageService;
import kr.co.cerberus.global.util.JsonbUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QnaService {

    private final QnaRepository qnaRepository;
    private final MemberRepository memberRepository;
    private final RelationRepository relationRepository;
    private final FileStorageService fileStorageService;
	
	// Q&A 생성
	@Transactional
	public QnaResponseDto createQna(Long userId, Role userRole, QnaCreateRequestDto requestDto, List<MultipartFile> files) {
		// 멘티가 질문하는 경우
		if (userRole != Role.MENTEE) {
			throw new CustomException(ErrorCode.ACCESS_DENIED, "멘티만 Q&A를 등록할 수 있습니다.");
		}
		
		// 멘티 ID 존재 여부 확인
		memberRepository.findById(requestDto.menteeId())
				.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "유효하지 않은 멘티 ID입니다."));
		
		// 멘티-멘토 관계에서 멘토 ID 조회
		Relation relation = relationRepository.findByMenteeIdAndDeleteYn(requestDto.menteeId(), "N");
		if (relation == null) {
			throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "멘토가 배정되지 않았습니다.");
		}
		Long mentorId = relation.getMentorId();
		
		// 파일 저장 및 FileInfo 리스트 생성
		List<FileInfo> fileInfos = Collections.emptyList();
		if (files != null && !files.isEmpty()) {
			fileInfos = files.stream()
					.map(file -> new FileInfo(file.getOriginalFilename(), fileStorageService.storeFile(file, "qnas"), null))
					.toList();
		}
		
		Qna qna = Qna.builder()
				.menteeId(requestDto.menteeId())
				.mentorId(mentorId)
				.qnaDate(requestDto.date())
				.questionContent(requestDto.questionContent())
				.qnaFile(JsonbUtils.toJson(fileInfos))
				.qnaCompleteYn("N")
				.build();
		Qna savedQna = qnaRepository.save(qna);
		return mapToResponseDto(savedQna);
	}
	
	// Q&A 수정
	@Transactional
	public QnaResponseDto updateQna(QnaUpdateRequestDto requestDto, List<MultipartFile> files) {
		Qna qna = qnaRepository.findByIdAndDeleteYn(requestDto.qnaId(), "N")
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
		
		if ("Y".equals(qna.getQnaCompleteYn())) {
			throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "답변이 완료된 Q&A는 수정할 수 없습니다.");
		}
		
		// 입력값이 있으면 변경, 없으면 기존 유지
		String questionContent = (requestDto.questionContent() != null && !requestDto.questionContent().isBlank())
				? requestDto.questionContent() : qna.getQuestionContent();
		
		String qnaFile = qna.getQnaFile();
		if (files != null && !files.isEmpty()) {
			List<FileInfo> fileInfos = files.stream()
					.map(file -> new FileInfo(file.getOriginalFilename(), fileStorageService.storeFile(file, "qnas"), null))
					.toList();
			qnaFile = JsonbUtils.toJson(fileInfos);
		}
		
		qna.updateQuestion(questionContent, qnaFile);
		return mapToResponseDto(qna);
	}
	
	// Q&A 답변
	@Transactional
	public QnaResponseDto answerQna(Long userId, Role userRole, QnaAnswerRequestDto requestDto) {
		Qna qna = qnaRepository.findById(requestDto.qnaId())
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
		
		if (!Objects.equals(qna.getMentorId(), userId) || userRole != Role.MENTOR) {
			throw new CustomException(ErrorCode.ACCESS_DENIED, "해당 Q&A에 답변할 권한이 없습니다.");
		}
		
		if ("Y".equals(qna.getQnaCompleteYn())) {
			throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "이미 답변되었거나 종료된 Q&A입니다.");
		}
		
		qna.updateAnswer(requestDto.answerContent());
		return mapToResponseDto(qna);
	}
	
	// Q&A 삭제
	@Transactional
	public void deleteQna(Long qnaId) {
		Qna qna = qnaRepository.findByIdAndDeleteYn(qnaId, "N")
				.orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
		qna.delete();
	}

    public QnaResponseDto getQnaDetail(Long qnaId) {
        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        return mapToResponseDto(qna);
    }

    // 멘티 ID와 날짜로 Q&A 단건 조회
    public List<QnaResponseDto> getQnaByMenteeIdAndDate(Long menteeId, LocalDate date) {
        List<Qna> qna = qnaRepository.findAllByMenteeIdAndQnaDateAndDeleteYnOrderByCreateDatetime(menteeId, date, "N");
        return qna != null ? mapToResponseDto(qna) : null;
    }

	public List<QnaResponseDto> getQnaByMentorIdAndDate(Long mentorId, LocalDate date) {
		List<Qna> qna = qnaRepository.findByMentorIdAndQnaDateAndDeleteYnOrderByCreateDatetime(mentorId, date, "N");
		return qna != null ? mapToResponseDto(qna) : null;
	}

	private QnaResponseDto mapToResponseDto(Qna qna) {
		return new QnaResponseDto(
						qna.getId(),
						qna.getMenteeId(),
						qna.getMentorId(),
						qna.getQnaDate(),
						qna.getQuestionContent(),
						qna.getAnswerContent(),
						Optional.ofNullable(JsonbUtils.fromJson(qna.getQnaFile(), new TypeReference<List<FileInfo>>() {})).orElse(List.of()),
						qna.getCreateDatetime(),
						qna.getUpdateDatetime()
		);
	}

	private List<QnaResponseDto> mapToResponseDto(List<Qna> qnas) {
		return qnas.stream()
				.map(qna -> new QnaResponseDto(
						qna.getId(),
						qna.getMenteeId(),
						qna.getMentorId(),
						qna.getQnaDate(),
						qna.getQuestionContent(),
						qna.getAnswerContent(),
						Optional.ofNullable(JsonbUtils.fromJson(qna.getQnaFile(), new TypeReference<List<FileInfo>>() {})).orElse(List.of()),
						qna.getCreateDatetime(),
						qna.getUpdateDatetime())
				)
				.toList();
	}
}