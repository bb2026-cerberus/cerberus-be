package kr.co.cerberus.feature.mentor.service;

import kr.co.cerberus.feature.assignment.domain.AssignmentStatus;
import kr.co.cerberus.feature.feedback.domain.FeedbackStatus;
import kr.co.cerberus.feature.member.repository.MemberRepository;
import kr.co.cerberus.feature.mentor.dto.DraftCountResponseDto;
import kr.co.cerberus.feature.mentor.dto.MenteeProgressResponseDto;
import kr.co.cerberus.feature.mentor.dto.MentorAssignmentSummaryDto;
import kr.co.cerberus.feature.mentor.dto.MentorFeedbackSummaryDto;
import kr.co.cerberus.feature.mentor.dto.MentorHomeResponseDto;
import kr.co.cerberus.feature.mentor.dto.MentorQnaSummaryDto;
import kr.co.cerberus.feature.mentor.dto.SubjectProgressDto;
import kr.co.cerberus.feature.feedback.repository.FeedbackRepository;
import kr.co.cerberus.feature.qna.repository.QnaRepository;
import kr.co.cerberus.feature.relation.Relation;
import kr.co.cerberus.feature.relation.repository.RelationRepository;
import kr.co.cerberus.feature.todo.Todo;
import kr.co.cerberus.feature.todo.repository.TodoRepository;
import kr.co.cerberus.global.error.CustomException;
import kr.co.cerberus.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MentorService {

    private final TodoRepository todoRepository;
    private final FeedbackRepository feedbackRepository;
    private final QnaRepository qnaRepository;
    private final RelationRepository relationRepository; // RelationRepository 주입
    private final MemberRepository memberRepository; // MemberRepository 주입




    // 멘토 홈 화면 데이터 조회
    public MentorHomeResponseDto getMentorHomeData(Long mentorId, LocalDate date) {
        List<Long> menteeIds = getMenteeIdsByMentorId(mentorId);
        if (menteeIds.isEmpty()) {
            return new MentorHomeResponseDto(List.of(), List.of(), List.of());
        }
        Map<Long, String> menteeNames = getMenteeNames(menteeIds);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        // 1. 과제 요약
        List<MentorAssignmentSummaryDto> assignmentSummaries = todoRepository.findByMenteeIdInAndStatusInAndTodoDateBetweenAndActivateYn(
                        menteeIds,
                        Arrays.asList(AssignmentStatus.ASSIGNED, AssignmentStatus.IN_PROGRESS),
                        date,
                        date, // 해당 날짜의 과제만 조회
                        "Y")
                .stream()
                .map(todo -> new MentorAssignmentSummaryDto(
                        todo.getId(),
                        todo.getMenteeId(),
                        menteeNames.getOrDefault(todo.getMenteeId(), "알 수 없는 멘티"),
                        todo.getTodoName(),
                        todo.getTodoDate(),
                        todo.getStatus()
                ))
                .collect(Collectors.toList());

        // 2. 피드백 요약
        // TODO: feedbackRepository.findByMentorIdAndStatusInAndCreateDatetimeBetweenAndActivateYn 메서드는 feedDate가 아닌 createDatetime으로 조회하는 문제.
        // 요구사항이 '날짜 기반 조회: 피드백 리스트' 이므로 feedDate를 기준으로 조회해야 함.
        // 현재 피드백 엔티티에 feedDate가 LocalDate 타입으로 있어서 BETWEEN 쿼리가 힘듦.
        // 피드백 생성 일자(createDatetime) 또는 feedDate가 LocalDate가 아닌 LocalDateTime으로 변경 필요.
        // 일단은 createDatetime 기준으로 조회
        List<MentorFeedbackSummaryDto> feedbackSummaries = feedbackRepository.findByMentorIdAndStatusInAndCreateDatetimeBetweenAndActivateYn(
                        mentorId,
                        Arrays.asList(FeedbackStatus.PENDING), // 피드백 대기중인 것만 조회
                        startOfDay,
                        endOfDay,
                        "Y")
                .stream()
                .map(feedback -> new MentorFeedbackSummaryDto(
                        feedback.getId(),
                        feedback.getMenteeId(),
                        menteeNames.getOrDefault(feedback.getMenteeId(), "알 수 없는 멘티"),
                        feedback.getTodoId(),
                        feedback.getFeedDate(), // feedDate는 멘티 API 호환을 위해 유지
                        feedback.getStatus()
                ))
                .collect(Collectors.toList());

        // 3. Q&A 요약
        List<MentorQnaSummaryDto> qnaSummaries = qnaRepository.findByMentorIdAndCreateDatetimeBetweenAndActivateYn(
                        mentorId,
                        startOfDay,
                        endOfDay,
                        "Y")
                .stream()
                .map(qna -> new MentorQnaSummaryDto(
                        qna.getId(),
                        qna.getMenteeId(),
                        menteeNames.getOrDefault(qna.getMenteeId(), "알 수 없는 멘티"),
                        qna.getTitle(),
                        qna.getStatus(),
                        qna.getCreateDatetime()
                ))
                .collect(Collectors.toList());

        return new MentorHomeResponseDto(assignmentSummaries, feedbackSummaries, qnaSummaries);
    }

    // 임시저장 개수 조회 (Redis 캐싱 제거)
    public DraftCountResponseDto getDraftCounts(Long mentorId) {
        // Redis 캐싱을 사용하지 않으므로 캐시 관련 로직 제거
        log.debug("Draft count calculated directly from DB for mentorId: {}", mentorId);
        List<Long> menteeIds = getMenteeIdsByMentorId(mentorId);

        long assignmentDraftCount = todoRepository.countByMenteeIdInAndStatusAndActivateYn(
                menteeIds, AssignmentStatus.DRAFT, "Y");
        long feedbackDraftCount = feedbackRepository.countByMentorIdAndStatusAndActivateYn(
                mentorId, FeedbackStatus.DRAFT, "Y");

        DraftCountResponseDto response = new DraftCountResponseDto(assignmentDraftCount, feedbackDraftCount);

        return response;
    }

    // 멘티별 진행률 통계 계산
    public MenteeProgressResponseDto getMenteeProgress(Long mentorId, Long menteeId) {
        // 멘토가 해당 멘티를 관리하는지 확인
        if (!isMentorManagingMentee(mentorId, menteeId)) {
            throw new CustomException(ErrorCode.RELATION_ACCESS_DENIED);
        }
        
        Map<Long, String> menteeNames = getMenteeNames(Collections.singletonList(menteeId));

        List<Todo> todos = todoRepository.findByMenteeIdAndActivateYn(menteeId, "Y");
        if (todos.isEmpty()) {
            return new MenteeProgressResponseDto(menteeId, menteeNames.getOrDefault(menteeId, "알 수 없는 멘티"), 0.0, List.of());
        }

        long totalTodos = todos.size();
        long completedTodos = todos.stream()
                .filter(todo -> todo.getStatus() == AssignmentStatus.COMPLETED)
                .count();

        double overallProgress = (totalTodos > 0) ? (double) completedTodos / totalTodos * 100 : 0.0;

        // 과목별 진행률
        Set<String> subjects = todos.stream()
                .map(Todo::getTodoSubjects)
                .filter(Objects::nonNull) // null 값 필터링
                .collect(Collectors.toSet());

        List<SubjectProgressDto> subjectProgressList = new ArrayList<>();
        for (String subject : subjects) {
            // 해당 과목의 모든 할 일을 조회하여 총 개수를 얻음
            List<Todo> totalSubjectTodosList = todoRepository.findByMenteeIdAndTodoSubjectsAndActivateYn(
                    menteeId, subject, "Y"
            );
            long totalSubjectTodos = totalSubjectTodosList.size();

            // 해당 과목 중 완료된 할 일만 필터링하여 개수를 얻음
            long completedSubjectTodos = totalSubjectTodosList.stream()
                    .filter(todo -> todo.getStatus() == AssignmentStatus.COMPLETED)
                    .count();
            double subjectProgress = (totalSubjectTodos > 0) ? (double) completedSubjectTodos / totalSubjectTodos * 100 : 0.0;
            subjectProgressList.add(new SubjectProgressDto(subject, subjectProgress));
        }

        return new MenteeProgressResponseDto(menteeId, menteeNames.getOrDefault(menteeId, "알 수 없는 멘티"), overallProgress, subjectProgressList);
    }


    // 멘토가 관리하는 멘티 ID 목록을 가져오는 실제 로직
    private List<Long> getMenteeIdsByMentorId(Long mentorId) {
        return relationRepository.findByMentorIdAndActivateYn(mentorId, "Y")
                .stream()
                .map(Relation::getMenteeId)
                .collect(Collectors.toList());
    }

    // 멘티 ID를 통해 멘티 이름을 조회하는 헬퍼 메서드
    private Map<Long, String> getMenteeNames(List<Long> menteeIds) {
        return memberRepository.findAllById(menteeIds)
                .stream()
                .collect(Collectors.toMap(m -> m.getId(), m -> m.getName(), (oldValue, newValue) -> oldValue)); // 중복 키 발생 시 기존 값 유지
    }

    // 멘토가 특정 멘티를 관리하는지 확인하는 헬퍼 메서드
    private boolean isMentorManagingMentee(Long mentorId, Long menteeId) {
        return relationRepository.findByMentorIdAndActivateYn(mentorId, "Y")
                .stream()
                .anyMatch(relation -> relation.getMenteeId().equals(menteeId));
    }
}
