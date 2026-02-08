package kr.co.cerberus.feature.member;

import kr.co.cerberus.global.common.BaseEnum;
import kr.co.cerberus.global.util.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

//
@Getter
@RequiredArgsConstructor
public enum Role implements BaseEnum<String> {
	MENTOR("ROLE_MENTOR", "멘토"), MENTEE("ROLE_MENTEE", "멘티");
	
	private final String code;
	private final String description;
	
	private static final Map<String, Role> codeMap = EnumUtils.createByCodeMap(Role.class);
	
	public static Role of(String code) {
		return EnumUtils.fromCode(Role.class, codeMap, code);
	}

	@Override
	public String getCode() {
		return this.code;
	}

	@Override
	public String getDescription() {
		return this.description;
	}
}
