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
	MENTOR("ROLE_MENTOR"), MENTEE("ROLE_MENTEE");
	
	private final String code;
	
	private static final Map<String, Role> codeMap = EnumUtils.createByCodeMap(Role.class);
	
	public static Role of(String code) {
		return EnumUtils.fromCode(Role.class, codeMap, code);
	}
}
