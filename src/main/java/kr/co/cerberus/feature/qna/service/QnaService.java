package kr.co.cerberus.feature.qna.service;

import kr.co.cerberus.feature.member.Member;
import kr.co.cerberus.feature.member.Role;
import kr.co.cerberus.feature.member.repository.MemberRepository;
import kr.co.cerberus.feature.qna.Qna;
import kr.co.cerberus.feature.qna.dto.QnaAnswerRequestDto;
import kr.co.cerberus.feature.qna.dto.QnaCreateRequestDto;
import kr.co.cerberus.feature.qna.dto.QnaResponseDto;
import kr.co.cerberus.feature.qna.dto.QnaUpdateRequestDto;
import kr.co.cerberus.feature.qna.repository.QnaRepository;
import kr.co.cerberus.feature.relation.repository.RelationRepository;
import kr.co.cerberus.global.error.CustomException;
import kr.co.cerberus.global.error.ErrorCode;
import kr.co.cerberus.global.util.JsonbUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QnaService {

    private final QnaRepository qnaRepository;
    private final MemberRepository memberRepository;
    private final RelationRepository relationRepository;

    @Transactional
    public QnaResponseDto createQna(Long userId, Role userRole, QnaCreateRequestDto requestDto) {
        if (userRole == Role.MENTEE) {
            if (!Objects.equals(requestDto.menteeId(), userId)) {
                throw new CustomException(ErrorCode.ACCESS_DENIED, "본인의 Q&A만 등록할 수 있습니다.");
            }
            validateMentorMenteeRelation(requestDto.mentorId(), requestDto.menteeId());
        } else {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "멘티만 Q&A를 등록할 수 있습니다.");
        }

        memberRepository.findById(requestDto.menteeId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "유효하지 않은 멘티 ID입니다."));
        memberRepository.findById(requestDto.mentorId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "유효하지 않은 멘토 ID입니다."));


        Qna qna = Qna.builder()
                .menteeId(requestDto.menteeId())
                .mentorId(requestDto.mentorId())
                .title(requestDto.title())
                .questionContent(requestDto.questionContent())
                .qnaFile(JsonbUtils.toJson(requestDto.qnaFiles()))
                .qnaCompleteYn("N")
                .build();
        Qna savedQna = qnaRepository.save(qna);
        return mapToResponseDto(savedQna);
    }

    @Transactional
    public QnaResponseDto updateQna(Long userId, Role userRole, QnaUpdateRequestDto requestDto) {
        Qna qna = qnaRepository.findById(requestDto.qnaId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!Objects.equals(qna.getMenteeId(), userId) || userRole != Role.MENTEE) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "본인의 Q&A만 수정할 수 있습니다.");
        }

        if ("Y".equals(qna.getQnaCompleteYn())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "답변이 완료된 Q&A는 수정할 수 없습니다.");
        }

        qna.updateQuestion(
                requestDto.title(),
                requestDto.questionContent(),
                JsonbUtils.toJson(requestDto.qnaFiles())
        );
        return mapToResponseDto(qna);
    }

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

    @Transactional
    public void deleteQna(Long userId, Role userRole, Long qnaId) {
        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        if (userRole == Role.MENTEE && !Objects.equals(qna.getMenteeId(), userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "본인의 Q&A만 삭제할 수 있습니다.");
        } else if (userRole == Role.MENTOR && !Objects.equals(qna.getMentorId(), userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "관리하는 멘티의 Q&A만 삭제할 수 있습니다.");
        }

        qna.delete();
    }

    public QnaResponseDto getQnaDetail(Long qnaId) {
        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        return mapToResponseDto(qna);
    }

    public List<QnaResponseDto> getQnasByMentorId(Long mentorId, Role userRole) {
        if (userRole != Role.MENTOR) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "멘토만 Q&A 목록을 조회할 수 있습니다.");
        }
        List<Qna> qnas = qnaRepository.findByMentorId(mentorId);
        return qnas.stream()
                .filter(q -> !q.isDeleted())
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public List<QnaResponseDto> getQnasByMenteeId(Long menteeId, Role userRole) {
        if (userRole != Role.MENTEE) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "멘티만 본인의 Q&A 목록을 조회할 수 있습니다.");
        }
        List<Qna> qnas = qnaRepository.findByMenteeId(menteeId);
        return qnas.stream()
                .filter(q -> !q.isDeleted())
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private QnaResponseDto mapToResponseDto(Qna qna) {
        return new QnaResponseDto(
                qna.getId(),
                qna.getMenteeId(),
                qna.getMentorId(),
                qna.getTitle(),
                qna.getQuestionContent(),
                qna.getAnswerContent(),
                Optional.ofNullable(JsonbUtils.fromJson(qna.getQnaFile(), new TypeReference<List<kr.co.cerberus.global.jsonb.FileInfo>>() {})).orElse(List.of()),
                qna.getCreateDatetime(),
                qna.getUpdateDatetime()
        );
    }

    private void validateMentorMenteeRelation(Long mentorId, Long menteeId) {
        if (!memberRepository.findById(mentorId).map(m -> m.getRole() == Role.MENTOR).orElse(false)) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER, "유효하지 않은 멘토 ID입니다.");
        }
        if (!memberRepository.findById(menteeId).map(m -> m.getRole() == Role.MENTEE).orElse(false)) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER, "유효하지 않은 멘티 ID입니다.");
        }
        if (!relationRepository.findByMentorId(mentorId).stream()
                .anyMatch(r -> r.getMenteeId().equals(menteeId))) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER, "멘토와 멘티 간의 관계가 존재하지 않습니다.");
        }
    }
}