package kr.co.cerberus.feature.solution.repository;

import aj.org.objectweb.asm.commons.Remapper;
import kr.co.cerberus.feature.solution.Solution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SolutionRepository extends JpaRepository<Solution, Long> {
    List<Solution> findByMentorIdAndMenteeIdAndDeleteYn(Long mentorId, Long menteeId, String deleteYn);
    List<Solution> findByMentorIdAndDeleteYn(Long mentorId, String deleteYn);
    List<Solution> findByMenteeIdAndDeleteYn(Long menteeId, String deleteYn);
	Optional<Solution> findByIdAndDeleteYn(Long id, String deleteYn);
}
