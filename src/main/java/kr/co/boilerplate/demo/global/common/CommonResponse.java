package kr.co.boilerplate.demo.global.common;

import lombok.Builder;
import org.springframework.http.HttpStatus;

@Builder
public record CommonResponse(HttpStatus status, String message, Object data) {
}
