package kr.co.cerberus.global.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	// 비즈니스 로직 에러 처리 (400 Bad Request 등)
	@ExceptionHandler(CustomException.class)
	protected ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
		log.error("CustomException: {}, {}", e.getErrorCode(), e.getMessage(), e);
		return ErrorResponse.toResponseEntity(e.getErrorCode());
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {

		log.warn("HandleMethodArgumentNotValidException", ex);

		ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
		return ResponseEntity
				.status(errorCode.getStatus())
				.body(ErrorResponse.toResponseEntity(errorCode, ex.getBindingResult()).getBody());
	}

	// Spring MVC 표준 에러 처리 (405 Method Not Allowed 등)
	@Override
	protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
			HttpRequestMethodNotSupportedException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {

		log.warn("HandleHttpRequestMethodNotSupported: {}", ex.getMessage());

		ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;
		return ResponseEntity
				.status(errorCode.getStatus())
				.body(ErrorResponse.builder()
						.status(errorCode.getStatus().value())
						.error(errorCode.getStatus().name())
						.code(errorCode.name())
						.message(ex.getMessage())
						.build()
				);
	}

	// 그 외 모든 예상치 못한 에러 (500 Internal Server Error)
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("handleException throw Exception : {}", e.getMessage(), e);
        return ErrorResponse.toResponseEntity(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}