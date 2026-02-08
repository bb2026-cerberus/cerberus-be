package kr.co.cerberus.feature.weakness.repository;

import kr.co.cerberus.feature.weakness.WeaknessSolution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WeaknessSolutionRepository extends JpaRepository<WeaknessSolution, Long> {
    List<WeaknessSolution> findByMentorIdAndMenteeIdAndActivateYn(Long mentorId, Long menteeId, String activateYn);
    List<WeaknessSolution> findByMenteeIdAndActivateYn(Long menteeId, String activateYn);
}
