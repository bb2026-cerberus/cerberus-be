package kr.co.cerberus.feature.feedback.service;

import kr.co.cerberus.feature.feedback.Feedback;
import kr.co.cerberus.feature.feedback.dto.SubjectFeedbackResponseDto;
import kr.co.cerberus.feature.feedback.repository.FeedbackRepository;
import kr.co.cerberus.feature.todo.Todo;
import kr.co.cerberus.feature.todo.repository.TodoRepository;
import kr.co.cerberus.global.jsonb.FeedbackFileData;
import kr.co.cerberus.global.util.JsonbUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FeedbackService {

	private final TodoRepository todoRepository;
	private final FeedbackRepository feedbackRepository;

	public SubjectFeedbackResponseDto findFeedbackBySubject(Long menteeId, String subject, LocalDate endDate) {
		LocalDate startDate = endDate.minusDays(7);

		// 해당 멘티의 해당 과목 할일/과제 조회 (기간 내)
		List<Todo> allTodos = new ArrayList<>();
		allTodos.addAll(todoRepository.findByMenteeIdAndTodoDateBetweenAndTodoAssignYnAndDeleteYn(
				menteeId, startDate, endDate, "N", "N"));
		allTodos.addAll(todoRepository.findByMenteeIdAndTodoDateBetweenAndTodoAssignYnAndDeleteYn(
				menteeId, startDate, endDate, "Y", "N"));

		// 과목 필터링
		List<Todo> filteredTodos = allTodos.stream()
				.filter(todo -> subject.equals(todo.getTodoSubjects()))
				.toList();

		// 할일/과제 ID 리스트 추출
		List<Long> todoIds = filteredTodos.stream()
				.map(Todo::getId)
				.toList();

		// 피드백 조회
		List<Feedback> feedbacks = feedbackRepository.findByTodoIdInAndDeleteYn(todoIds, "N");
		Map<Long, Feedback> feedbackMap = feedbacks.stream()
				.collect(Collectors.toMap(Feedback::getTodoId, f -> f, (a, b) -> a));

		// 피드백 아이템 리스트 구성 - feedFile JSONB 파싱
		List<SubjectFeedbackResponseDto.FeedbackItemDto> feedbackItems = filteredTodos.stream()
				.filter(todo -> feedbackMap.containsKey(todo.getId()))
				.map(todo -> {
					Feedback feedback = feedbackMap.get(todo.getId());
					FeedbackFileData feedbackData = JsonbUtils.fromJson(feedback.getFeedFile(), FeedbackFileData.class);

					String type = "Y".equals(todo.getTodoAssignYn()) ? "ASSIGNMENT" : "TODO";
					String summary = (feedbackData != null) ? feedbackData.getSummary() : null;

					return SubjectFeedbackResponseDto.FeedbackItemDto.builder()
							.type(type)
							.dataId(todo.getId())
							.subject(todo.getTodoSubjects())
							.title(todo.getTodoName())
							.feedbackSummary(summary)
							.date(todo.getTodoDate())
							.build();
				})
				.toList();

		// weeklyFeedback: 피드백 content들을 종합한 주간 요약
		String weeklyFeedback = buildWeeklyFeedback(feedbackMap, filteredTodos);

		return SubjectFeedbackResponseDto.builder()
				.weeklyFeedback(weeklyFeedback)
				.assignment(feedbackItems)
				.build();
	}

	private String buildWeeklyFeedback(Map<Long, Feedback> feedbackMap, List<Todo> todos) {
		List<String> contents = todos.stream()
				.filter(todo -> feedbackMap.containsKey(todo.getId()))
				.map(todo -> {
					Feedback feedback = feedbackMap.get(todo.getId());
					FeedbackFileData data = JsonbUtils.fromJson(feedback.getFeedFile(), FeedbackFileData.class);
					return (data != null) ? data.getContent() : null;
				})
				.filter(content -> content != null && !content.isBlank())
				.toList();

		if (contents.isEmpty()) return null;

		return String.join(" ", contents);
	}
}