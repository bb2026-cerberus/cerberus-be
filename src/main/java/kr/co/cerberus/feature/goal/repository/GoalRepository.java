package kr.co.cerberus.feature.goal.repository;

import kr.co.cerberus.feature.goal.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoalRepository extends JpaRepository<Goal, Long> {

	Optional<Goal> findByIdAndDeleteYn(Long id, String deleteYn);
}
