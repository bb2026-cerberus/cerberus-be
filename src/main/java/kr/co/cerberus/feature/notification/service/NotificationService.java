package kr.co.cerberus.feature.notification.service;

import kr.co.cerberus.feature.notification.Notification;
import kr.co.cerberus.feature.notification.dto.NotificationListResponseDto;
import kr.co.cerberus.feature.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationService {

	private final NotificationRepository notificationRepository;

	public NotificationListResponseDto findNotifications(Long menteeId) {
		List<Notification> notifications = notificationRepository.findByMenteeIdOrderBySentAtDesc(menteeId);

		List<NotificationListResponseDto.NotificationDto> content = notifications.stream()
				.map(noti -> NotificationListResponseDto.NotificationDto.builder()
						.notificationId(noti.getId())
						.notificationType(noti.getNotificationType().name())
						.dataId(noti.getDataId())
						.title(noti.getTitle())
						.content(noti.getContent())
						.sentAt(noti.getSentAt())
						.read(noti.isRead())
						.build())
				.toList();

		return NotificationListResponseDto.builder()
				.content(content)
				.build();
	}
}
