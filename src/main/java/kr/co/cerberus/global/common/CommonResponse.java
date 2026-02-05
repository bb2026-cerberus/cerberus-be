package kr.co.cerberus.global.common;

import lombok.Builder;
import org.springframework.http.HttpStatus;

@Builder
public record CommonResponse<T>(boolean success, T data) {

	public static <T> CommonResponse<T> of(T data) {
		return new CommonResponse<>(true, data);
	}

	public static <T> CommonResponse<T> error(T data) {
		return new CommonResponse<>(false, data);
	}
}