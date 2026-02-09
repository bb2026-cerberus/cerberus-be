package kr.co.cerberus.feature.solution.repository;

import aj.org.objectweb.asm.commons.Remapper;
import kr.co.cerberus.feature.solution.Solution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SolutionRepository extends JpaRepository<Solution, Long> {
    List<Solution> findByMentorIdAndMenteeId(Long mentorId, Long menteeId);
    List<Solution> findByMentorId(Long mentorId);
	
	Optional<Solution> findByIdAndDeleteYn(Long id, String deleteYn);

    List<Solution> findByMenteeId(Long menteeId);
}
