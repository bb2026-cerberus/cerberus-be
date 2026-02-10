package kr.co.cerberus.feature.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.cerberus.feature.notification.dto.NotificationListResponseDto;
import kr.co.cerberus.feature.notification.dto.PushSubscribeRequestDto;
import kr.co.cerberus.feature.notification.service.NotificationService;
import kr.co.cerberus.global.common.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@Tag(name = "Notification", description = "알림 관련 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;

	@Operation(summary = "알림 목록 조회", description = "특정 멘티의 알림 목록을 조회합니다.")
	@GetMapping
	public ResponseEntity<CommonResponse<NotificationListResponseDto>> getNotifications(@RequestParam Long menteeId) {
		NotificationListResponseDto notifications = notificationService.findNotifications(menteeId);
		return ResponseEntity.ok(CommonResponse.of(notifications));
	}

    @PostMapping("/subscribe")
    @Operation(summary = "PWA 푸시 구독 등록", description = "멘티의 Web Push 구독 정보를 저장합니다.")
    public ResponseEntity<CommonResponse<Void>> subscribe(@Valid @RequestBody PushSubscribeRequestDto req) {
        notificationService.saveSubscription(req);
        return ResponseEntity.ok(CommonResponse.of(null));
    }

}
