package kr.co.cerberus.feature.todo.repository;

import kr.co.cerberus.feature.todo.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
	List<Todo> findByMenteeIdInAndTodoAssignYnAndTodoDateBetweenAndActivateYn(
			List<Long> menteeIds, String assignYn, LocalDate startDate, LocalDate endDate, String activateYn);

	// 특정 멘토의 멘티들이 임시 저장한 과제의 개수
	long countByMenteeIdInAndTodoAssignYnAndActivateYn(List<Long> menteeIds, String assignYn, String activateYn);

	// 진행률 통계를 위한 완료된 할 일 조회
	List<Todo> findByMenteeIdAndTodoSubjectsAndTodoCompleteYnAndActivateYn(
			Long menteeId, String todoSubjects, String completeYn, String activateYn);

	// MenteeId로 모든 Todo를 조회 (진행률 계산용)
	List<Todo> findByMenteeIdAndActivateYn(Long menteeId, String activateYn);

	// 멘티 ID, 과목, 활성화 여부로 모든 Todo를 조회 (진행률 계산을 위해 특정 과목의 모든 할 일을 가져옴)
	List<Todo> findByMenteeIdAndTodoSubjectsAndActivateYn(Long menteeId, String todoSubjects, String activateYn);

	Optional<Todo> findByIdAndActivateYn(Long id, String activateYn);

    List<Todo> findByMenteeIdAndTodoDateBetweenAndDeleteYn(Long menteeId, LocalDate startDate, LocalDate endDate, String deleteYn);

	long countByMenteeIdInAndTodoDraftYnAfterAndActivateYn(Collection<Long> menteeIds, String todoDraftYnAfter, String activateYn);
}
