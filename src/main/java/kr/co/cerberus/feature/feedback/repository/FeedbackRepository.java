package kr.co.cerberus.feature.feedback.repository;

import kr.co.cerberus.feature.feedback.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

	Optional<Feedback> findByTodoIdAndDeleteYn(Long todoId, String deleteYn);

	List<Feedback> findByTodoIdInAndDeleteYn(List<Long> todoIds, String deleteYn);

	List<Feedback> findByTodoIdInAndFeedDateBetweenAndDeleteYn(
			List<Long> todoIds, LocalDate startDate, LocalDate endDate, String deleteYn);
}
