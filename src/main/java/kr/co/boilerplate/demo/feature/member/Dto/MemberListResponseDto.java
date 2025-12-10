package kr.co.boilerplate.demo.feature.member.Dto;

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
			String nickName,
			String email
	) {}
}
