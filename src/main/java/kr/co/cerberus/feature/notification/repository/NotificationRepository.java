package kr.co.cerberus.feature.notification.repository;

import kr.co.cerberus.feature.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	List<Notification> findByMenteeIdOrderBySentAtDesc(Long menteeId);
}
