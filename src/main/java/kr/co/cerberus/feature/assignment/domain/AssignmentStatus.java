package kr.co.cerberus.feature.assignment.domain;

import kr.co.cerberus.global.common.BaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AssignmentStatus implements BaseEnum<String> {
    DRAFT("DRAFT", "임시저장"),
    ASSIGNED("ASSIGNED", "과제 할당"),
    IN_PROGRESS("IN_PROGRESS", "진행중"),
    COMPLETED("COMPLETED", "완료"),
    CANCELED("CANCELED", "취소됨");

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
}
