package kr.co.cerberus.feature.feedback.repository;

import kr.co.cerberus.feature.feedback.Feedback;
import kr.co.cerberus.feature.feedback.domain.FeedbackStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

	Optional<Feedback> findByTodoIdAndDeleteYn(Long todoId, String deleteYn);

	// 기존 멘티 API 호환을 위해 유지
	List<Feedback> findByTodoIdInAndDeleteYn(List<Long> todoIds, String deleteYn);

	// 기존 멘티 API 호환을 위해 유지
	List<Feedback> findByTodoIdInAndFeedDateBetweenAndDeleteYn(
			List<Long> todoIds, LocalDate startDate, LocalDate endDate, String deleteYn);

	// 멘토 ID로 피드백 조회 (멘토 홈)
	List<Feedback> findByMentorIdAndActivateYn(Long mentorId, String activateYn);

	// 멘토 ID, 특정 기간, 특정 상태의 피드백 조회 (멘토 홈)
	List<Feedback> findByMentorIdAndStatusInAndCreateDatetimeBetweenAndActivateYn(
			Long mentorId, List<FeedbackStatus> statuses, LocalDateTime startDt, LocalDateTime endDt, String activateYn);

	// 멘토가 관리하는 멘티의 임시 저장 피드백 개수
	// (멘토-멘티 관계가 명확하지 않아, 이 메서드는 멘토가 관리하는 모든 멘티의 임시저장 개수로 가정)
	// 멘티 목록을 받아 처리
	long countByMentorIdAndStatusAndActivateYn(Long mentorId, FeedbackStatus status, String activateYn);

	Optional<Feedback> findByIdAndActivateYn(Long id, String activateYn);
}
