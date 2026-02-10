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

    private final MemberRepository memberRepository;
    private final TodoRepository  todoRepository;

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
}
