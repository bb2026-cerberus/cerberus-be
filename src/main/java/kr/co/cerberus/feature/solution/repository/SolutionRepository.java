package kr.co.cerberus.feature.solution.repository;

import kr.co.cerberus.feature.solution.Solution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SolutionRepository extends JpaRepository<Solution, Long> {
    List<Solution> findByMentorIdAndActivateYn(Long mentorId, String activateYn);
    List<Solution> findByMentorIdAndTitleContainingIgnoreCaseAndActivateYn(Long mentorId, String title, String activateYn);
    Optional<Solution> findByIdAndDeleteYn(Long id, String deleteYn);

}
