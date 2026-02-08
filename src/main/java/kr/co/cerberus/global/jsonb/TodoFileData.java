package kr.co.cerberus.global.jsonb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * tb_todo.todo_file JSONB 구조
 *
 * {
 *   "workbooks": [{"fileName": "학습지.pdf", "fileUrl": "/files/101"}],
 *   "verificationImages": [{"fileName": "인증사진1.jpg", "fileUrl": "/files/201"}]
 * }
 */
@Getter
@Builder(toBuilder = true) // toBuilder = true 추가하여 기존 인스턴스 기반으로 새 빌더 생성
@NoArgsConstructor
@AllArgsConstructor
public class TodoFileData {
	private List<FileInfo> workbooks;
	private List<FileInfo> verificationImages;

	// verificationImages를 초기화하는 정적 팩토리 메서드
	public static TodoFileData withVerificationImages(List<FileInfo> images) {
		return TodoFileData.builder()
				.verificationImages(Optional.ofNullable(images).orElse(Collections.emptyList()))
				.build();
	}

	// workbooks를 초기화하는 정적 팩토리 메서드
	public static TodoFileData withWorkbooks(List<FileInfo> files) {
		return TodoFileData.builder()
				.workbooks(Optional.ofNullable(files).orElse(Collections.emptyList()))
				.build();
	}

	// 기존 인스턴스에 verificationImages를 업데이트하는 메서드
	public TodoFileData updateVerificationImages(List<FileInfo> images) {
		return this.toBuilder()
				.verificationImages(Optional.ofNullable(images).orElse(Collections.emptyList()))
				.build();
	}

	// 기존 인스턴스에 workbooks를 업데이트하는 메서드
	public TodoFileData updateWorkbooks(List<FileInfo> files) {
		return this.toBuilder()
				.workbooks(Optional.ofNullable(files).orElse(Collections.emptyList()))
				.build();
	}
}
