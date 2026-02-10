package kr.co.cerberus.feature.qna.repository;

import kr.co.cerberus.feature.qna.Qna;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QnaRepository extends JpaRepository<Qna, Long> {
    List<Qna> findByMentorIdAndCreateDatetimeBetween(Long mentorId, LocalDateTime startDt, LocalDateTime endDt);
	List<Qna> findAllByMenteeIdAndQnaDateAndDeleteYnOrderByCreateDatetime(Long menteeId, LocalDate qnaDate, String deleteYn);
	Optional<Qna> findByIdAndDeleteYn(Long id, String deleteYn);
    List<Qna> findByMentorIdAndQnaDateAndDeleteYnOrderByCreateDatetime(Long mentorId, LocalDate qnaDate, String deleteYn);
}
