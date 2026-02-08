package kr.co.cerberus.feature.solution.repository;

import kr.co.cerberus.feature.solution.Solution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SolutionRepository extends JpaRepository<Solution, Long> {
    List<Solution> findByMentorId(Long mentorId);
    List<Solution> findByMentorIdAndTitleContainingIgnoreCase(Long mentorId, String title);
}