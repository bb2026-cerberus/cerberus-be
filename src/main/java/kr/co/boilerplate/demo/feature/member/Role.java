package kr.co.boilerplate.demo.feature.member;

import kr.co.boilerplate.demo.global.common.BaseEnum;
import kr.co.boilerplate.demo.global.util.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

//
@Getter
@RequiredArgsConstructor
public enum Role implements BaseEnum<String> {
    ADMIN("ROLE_ADMIN"), USER("ROLE_USER"), GUEST("ROLE_GUEST");
	
	private final String code;
	
	private static final Map<String, Role> codeMap = EnumUtils.createByCodeMap(Role.class);
	
	public static Role of(String code) {
		return EnumUtils.fromCode(Role.class, codeMap, code);
	}
}
