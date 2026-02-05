package kr.co.cerberus.feature.notification;

import jakarta.persistence.*;
import kr.co.cerberus.global.common.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "master", name = "tb_notification")
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "noti_seq")
	private Long id;

	@Column(name = "mentee_seq")
	private Long menteeId;

	@Enumerated(EnumType.STRING)
	@Column(name = "noti_type")
	private NotificationType notificationType;

	@Column(name = "data_id")
	private Long dataId;

	@Column(name = "noti_title")
	private String title;

	@Column(name = "noti_content")
	private String content;

	@Column(name = "sent_at")
	private LocalDateTime sentAt;

	@Builder.Default
	@Column(name = "read_yn", columnDefinition = "bpchar(1) default 'N'")
	private String readYn = "N";

	public boolean isRead() {
		return "Y".equals(this.readYn);
	}

	public void markRead() {
		this.readYn = "Y";
	}
}
