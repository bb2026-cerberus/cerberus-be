package kr.co.cerberus.global.util;

import kr.co.cerberus.global.error.CustomException;
import kr.co.cerberus.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import jakarta.servlet.ServletContext;

/**
 * 로컬 파일 시스템을 활용한 파일 저장 서비스
 */
@Service
public class FileStorageService {

    private final Path fileStorageLocation;
    private final ServletContext servletContext;

    public FileStorageService(@Value("${file.upload-dir}") String uploadDir, ServletContext servletContext) {
        this.fileStorageLocation = Paths.get(uploadDir)
                .toAbsolutePath().normalize();
        this.servletContext = servletContext;
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "업로드 디렉토리를 생성할 수 없습니다.");
        }
    }

    /**
     * 파일을 저장하고 웹 접근 경로를 반환합니다.
     * @param file 멀티파트 파일
     * @param subDir 하위 디렉토리 (도메인별 구분용, 예: "assignments", "todos")
     * @return 웹 접근 가능 URL (/files/...)
     */
    public String storeFile(MultipartFile file, String subDir) {
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        
        try {
            Path targetLocation = this.fileStorageLocation.resolve(subDir).normalize();
            Files.createDirectories(targetLocation);
            
            Path filePath = targetLocation.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return "/files/" + subDir + "/" + fileName;
        } catch (IOException ex) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "파일을 저장할 수 없습니다.");
        }
    }

    /**
     * 웹 접근 URL을 사용하여 파일을 Resource 형태로 로드합니다.
     * @param fileUrl 웹 접근 가능한 파일 URL (예: /files/assignments/uuid_filename.jpg)
     * @return 로드된 Resource 객체
     */
    public Resource loadFileAsResource(String fileUrl) {
        try {
            // fileUrl에서 "/files/" 부분을 제거하고 실제 파일 시스템 경로를 구성
            // 예: "/files/assignments/uuid_filename.jpg" -> "assignments/uuid_filename.jpg"
            String relativePathStr = fileUrl.substring(fileUrl.indexOf("/files/") + "/files/".length());
            Path filePath = this.fileStorageLocation.resolve(relativePathStr).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "파일을 찾을 수 없습니다: " + fileUrl);
            }
        } catch (MalformedURLException ex) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "파일 URL이 잘못되었습니다: " + fileUrl);
        }
    }

    /**
     * Resource 객체의 Content-Type을 결정합니다.
     * @param resource 파일 Resource
     * @return Content-Type 문자열 (예: "image/jpeg", "application/pdf")
     */
    public String getContentType(Resource resource) {
        try {
            // 파일의 실제 Content-Type을 감지
            String contentType = Files.probeContentType(resource.getFile().toPath());
            if (contentType == null) {
                // 특정 Content-Type을 유추할 수 없는 경우, ServletContext를 사용하여 유추 시도
                contentType = servletContext.getMimeType(resource.getFilename());
                if (contentType == null) {
                    contentType = "application/octet-stream"; // 기본값
                }
            }
            return contentType;
        } catch (IOException ex) {
            return "application/octet-stream"; // 기본값
        }
    }
}
