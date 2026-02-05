package kr.co.cerberus.global.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class JsonbUtils {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.registerModule(new JavaTimeModule());

	/**
	 * 객체를 JSON 문자열로 직렬화
	 */
	public <T> String toJson(T object) {
		if (object == null) return null;
		try {
			return OBJECT_MAPPER.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			log.error("JSON 직렬화 실패: {}", e.getMessage(), e);
			throw new IllegalArgumentException("JSON 직렬화에 실패했습니다.", e);
		}
	}

	/**
	 * JSON 문자열을 객체로 역직렬화
	 */
	public <T> T fromJson(String json, Class<T> clazz) {
		if (json == null || json.isBlank()) return null;
		try {
			return OBJECT_MAPPER.readValue(json, clazz);
		} catch (JsonProcessingException e) {
			log.error("JSON 역직렬화 실패 (class={}): {}", clazz.getSimpleName(), e.getMessage(), e);
			throw new IllegalArgumentException("JSON 역직렬화에 실패했습니다.", e);
		}
	}

	/**
	 * JSON 문자열을 제네릭 타입으로 역직렬화 (List 등)
	 */
	public <T> T fromJson(String json, TypeReference<T> typeRef) {
		if (json == null || json.isBlank()) return null;
		try {
			return OBJECT_MAPPER.readValue(json, typeRef);
		} catch (JsonProcessingException e) {
			log.error("JSON 역직렬화 실패 (typeRef={}): {}", typeRef.getType(), e.getMessage(), e);
			throw new IllegalArgumentException("JSON 역직렬화에 실패했습니다.", e);
		}
	}
}
