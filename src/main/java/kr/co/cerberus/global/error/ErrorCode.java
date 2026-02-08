package kr.co.cerberus.global.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ErrorCode {
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "파라미터 값을 확인해주세요."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP Method입니다."),
	INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
	RELATION_ACCESS_DENIED(HttpStatus.BAD_REQUEST, "해당 멘티에 대한 접근 권한이 없습니다."),
	// TODO: 도메인별(Assignment, Feedback, Report 등) 세분화된 에러 코드 정의 필요
	// TODO: 현재 시스템은 Name 기반이므로 Email 관련 코드 수정 또는 확장 필요
	LOCAL_LOGIN_FAIL(HttpStatus.BAD_REQUEST, "로그인 실패! 이름이나 비밀번호를 확인해주세요."),
	    OAUTH2_LOGIN_FAIL(HttpStatus.BAD_REQUEST, "소셜 로그인에 실패했습니다."),
	    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "이미 존재하는 이메일입니다."),
	
	
	    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 에러입니다."),    UNAUTHORIZED_USER(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");

    private final HttpStatus status;
    private final String message;
}