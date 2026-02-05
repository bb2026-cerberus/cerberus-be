package kr.co.cerberus.feature.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class NotificationListResponseDto {
	private List<NotificationDto> content;

	@Getter
	@Builder
	public static class NotificationDto {
		private Long notificationId;
		private String notificationType;
		private Long dataId;
		private String title;
		private String content;
		private LocalDateTime sentAt;
		private boolean read;
	}
}
