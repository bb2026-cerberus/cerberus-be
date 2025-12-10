package kr.co.boilerplate.demo.feature.oauth2;

import kr.co.boilerplate.demo.global.common.BaseEnum;
import kr.co.boilerplate.demo.global.util.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum SocialType implements BaseEnum<String> {
	LOCAL("local"),
	GOOGLE("google"),
	NAVER("naver"),
	KAKAO("kakao"),
	GITHUB("github");

	private final String code;

	private static final Map<String, SocialType> codeMap = EnumUtils.createByCodeMap(SocialType.class);

	public static SocialType of(String code) {
		return EnumUtils.fromCode(SocialType.class, codeMap, code);
	}
}