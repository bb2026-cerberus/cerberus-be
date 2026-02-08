package kr.co.cerberus.global.common;

import kr.co.cerberus.global.util.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum Subject implements BaseEnum<String> {
	KOREAN("KOREAN", "국어"),
	ENGLISH("ENGLISH", "영어"),
	MATH("MATH", "수학");

	private final String code;
	private final String description;

	@Override
	public String getCode() {
		return this.code;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	private static final Map<String, Subject> codeMap = EnumUtils.createByCodeMap(Subject.class);

	public static Subject of(String code) {
		return EnumUtils.fromCode(Subject.class, codeMap, code);
	}
}
