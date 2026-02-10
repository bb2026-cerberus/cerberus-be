package kr.co.cerberus.feature.notification;

import jakarta.persistence.*;
import kr.co.cerberus.global.common.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(schema = "master", name = "tb_push_subscription")
public class PushSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "push_sub_seq")
    private Long id;

    @Column(name = "mentee_seq", nullable = false)
    private Long menteeId;

    @Column(name = "endpoint", columnDefinition = "TEXT", nullable = false)
    private String endpoint;

    @Column(name = "p256dh", columnDefinition = "TEXT", nullable = false)
    private String p256dh;

    @Column(name = "auth", columnDefinition = "TEXT", nullable = false)
    private String auth;
}
