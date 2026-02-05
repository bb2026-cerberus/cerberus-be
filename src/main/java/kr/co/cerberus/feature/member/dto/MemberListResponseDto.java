package kr.co.cerberus.feature.member.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MemberListResponseDto {
    private List<MemberDto> members;
    private long totalMembers;
	
	@Builder
	public record MemberDto(
			Long id,
			String name
	) {}
}
