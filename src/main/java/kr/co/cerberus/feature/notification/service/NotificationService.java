package kr.co.cerberus.feature.notification.service;

import kr.co.cerberus.feature.notification.Notification;
import kr.co.cerberus.feature.notification.PushSubscription;
import kr.co.cerberus.feature.notification.dto.NotificationListResponseDto;
import kr.co.cerberus.feature.notification.dto.PushSubscribeRequestDto;
import kr.co.cerberus.feature.notification.repository.NotificationRepository;
import kr.co.cerberus.feature.notification.repository.PushSubscriptionRepository;
import kr.co.cerberus.feature.todo.Todo;
import kr.co.cerberus.feature.todo.repository.TodoRepository;
import kr.co.cerberus.global.common.NotificationType;
import kr.co.cerberus.global.config.WebPushProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import kr.co.cerberus.global.config.WebPushProperties;


import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationService {

	private final NotificationRepository notificationRepository;
    private final TodoRepository todoRepository;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final WebPushProperties webPushProperties;

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
    // ✅ 1) 구독 저장
    @Transactional
    public void saveSubscription(PushSubscribeRequestDto req) {
        pushSubscriptionRepository.deleteByMenteeIdAndEndpoint(req.menteeId(), req.endpoint());

        pushSubscriptionRepository.save(PushSubscription.builder()
                .menteeId(req.menteeId())
                .endpoint(req.endpoint())
                .p256dh(req.p256dh())
                .auth(req.auth())
                .build());
    }

    // ✅ 2) “피드백 등록됨” 알림 생성 + 푸시 전송(하나의 진입점)
    @Transactional
    public void notifyFeedbackCompleted(Long menteeId, Long todoId) {

        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found"));

        NotificationType notificationType =
                "Y".equals(todo.getTodoAssignYn())
                        ? NotificationType.FEEDBACK_ASSIGNMENT
                        : NotificationType.FEEDBACK_TODO;

        String title = "피드백이 도착했어요";
        String content = "멘토가 피드백을 등록했어요. 확인해보세요!";

        // 1️⃣ DB 알림 저장
        Notification noti = Notification.builder()
                .menteeId(menteeId)
                .notificationType(notificationType)
                .dataId(todoId)
                .title(title)
                .content(content)
                .sentAt(LocalDateTime.now())
                .readYn("N")
                .build();

        notificationRepository.save(noti);

        // 2️⃣ PWA 푸시 발송
        sendWebPushAsync(
                menteeId,
                title,
                content,
                "/todos/" + todoId
        );
    }

    // ✅ 3) 실제 Web Push 발송
    @Async
    public void sendWebPushAsync(Long menteeId, String title, String body, String url) {
        List<PushSubscription> subs = pushSubscriptionRepository.findByMenteeId(menteeId);;
        if (subs.isEmpty()) return;

        try {
            PushService pushService = new PushService()
                    .setSubject(webPushProperties.getSubject())
                    .setPublicKey(webPushProperties.getPublicKey())
                    .setPrivateKey(webPushProperties.getPrivateKey());

            String payload = """
                    {"title":"%s","body":"%s","url":"%s"}
                    """.formatted(escape(title), escape(body), escape(url));

            for (PushSubscription s : subs) {
                Subscription.Keys keys = new Subscription.Keys(s.getP256dh(), s.getAuth());
                Subscription subscription = new Subscription(s.getEndpoint(), keys);

                nl.martijndwars.webpush.Notification pushNoti =
                        new nl.martijndwars.webpush.Notification(subscription, payload);
                pushService.send(pushNoti);
            }
        } catch (Exception e) {
            log.warn("WebPush send failed. menteeId={}, reason={}", menteeId, e.getMessage());
        }
    }

    private String escape(String v) {
        return v == null ? "" : v.replace("\"", "\\\"");
    }


    @Transactional
    public void notifyIncompleteTodos(LocalDate targetDate) {

        // 1️⃣ 어제까지의 미완료 todo 조회
        List<Todo> todos = todoRepository
                .findByTodoDateBeforeAndTodoCompleteYnAndDeleteYn(
                        targetDate.plusDays(1), // 오늘 포함 안 되게
                        "N",
                        "N"
                );

        if (todos.isEmpty()) return;

        // 2️⃣ 멘티별 그룹핑
        Map<Long, List<Todo>> todosByMentee = todos.stream()
                .collect(Collectors.groupingBy(Todo::getMenteeId));

        // 3️⃣ 멘티별 알림 생성
        for (Map.Entry<Long, List<Todo>> entry : todosByMentee.entrySet()) {
            Long menteeId = entry.getKey();
            List<Todo> list = entry.getValue();

            String title = "아직 완료하지 않은 할 일이 있어요";
            String content = String.format(
                    "미완료된 할 일/과제가 %d개 있어요. 오늘 확인해보세요!",
                    list.size()
            );

            Notification noti = Notification.builder()
                    .menteeId(menteeId)
                    .notificationType(NotificationType.REMIND)
                    .dataId(null)
                    .title(title)
                    .content(content)
                    .sentAt(LocalDateTime.now())
                    .readYn("N")
                    .build();

            notificationRepository.save(noti);

            // 4️⃣ 푸시 (구독 있으면만)
            sendWebPushAsync(
                    menteeId,
                    title,
                    content,
                    "/assignments"
            );
        }
    }



}
