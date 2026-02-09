package kr.co.cerberus.feature.todo.repository;

import kr.co.cerberus.feature.todo.Todo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, Long> {

	// 멘티 ID, 과제 할당 여부, 삭제 여부, 드래프트 여부로 조회
	List<Todo> findByMenteeIdAndTodoAssignYnAndDeleteYnAndTodoDraftYn(Long menteeId, String assignYn, String deleteYn, String todoDraftYn);

	// 멘티 ID, 날짜, 과제 할당 여부, 삭제 여부, 드래프트 여부로 조회
	List<Todo> findByMenteeIdAndTodoDateAndTodoAssignYnAndDeleteYnAndTodoDraftYn(
			Long menteeId, LocalDate todoDate, String assignYn, String deleteYn, String todoDraftYn);

	// 멘티 ID, 기간, 과제 할당 여부, 삭제 여부, 드래프트 여부로 조회
	List<Todo> findByMenteeIdAndTodoDateBetweenAndTodoAssignYnAndDeleteYnAndTodoDraftYn(
			Long menteeId, LocalDate startDate, LocalDate endDate, String assignYn, String deleteYn, String todoDraftYn);

	// 멘티 ID, 과제 할당 여부, 삭제 여부로 조회 (기존 멘티 API 호환)
	List<Todo> findByMenteeIdAndTodoAssignYnAndDeleteYn(Long menteeId, String assignYn, String deleteYn);

	// 멘티 ID, 날짜, 과제 할당 여부, 삭제 여부로 조회 (기존 멘티 API 호환)
	List<Todo> findByMenteeIdAndTodoDateAndTodoAssignYnAndDeleteYn(
			Long menteeId, LocalDate todoDate, String assignYn, String deleteYn);

	// 멘티 ID, 기간, 과제 할당 여부, 삭제 여부로 조회 (기존 멘티 API 호환)
	List<Todo> findByMenteeIdAndTodoDateBetweenAndTodoAssignYnAndDeleteYn(
			Long menteeId, LocalDate startDate, LocalDate endDate, String assignYn, String deleteYn);

	// 멘토가 자신에게 할당된 멘티들의 과제 조회 (멘토 홈)
	List<Todo> findByMenteeIdInAndTodoAssignYnAndTodoDateBetween(
			List<Long> menteeIds, String assignYn, LocalDate startDate, LocalDate endDate);

	// 특정 멘토의 멘티들이 임시 저장한 과제의 개수
	long countByMenteeIdInAndTodoAssignYnAndTodoDraftYnAndDeleteYn(List<Long> menteeIds, String assignYn, String draftYn, String deleteYn);
	
	// MenteeId로 모든 Todo를 조회 (진행률 계산용)
	List<Todo> findByMenteeId(Long menteeId);

	// 멘티 ID, 과목별 모든 할 일을 가져옴
	List<Todo> findByMenteeIdAndTodoSubjects(Long menteeId, String todoSubjects);
	
	List<Todo> findByMenteeIdInAndTodoDateBetween(Collection<Long> menteeIds, LocalDate todoDateAfter, LocalDate todoDateBefore);
	
	List<Todo> findByMenteeIdAndTodoDateBetweenAndDeleteYn(Long menteeId, LocalDate startDate, LocalDate endDate, String deleteYn);

	List<Todo> findAllByTodoDateBetweenAndTodoAssignYnAndDeleteYnOrderByMenteeId(LocalDate todoDateAfter, LocalDate todoDateBefore, String todoAssignYn, String deleteYn);

    List<Todo> findByMenteeIdAndDeleteYn(Long menteeId, String deleteYn);
}