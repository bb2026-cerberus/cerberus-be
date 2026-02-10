package kr.co.cerberus.feature.solution.service;

import kr.co.cerberus.feature.member.Role;
import kr.co.cerberus.feature.member.repository.MemberRepository;
import kr.co.cerberus.feature.relation.repository.RelationRepository;
import kr.co.cerberus.feature.solution.Solution;
import kr.co.cerberus.feature.solution.dto.SolutionCreateRequestDto;
import kr.co.cerberus.feature.solution.dto.SolutionResponseDto;
import kr.co.cerberus.feature.solution.dto.SolutionUpdateRequestDto;
import kr.co.cerberus.feature.solution.repository.SolutionRepository;
import kr.co.cerberus.global.error.CustomException;
import kr.co.cerberus.global.error.ErrorCode;
import kr.co.cerberus.global.util.JsonbUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import kr.co.cerberus.global.util.FileStorageService;
import kr.co.cerberus.global.jsonb.FileInfo;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SolutionService {

    private final SolutionRepository solutionRepository;
    private final MemberRepository memberRepository;
    private final RelationRepository relationRepository;
    private final FileStorageService fileStorageService;

    // 약점 솔루션 생성
    @Transactional
    public SolutionResponseDto createSolution(Long mentorId, SolutionCreateRequestDto requestDto, MultipartFile file) {
        // 요청하는 멘토와 솔루션의 멘토 ID 일치 여부 확인
        if (!Objects.equals(requestDto.mentorId(), mentorId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "본인의 약점 솔루션만 생성할 수 있습니다.");
        }
        // 멘토 및 멘티 ID 유효성 및 관계 검증
        validateMentorMenteeRelation(mentorId, requestDto.menteeId());

        // 파일 업로드 처리
        FileInfo fileInfo = new FileInfo();
        if (file != null && !file.isEmpty()) {
            fileInfo = new FileInfo(file.getOriginalFilename(), fileStorageService.storeFile(file, "weakness_solutions"), null);
        }

        Solution solution = Solution.builder()
                .menteeId(requestDto.menteeId())
                .mentorId(mentorId)
                .subject(requestDto.subject())
                .solutionContent(requestDto.content())
                .solutionFile(JsonbUtils.toJson(fileInfo))
                .build();
        Solution savedSolution = solutionRepository.save(solution);
        return mapToResponseDto(savedSolution);
    }

    // 약점 솔루션 수정
    @Transactional
    public SolutionResponseDto updateSolution(Long mentorId, SolutionUpdateRequestDto requestDto, MultipartFile file) {
        Solution solution = solutionRepository.findById(requestDto.solutionId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        // 요청하는 멘토가 해당 솔루션의 소유자인지 확인
        if (!Objects.equals(solution.getMentorId(), mentorId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "해당 약점 솔루션을 수정할 권한이 없습니다.");
        }

        // 파일 업로드 처리
        String solutionFileJson = solution.getSolutionFile();
        if (file != null && !file.isEmpty()) {
	        FileInfo fileInfo = new FileInfo(file.getOriginalFilename(), fileStorageService.storeFile(file, "weakness_solutions"), null);
            solutionFileJson = JsonbUtils.toJson(fileInfo);
        }

        solution.updateSolution(requestDto.subject(), requestDto.content(), solutionFileJson);
        return mapToResponseDto(solution);
    }

    // 약점 솔루션 삭제 (비활성화)
    @Transactional
    public void deleteWeaknessSolution(Long mentorId, Long weaknessSolutionId) {
        Solution solution = solutionRepository.findById(weaknessSolutionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        // 요청하는 멘토가 해당 솔루션의 소유자인지 확인
        if (!Objects.equals(solution.getMentorId(), mentorId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "해당 약점 솔루션을 삭제할 권한이 없습니다.");
        }
        solution.delete();
    }

    // 멘토가 특정 멘티의 또는 자신의 전체 약점 솔루션 목록 조회
    public List<SolutionResponseDto> getWeaknessSolutionsByMentorAndMentee(Long mentorId, Long menteeId) {
        List<Solution> solutions;
        
        if (menteeId != null) {
            // 특정 멘티 조회 시 멘토-멘티 관계 확인
            validateMentorMenteeRelation(mentorId, menteeId);
            solutions = solutionRepository.findByMentorIdAndMenteeIdAndDeleteYn(mentorId, menteeId, "N");
        } else {
            // menteeId가 null이면 멘토가 등록한 전체 조회
            solutions = solutionRepository.findByMentorIdAndDeleteYn(mentorId, "N");
        }

        return solutions.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private SolutionResponseDto mapToResponseDto(Solution solution) {
        FileInfo fileInfo = null;
        String fileJson = solution.getSolutionFile();
        if (fileJson != null && !fileJson.isBlank()) {
            try {
                if (fileJson.trim().startsWith("[")) {
                    List<FileInfo> files = JsonbUtils.fromJson(fileJson, new TypeReference<List<FileInfo>>() {});
                    if (files != null && !files.isEmpty()) fileInfo = files.get(0);
                } else {
                    fileInfo = JsonbUtils.fromJson(fileJson, FileInfo.class);
                }
            } catch (Exception e) {
                // ignore
            }
        }

        return new SolutionResponseDto(
                solution.getId(),
                solution.getMenteeId(),
                solution.getMentorId(),
                solution.getSolutionContent(),
                solution.getSubject(),
                fileInfo,
                solution.getCreateDatetime(),
                solution.getUpdateDatetime()
        );
    }
    
    // 멘토와 멘티의 관계 유효성 검증
    private void validateMentorMenteeRelation(Long mentorId, Long menteeId) {
        if (!memberRepository.findById(mentorId).map(m -> m.getRole() == Role.MENTOR).orElse(false)) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER, "유효하지 않은 멘토 ID입니다.");
        }
        if (!memberRepository.findById(menteeId).map(m -> m.getRole() == Role.MENTEE).orElse(false)) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER, "유효하지 않은 멘티 ID입니다.");
        }
        if (relationRepository.findByMentorId(mentorId).stream()
                .noneMatch(r -> r.getMenteeId().equals(menteeId))) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER, "멘토와 멘티 간의 관계가 존재하지 않습니다.");
        }
    }
	
	public Map<Long, String> getAllSolutionContent(Set<Long> solutionIds) {
		return solutionRepository.findAllById(solutionIds).stream()
				.collect(Collectors.toMap(Solution::getId, Solution::getSolutionContent));
	}
	
	public String getSolutionTitleById(Long solutionId) {
		if (solutionId == null) return null;
		return solutionRepository.findByIdAndDeleteYn(solutionId, "N")
				.map(Solution::getSolutionContent)
				.orElse(null);
	}
	
	public List<FileInfo> parseSolutionFiles(Long solutionId) {
		if (solutionId == null) return Collections.emptyList();
		
		return solutionRepository.findByIdAndDeleteYn(solutionId, "N")
				.map(solution -> {
                    String fileJson = solution.getSolutionFile();
                    if (fileJson == null || fileJson.isBlank()) return Collections.<FileInfo>emptyList();
                    
                    try {
                        if (fileJson.trim().startsWith("[")) {
                            List<FileInfo> files = JsonbUtils.fromJson(fileJson, new TypeReference<List<FileInfo>>() {});
                            return Optional.ofNullable(files).orElse(Collections.emptyList());
                        } else {
                            FileInfo file = JsonbUtils.fromJson(fileJson, FileInfo.class);
                            return file != null ? List.of(file) : Collections.<FileInfo>emptyList();
                        }
                    } catch (Exception e) {
                        return Collections.<FileInfo>emptyList();
                    }
				})
				.orElse(Collections.emptyList());
	}
}
