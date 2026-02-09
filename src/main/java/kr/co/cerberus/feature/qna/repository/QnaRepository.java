package kr.co.cerberus.feature.qna.repository;

import kr.co.cerberus.feature.qna.Qna;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QnaRepository extends JpaRepository<Qna, Long> {
    List<Qna> findByMentorId(Long mentorId);
    List<Qna> findByMenteeId(Long menteeId);
    List<Qna> findByMentorIdAndCreateDatetimeBetween(Long mentorId, LocalDateTime startDt, LocalDateTime endDt);
	Optional<Qna> findByMenteeIdAndQnaDateAndDeleteYn(Long menteeId, LocalDate qnaDate, String deleteYn);
	Optional<Qna> findByIdAndDeleteYn(Long id, String deleteYn);
	
    // 멘토 홈 Q&A 현황용 - 답변 대기 중인(qnaCompleteYn='N') 개수 조회
    long countByMentorIdAndQnaCompleteYn(Long mentorId, String qnaCompleteYn);
}
