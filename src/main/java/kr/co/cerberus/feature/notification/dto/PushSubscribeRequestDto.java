package kr.co.cerberus.feature.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PushSubscribeRequestDto (
        @NotNull Long menteeId,
        @NotBlank String endpoint,
        @NotBlank String p256dh,
        @NotBlank String auth
) {}
