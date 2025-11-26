package kr.co.boilerplate.demo.global.util;

import kr.co.boilerplate.demo.global.common.BaseEnum;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@UtilityClass
public class EnumUtils {

    public <T extends Enum<T> & BaseEnum<C>, C> Map<C, T> createByCodeMap(Class<T> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .collect(Collectors.toMap(BaseEnum::getCode, Function.identity()));
    }

    public <E extends Enum<E> & BaseEnum<C>, C> E fromCode(Class<E> enumClass, Map<C, E> byCode, C code) {
        E result = byCode.get(code);
        if (result == null) {
            log.error("Invalid code {} for enum {}", code, enumClass.getSimpleName());
            throw new IllegalArgumentException(String.format("지원하지 않는 코드입니다: %s (%s)", code, enumClass.getSimpleName()));
        }
        return result;
    }
}