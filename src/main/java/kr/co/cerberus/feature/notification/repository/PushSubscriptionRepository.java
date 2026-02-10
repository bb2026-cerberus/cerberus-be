package kr.co.cerberus.feature.notification.repository;

import kr.co.cerberus.feature.notification.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
//    PushSubscription findByMenteeIdAndEndpoint(Long menteeId, String endpoint);
    void deleteByMenteeIdAndEndpoint(Long menteeId, String endpoint);
    List<PushSubscription> findByMenteeId(Long menteeId);
}
