package kr.co.cerberus.feature.qna.dto;

import kr.co.cerberus.global.jsonb.FileInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record QnaUpdateRequestDto(
    @NotNull Long qnaId,
    Long relatedEntityId,
    String relatedEntityType,
    @NotBlank String title,
    @NotBlank String questionContent,
    List<FileInfo> qnaFiles
) {}
