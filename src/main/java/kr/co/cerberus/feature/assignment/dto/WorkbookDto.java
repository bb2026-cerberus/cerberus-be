package kr.co.cerberus.feature.assignment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WorkbookDto {
    private String fileName;
    private String fileUrl;
    private String description;
}
