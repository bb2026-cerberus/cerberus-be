package kr.co.cerberus.feature.solution.service;

import kr.co.cerberus.feature.solution.Solution;
import kr.co.cerberus.feature.solution.dto.SolutionCreateRequestDto;
import kr.co.cerberus.feature.solution.dto.SolutionResponseDto;
import kr.co.cerberus.feature.solution.dto.SolutionUpdateRequestDto;
import kr.co.cerberus.feature.solution.repository.SolutionRepository;
import kr.co.cerberus.global.error.CustomException;
import kr.co.cerberus.global.error.ErrorCode;
import kr.co.cerberus.global.jsonb.FileInfo;
import kr.co.cerberus.global.util.JsonbUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SolutionService {

    private final SolutionRepository solutionRepository;

    // 솔루션 생성
    @Transactional
    public SolutionResponseDto createSolution(SolutionCreateRequestDto requestDto) {
        Solution solution = Solution.builder()
                .mentorId(requestDto.mentorId())
                .title(requestDto.title())
                .description(requestDto.description())
                .subject(requestDto.subject())
                .solutionFile(JsonbUtils.toJson(requestDto.solutionFiles()))
                .build();
        Solution savedSolution = solutionRepository.save(solution);
        return mapToResponseDto(savedSolution);
    }

    // 솔루션 수정
    @Transactional
    public SolutionResponseDto updateSolution(Long mentorId, SolutionUpdateRequestDto requestDto) {
        Solution solution = solutionRepository.findById(requestDto.solutionId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!Objects.equals(solution.getMentorId(), mentorId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "해당 솔루션을 수정할 권한이 없습니다.");
        }

        solution.updateSolution(
                requestDto.title(),
                requestDto.description(),
                requestDto.subject(),
                JsonbUtils.toJson(requestDto.solutionFiles())
        );
        return mapToResponseDto(solution);
    }

    // 솔루션 삭제 (비활성화)
    @Transactional
    public void deleteSolution(Long mentorId, Long solutionId) {
        Solution solution = solutionRepository.findById(solutionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!Objects.equals(solution.getMentorId(), mentorId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "해당 약점 솔루션을 삭제할 권한이 없습니다.");
        }

        solution.delete();
    }

    // 솔루션 상세 조회
    public SolutionResponseDto getSolutionDetail(Long solutionId) {
        Solution solution = solutionRepository.findById(solutionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        return mapToResponseDto(solution);
    }

    // 멘토별 솔루션 목록 조회
    public List<SolutionResponseDto> getSolutionsByMentor(Long mentorId) {
        List<Solution> solutions = solutionRepository.findByMentorId(mentorId);
        return solutions.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // 멘토별 솔루션 검색 (제목 기준)
    public List<SolutionResponseDto> searchSolutionsByTitle(Long mentorId, String title) {
        List<Solution> solutions = solutionRepository.findByMentorIdAndTitleContainingIgnoreCase(mentorId, title);
        return solutions.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private SolutionResponseDto mapToResponseDto(Solution solution) {
        return new SolutionResponseDto(
                solution.getId(),
                solution.getMentorId(),
                solution.getTitle(),
                solution.getDescription(),
                solution.getSubject(),
                Optional.ofNullable(JsonbUtils.fromJson(solution.getSolutionFile(), new TypeReference<List<kr.co.cerberus.global.jsonb.FileInfo>>() {})).orElse(List.of()),
                solution.getCreateDatetime(),
                solution.getUpdateDatetime()
        );
    }

    public Map<Long, String> getAllSolutionTitle(Set<Long> solutionIds) {
        return solutionRepository.findAllById(solutionIds).stream()
                .collect(Collectors.toMap(Solution::getId, Solution::getTitle));
    }

    public String getSolutionTitleById(Long solutionId) {
        if (solutionId == null) return null;
        return solutionRepository.findById(solutionId)
                .map(Solution::getTitle)
                .orElse(null);
    }

    public List<FileInfo> parseSolutionFiles(Long solutionId) {
        if (solutionId == null) return Collections.emptyList();

        return solutionRepository.findById(solutionId)
                .map(solution -> {
                    List<FileInfo> files = JsonbUtils.fromJson(
                            solution.getSolutionFile(), new TypeReference<List<FileInfo>>() {});
                    return Optional.ofNullable(files).orElse(Collections.emptyList());
                })
                .orElse(Collections.emptyList());
    }

}