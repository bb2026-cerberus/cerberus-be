package kr.co.cerberus.feature.qna.repository;

import kr.co.cerberus.feature.qna.Qna;
import kr.co.cerberus.feature.qna.domain.QnaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface QnaRepository extends JpaRepository<Qna, Long> {
    List<Qna> findByMentorIdAndActivateYn(Long mentorId, String activateYn);
    List<Qna> findByMenteeIdAndActivateYn(Long menteeId, String activateYn);
    List<Qna> findByMentorIdAndCreateDatetimeBetweenAndActivateYn(Long mentorId, LocalDateTime startDt, LocalDateTime endDt, String activateYn);
    long countByMentorIdAndStatusAndActivateYn(Long mentorId, QnaStatus status, String activateYn); // 멘토 홈 Q&A 현황용
}
