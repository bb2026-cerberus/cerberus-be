package kr.co.cerberus.feature.mentee.service;

import kr.co.cerberus.feature.feedback.Feedback;
import kr.co.cerberus.feature.feedback.repository.FeedbackRepository;
import kr.co.cerberus.feature.member.Member;
import kr.co.cerberus.feature.member.repository.MemberRepository;
import kr.co.cerberus.feature.mentee.dto.MenteeMypageResponseDto;
import kr.co.cerberus.feature.mentor.dto.*;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenteeService {
// ... (existing fields)

    private final MemberRepository memberRepository;
    private final TodoRepository  todoRepository;
    private final FeedbackRepository feedbackRepository;
    private final QnaRepository qnaRepository;
    private final RelationRepository relationRepository;

    public MenteeMypageResponseDto getMenteeMypage(Long menteeId) {
        Member mentee = memberRepository.findById(menteeId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        // 이번 주 과제/할 일 전체 조회
        List<Todo> weeklyTodos = todoRepository.findByMenteeIdAndTodoDateBetweenAndDeleteYn(menteeId, monday, sunday, "N");

        // 멘토 과제 달성률
        List<Todo> mentorAssignments = weeklyTodos.stream().filter(t -> "Y".equals(t.getTodoAssignYn())).toList();
        double mentorRate = calculateRate(mentorAssignments);

        // 일반 할 일 달성률
        List<Todo> generalTodos = weeklyTodos.stream().filter(t -> "N".equals(t.getTodoAssignYn())).toList();
        double generalRate = calculateRate(generalTodos);

        // 이번 주 과목별 달성률
        Map<String, List<Todo>> subjectMap = weeklyTodos.stream()
                .filter(t -> t.getTodoSubjects() != null)
                .collect(Collectors.groupingBy(Todo::getTodoSubjects));

        List<SubjectProgressDto> subjectProgress = subjectMap.entrySet().stream()
                .map(entry -> new SubjectProgressDto(entry.getKey(), calculateRate(entry.getValue())))
                .toList();

        return new MenteeMypageResponseDto(
                menteeId,
                mentee.getMemName(),
                new MenteeMypageResponseDto.WeeklyAchievement(mentorRate, generalRate),
                subjectProgress
        );
    }

    private double calculateRate(List<Todo> todos) {
        if (todos.isEmpty()) return 0.0;
        long completed = todos.stream().filter(t -> "Y".equals(t.getTodoCompleteYn())).count();
        return (double) completed / todos.size() * 100.0;
    }

    // 멘토 홈 화면 데이터 조회
// ...
    public MentorHomeResponseDto getMentorHomeData(Long mentorId, LocalDate date) {
        List<Long> menteeIds = getMenteeIdsByMentorId(mentorId);
        if (menteeIds.isEmpty()) {
            return new MentorHomeResponseDto(List.of(), List.of(), List.of(), List.of());
        }
        Map<Long, String> menteeNames = getMenteeNames(menteeIds);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        // 1. 과제 요약 - 할당된 과제(assignYn='Y')만 조회
        List<Todo> allTodos = todoRepository.findByMenteeIdInAndTodoAssignYnAndTodoDateBetween(
                menteeIds, "Y", date, date);

        List<MentorAssignmentSummaryDto> assignmentSummaries = allTodos.stream()
                .map(todo -> new MentorAssignmentSummaryDto(
                        todo.getId(),
                        todo.getMenteeId(),
                        menteeNames.getOrDefault(todo.getMenteeId(), "알 수 없는 멘티"),
                        todo.getTodoName(),
                        todo.getTodoDate(),
                        determineStatus(todo)
                ))
                .collect(Collectors.toList());

        // 2. 피드백 요약 - 완료되지 않은(feedCompleteYn='N') 피드백 조회
        List<MentorFeedbackSummaryDto> feedbackSummaries = feedbackRepository.findByMentorIdAndFeedCompleteYnAndCreateDatetimeBetween(
                        mentorId,
                        "N",
                        startOfDay,
                        endOfDay)
                .stream()
                .map(feedback -> new MentorFeedbackSummaryDto(
                        feedback.getId(),
                        feedback.getMenteeId(),
                        menteeNames.getOrDefault(feedback.getMenteeId(), "알 수 없는 멘티"),
                        feedback.getTodoId(),
                        feedback.getFeedDate(),
                        determineFeedbackStatus(feedback)
                ))
                .collect(Collectors.toList());

        // 3. Q&A 요약
        List<MentorQnaSummaryDto> qnaSummaries = qnaRepository.findByMentorIdAndCreateDatetimeBetween(
                        mentorId,
                        startOfDay,
                        endOfDay)
                .stream()
                .map(qna -> new MentorQnaSummaryDto(
                        qna.getId(),
                        qna.getMenteeId(),
                        menteeNames.getOrDefault(qna.getMenteeId(), "알 수 없는 멘티"),
                        "Y".equals(qna.getQnaCompleteYn()) ? "ANSWERED" : "PENDING",
                        qna.getCreateDatetime()
                ))
                .collect(Collectors.toList());

        // 4. 멘티 관리 요약 (Mentee Management) - 오늘 할당된 과제 기준 통계
        List<MenteeManagementDto> menteeManagementList = menteeIds.stream()
                .map(menteeId -> {
                    // 해당 멘티의 오늘 할당된 과제 필터링
                    List<Todo> menteeTodos = allTodos.stream()
                            .filter(todo -> todo.getMenteeId().equals(menteeId))
                            .toList();

                    int totalCount = menteeTodos.size();
                    int completedCount = (int) menteeTodos.stream()
                            .filter(todo -> "Y".equals(todo.getTodoCompleteYn()))
                            .count();
                    List<String> unsubmittedTitles = menteeTodos.stream()
                            .filter(todo -> !"Y".equals(todo.getTodoCompleteYn()))
                            .map(Todo::getTodoName)
                            .toList();

                    return new MenteeManagementDto(
                            menteeId,
                            menteeNames.getOrDefault(menteeId, "알 수 없는 멘티"),
                            completedCount,
                            totalCount,
                            unsubmittedTitles
                    );
                })
                .sorted(Comparator.comparing(MenteeManagementDto::menteeName)) // 이름순 정렬
                .collect(Collectors.toList());

        return new MentorHomeResponseDto(assignmentSummaries, feedbackSummaries, qnaSummaries, menteeManagementList);
    }

    private String determineStatus(Todo todo) {
        if ("Y".equals(todo.getTodoCompleteYn())) return "COMPLETED";
        if ("Y".equals(todo.getTodoAssignYn())) return "ASSIGNED";
        if ("N".equals(todo.getTodoDraftYn())) return "DRAFT";
        return "IN_PROGRESS";
    }

    private String determineFeedbackStatus(Feedback feedback) {
        if ("Y".equals(feedback.getFeedCompleteYn())) return "COMPLETED";
        if ("Y".equals(feedback.getFeedDraftYn())) return "DRAFT";
        return "PENDING";
    }

    // 임시저장 개수 조회
    public DraftCountResponseDto getDraftCounts(Long mentorId) {
        log.debug("Draft count calculated directly from DB for mentorId: {}", mentorId);
        List<Long> menteeIds = getMenteeIdsByMentorId(mentorId);

        // 과제 임시저장: 할당된 과제(Y) 중 임시저장 상태(Y)인 것
        long assignmentDraftCount = todoRepository.countByMenteeIdInAndTodoAssignYnAndTodoDraftYnAndDeleteYn(
                menteeIds, "Y", "Y", "N");
        
        // 피드백 임시저장: 임시저장 상태(Y)인 것
        long feedbackDraftCount = feedbackRepository.countByMentorIdAndFeedDraftYnAndDeleteYn(
                mentorId, "Y", "N");

        return new DraftCountResponseDto(assignmentDraftCount, feedbackDraftCount);
    }

    // 멘티별 진행률 통계 계산
    public MenteeProgressResponseDto getMenteeProgress(Long mentorId, Long menteeId) {
        // 멘토가 해당 멘티를 관리하는지 확인
        if (!isMentorManagingMentee(mentorId, menteeId)) {
            throw new CustomException(ErrorCode.RELATION_ACCESS_DENIED);
        }
        
        Map<Long, String> menteeNames = getMenteeNames(Collections.singletonList(menteeId));

        List<Todo> todos = todoRepository.findByMenteeId(menteeId);
        if (todos.isEmpty()) {
            return new MenteeProgressResponseDto(menteeId, menteeNames.getOrDefault(menteeId, "알 수 없는 멘티"), 0.0, List.of());
        }

        long totalTodos = todos.size();
        long completedTodos = todos.stream()
                .filter(todo -> "Y".equals(todo.getTodoCompleteYn()))
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
            List<Todo> totalSubjectTodosList = todoRepository.findByMenteeIdAndTodoSubjects(
                    menteeId, subject
            );
            long totalSubjectTodos = totalSubjectTodosList.size();

            // 해당 과목 중 완료된 할 일만 필터링하여 개수를 얻음
            long completedSubjectTodos = totalSubjectTodosList.stream()
                    .filter(todo -> "Y".equals(todo.getTodoCompleteYn()))
                    .count();
            double subjectProgress = (totalSubjectTodos > 0) ? (double) completedSubjectTodos / totalSubjectTodos * 100 : 0.0;
            subjectProgressList.add(new SubjectProgressDto(subject, subjectProgress));
        }

        return new MenteeProgressResponseDto(menteeId, menteeNames.getOrDefault(menteeId, "알 수 없는 멘티"), overallProgress, subjectProgressList);
    }


    // 멘토가 관리하는 멘티 ID 목록을 가져오는 실제 로직
    private List<Long> getMenteeIdsByMentorId(Long mentorId) {
        return relationRepository.findByMentorId(mentorId)
                .stream()
                .map(Relation::getMenteeId)
                .collect(Collectors.toList());
    }

    // 멘티 ID를 통해 멘티 이름을 조회하는 헬퍼 메서드
    private Map<Long, String> getMenteeNames(List<Long> menteeIds) {
        return memberRepository.findAllById(menteeIds)
                .stream()
                .collect(Collectors.toMap(Member::getId, Member::getMemName, (oldValue, newValue) -> oldValue)); // 중복 키 발생 시 기존 값 유지
    }

    // 멘토가 특정 멘티를 관리하는지 확인하는 헬퍼 메서드
    private boolean isMentorManagingMentee(Long mentorId, Long menteeId) {
        return relationRepository.findByMentorId(mentorId)
                .stream()
                .anyMatch(relation -> relation.getMenteeId().equals(menteeId));
    }
}
