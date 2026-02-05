package kr.co.cerberus.global.jsonb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * tb_todo.todo_file JSONB 구조
 *
 * {
 *   "attachments": [{"fileName": "학습지.pdf", "fileUrl": "/files/101"}],
 *   "verificationImage": "/files/201"
 * }
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoFileData {
	private List<FileInfo> attachments;
	private String verificationImage;

	public static TodoFileData withVerification(String imageUrl) {
		return TodoFileData.builder()
				.verificationImage(imageUrl)
				.build();
	}

	public TodoFileData updateVerificationImage(String imageUrl) {
		return TodoFileData.builder()
				.attachments(this.attachments)
				.verificationImage(imageUrl)
				.build();
	}
}
