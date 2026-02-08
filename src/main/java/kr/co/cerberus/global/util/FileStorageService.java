package kr.co.cerberus.global.util;

import kr.co.cerberus.global.error.CustomException;
import kr.co.cerberus.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 로컬 파일 시스템을 활용한 파일 저장 서비스
 */
@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir)
                .toAbsolutePath().normalize();

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
}
