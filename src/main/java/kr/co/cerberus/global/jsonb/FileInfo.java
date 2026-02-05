package kr.co.cerberus.global.jsonb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * JSONB 공통 파일 정보 모델
 * todo_file, goal_file 등에서 파일 목록 항목으로 사용
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {
	private String fileName;
	private String fileUrl;
	private String description;
}
