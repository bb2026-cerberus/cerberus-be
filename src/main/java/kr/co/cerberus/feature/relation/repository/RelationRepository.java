package kr.co.cerberus.feature.relation.repository;

import kr.co.cerberus.feature.relation.Relation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RelationRepository extends JpaRepository<Relation, Long> {
    List<Relation> findByMentorIdAndActivateYn(Long mentorId, String activateYn);
    Relation findByMenteeIdAndDeleteYn(Long menteeId, String deleteYn);
}
