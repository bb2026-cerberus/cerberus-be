package kr.co.cerberus.feature.qna.repository;

import kr.co.cerberus.feature.qna.Qna;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QnaRepository extends JpaRepository<Qna, Long> {
    List<Qna> findByMenteeIdAndActivateYn(Long menteeId, String activateYn);
    List<Qna> findByMentorIdAndCreateDatetimeBetweenAndActivateYn(Long mentorId, LocalDateTime startDt, LocalDateTime endDt, String activateYn);
    List<Qna> findByMentorIdAndDeleteYn(Long mentorId, String deleteYn);
    Optional<Qna> findByMenteeIdAndQnaDateAndDeleteYn(Long menteeId, LocalDate qnaDate, String deleteYn);
    Optional<Qna> findByIdAndDeleteYn(Long id, String deleteYn);
}
