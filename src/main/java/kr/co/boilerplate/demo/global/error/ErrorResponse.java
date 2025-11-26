package kr.co.boilerplate.demo.global.error;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Error Response Dto
 * @Example
 * <pre>
 * {
 *     "timestamp": "2024-05-21T10:15:30",
 *     "status": 400,
 *     "error": "BAD_REQUEST",
 *     "code": "INVALID_INPUT_VALUE",
 *     "message": "입력값이 올바르지 않습니다.",
 *     "errors": [
 *         {
 *             "field": "email",
 *             "value": "wrong-email",
 *             "reason": "이메일 형식이 아닙니다."
 *         },
 *         {
 *             "field": "password",
 *             "value": "1234",
 *             "reason": "비밀번호는 8자 이상이어야 합니다."
 *         }
 *     ]
 * }
 * </pre>
 * ```
 */
@Getter
@Builder
public class ErrorResponse {
    private final LocalDateTime timestamp = LocalDateTime.now();
    private final int status;
    private final String error;
    private final String code;
    private final String message;
	private final List<ValidationError> errors;

    public static ResponseEntity<ErrorResponse> toResponseEntity(ErrorCode errorCode) {
        return toResponseEntity(errorCode, null);
    }

	public static ResponseEntity<ErrorResponse> toResponseEntity(ErrorCode errorCode, BindingResult bindingResult) {
		return ResponseEntity
				.status(errorCode.getStatus())
				.body(ErrorResponse.builder()
						.status(errorCode.getStatus().value())
						.error(errorCode.getStatus().name())
						.code(errorCode.name())
						.message(errorCode.getMessage())
						.errors(bindingResult != null ? ValidationError.of(bindingResult) : null)
						.build()
				);
	}

	public static String makeMessage(ErrorCode errorCode) {
		return errorCode.name() + errorCode.getMessage();
	}

	@Builder
	public record ValidationError(
			String field,
			String value,
			String reason
	) {
		public static List<ValidationError> of(BindingResult bindingResult) {
			return bindingResult.getFieldErrors().stream()
					.map(error -> ValidationError.builder().field(error.getField())
							.value(error.getRejectedValue() == null ? "" : error.getRejectedValue().toString())
							.reason(error.getDefaultMessage()).build())
					.collect(Collectors.toList());
		}
	}
}