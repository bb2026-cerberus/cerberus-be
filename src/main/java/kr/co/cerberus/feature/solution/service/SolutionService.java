package kr.co.cerberus.feature.solution.service;

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

import java.util.List;
import java.util.Objects;
import java.util.Optional; // Import Optional
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.type.TypeReference; // Import TypeReference

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SolutionService {

    private final SolutionRepository solutionRepository;

    // 솔루션 생성
    @Transactional
    public SolutionResponseDto createSolution(SolutionCreateRequestDto requestDto) {
        // TODO: 멘토 ID 유효성 검증 (memberRepository를 통해 실제로 존재하는 멘토인지 확인)
        // TODO: 보안 - 현재 로그인한 사용자의 ID가 requestDto.mentorId()와 일치하는지 검증 로직 추가
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

        // 요청하는 멘토가 해당 솔루션의 소유자인지 확인
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

        // 요청하는 멘토가 해당 솔루션의 소유자인지 확인
        if (!Objects.equals(solution.getMentorId(), mentorId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "해당 솔루션을 삭제할 권한이 없습니다.");
        }

        solution.delete(); // BaseEntity의 delete() 메서드 활용
    }

    // 솔루션 상세 조회
    public SolutionResponseDto getSolutionDetail(Long solutionId) {
        Solution solution = solutionRepository.findById(solutionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        return mapToResponseDto(solution);
    }

    // 멘토별 솔루션 목록 조회
    public List<SolutionResponseDto> getSolutionsByMentor(Long mentorId) {
        List<Solution> solutions = solutionRepository.findByMentorIdAndActivateYn(mentorId, "Y");
        return solutions.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // 멘토별 솔루션 검색 (제목 기준)
    public List<SolutionResponseDto> searchSolutionsByTitle(Long mentorId, String title) {
        List<Solution> solutions = solutionRepository.findByMentorIdAndTitleContainingIgnoreCaseAndActivateYn(mentorId, title, "Y");
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
}
