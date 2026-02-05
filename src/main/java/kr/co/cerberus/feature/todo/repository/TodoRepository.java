package kr.co.cerberus.feature.todo.repository;

import kr.co.cerberus.feature.todo.Todo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, Long> {

	// 할일 전체 조회 (과제 제외)
	List<Todo> findByMenteeIdAndTodoAssignYnAndDeleteYn(Long menteeId, String assignYn, String deleteYn);

	// 할일 일별 조회
	List<Todo> findByMenteeIdAndTodoDateAndTodoAssignYnAndDeleteYn(
			Long menteeId, LocalDate todoDate, String assignYn, String deleteYn);

	// 할일 기간별 조회
	List<Todo> findByMenteeIdAndTodoDateBetweenAndTodoAssignYnAndDeleteYn(
			Long menteeId, LocalDate startDate, LocalDate endDate, String assignYn, String deleteYn);
}
