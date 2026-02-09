package kr.co.cerberus.feature.feedback.repository;

import kr.co.cerberus.feature.feedback.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

	Optional<Feedback> findByTodoIdAndDeleteYn(Long todoId, String deleteYn);

	// 기존 멘티 API 호환을 위해 유지
	List<Feedback> findByTodoIdInAndDeleteYn(List<Long> todoIds, String deleteYn);
	
	// 멘토 ID로 피드백 조회 (멘토 홈)
	List<Feedback> findByMentorId(Long mentorId);

	// 멘토 ID, 특정 기간, 특정 상태의 피드백 조회 (멘토 홈)
	List<Feedback> findByMentorIdAndFeedCompleteYnAndCreateDatetimeBetween(
			Long mentorId, String feedCompleteYn, LocalDateTime startDt, LocalDateTime endDt);

	// 멘토가 관리하는 멘티의 임시 저장 피드백 개수
	long countByMentorIdAndFeedDraftYn(Long mentorId, String feedDraftYn);

	// 멘티 ID, 특정 기간의 모든 피드백 조회 (주간 리포트용)
	List<Feedback> findByMenteeIdAndFeedDateBetween(Long menteeId, LocalDate startDate, LocalDate endDate);
}