package kr.co.cerberus.feature.todo.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class VerificationResponseDto {
	private List<String> imageUrls;
}
