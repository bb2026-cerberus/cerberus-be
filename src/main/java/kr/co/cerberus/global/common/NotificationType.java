package kr.co.cerberus.global.common;

import kr.co.cerberus.global.util.EnumUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum NotificationType implements BaseEnum<String> {
	FEEDBACK_TODO("FEEDBACK_TODO"),
	FEEDBACK_ASSIGNMENT("FEEDBACK_ASSIGNMENT"),
	REMIND("REMIND");

	private final String code;

	private static final Map<String, NotificationType> codeMap = EnumUtils.createByCodeMap(NotificationType.class);

	public static NotificationType of(String code) {
		return EnumUtils.fromCode(NotificationType.class, codeMap, code);
	}
}
