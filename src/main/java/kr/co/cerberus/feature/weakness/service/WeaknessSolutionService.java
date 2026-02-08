package kr.co.cerberus.feature.weakness.service;

import kr.co.cerberus.feature.member.Member;
import kr.co.cerberus.feature.member.Role;
import kr.co.cerberus.feature.member.repository.MemberRepository;
import kr.co.cerberus.feature.relation.repository.RelationRepository;
import kr.co.cerberus.feature.weakness.WeaknessSolution;
import kr.co.cerberus.feature.weakness.dto.WeaknessSolutionCreateRequestDto;
import kr.co.cerberus.feature.weakness.dto.WeaknessSolutionResponseDto;
import kr.co.cerberus.feature.weakness.dto.WeaknessSolutionUpdateRequestDto;
import kr.co.cerberus.feature.weakness.repository.WeaknessSolutionRepository;
import kr.co.cerberus.global.error.CustomException;
import kr.co.cerberus.global.error.ErrorCode;
import kr.co.cerberus.global.util.JsonbUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeaknessSolutionService {

    private final WeaknessSolutionRepository weaknessSolutionRepository;
    private final MemberRepository memberRepository; // 멘티/멘토 유효성 검증용
    private final RelationRepository relationRepository; // 멘토-멘티 관계 검증용

    // 약점 솔루션 생성
    @Transactional
    public WeaknessSolutionResponseDto createWeaknessSolution(Long mentorId, WeaknessSolutionCreateRequestDto requestDto) {
        // 요청하는 멘토와 솔루션의 멘토 ID 일치 여부 확인
        if (!Objects.equals(requestDto.mentorId(), mentorId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "본인의 약점 솔루션만 생성할 수 있습니다.");
        }
        // 멘토 및 멘티 ID 유효성 및 관계 검증
        validateMentorMenteeRelation(mentorId, requestDto.menteeId());

        WeaknessSolution weaknessSolution = WeaknessSolution.builder()
                .menteeId(requestDto.menteeId())
                .mentorId(mentorId)
                .subject(requestDto.subject())
                .weaknessDescription(requestDto.weaknessDescription())
                .solutionContent(requestDto.solutionContent())
                .solutionFile(JsonbUtils.toJson(requestDto.solutionFiles()))
                .build();
        WeaknessSolution savedSolution = weaknessSolutionRepository.save(weaknessSolution);
        return mapToResponseDto(savedSolution);
    }

    // 약점 솔루션 수정
    @Transactional
    public WeaknessSolutionResponseDto updateWeaknessSolution(Long mentorId, WeaknessSolutionUpdateRequestDto requestDto) {
        WeaknessSolution weaknessSolution = weaknessSolutionRepository.findById(requestDto.weaknessSolutionId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        // 요청하는 멘토가 해당 솔루션의 소유자인지 확인
        if (!Objects.equals(weaknessSolution.getMentorId(), mentorId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "해당 약점 솔루션을 수정할 권한이 없습니다.");
        }

        weaknessSolution.updateWeaknessSolution(
                requestDto.subject(),
                requestDto.weaknessDescription(),
                requestDto.solutionContent(),
                JsonbUtils.toJson(requestDto.solutionFiles())
        );
        return mapToResponseDto(weaknessSolution);
    }

    // 약점 솔루션 삭제 (비활성화)
    @Transactional
    public void deleteWeaknessSolution(Long mentorId, Long weaknessSolutionId) {
        WeaknessSolution weaknessSolution = weaknessSolutionRepository.findById(weaknessSolutionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        // 요청하는 멘토가 해당 솔루션의 소유자인지 확인
        if (!Objects.equals(weaknessSolution.getMentorId(), mentorId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "해당 약점 솔루션을 삭제할 권한이 없습니다.");
        }
        weaknessSolution.delete();
    }

    // 약점 솔루션 상세 조회
    public WeaknessSolutionResponseDto getWeaknessSolutionDetail(Long weaknessSolutionId) {
        WeaknessSolution weaknessSolution = weaknessSolutionRepository.findById(weaknessSolutionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        return mapToResponseDto(weaknessSolution);
    }

    // 멘토가 특정 멘티의 약점 솔루션 목록 조회
    public List<WeaknessSolutionResponseDto> getWeaknessSolutionsByMentorAndMentee(Long mentorId, Long menteeId) {
        // 멘토-멘티 관계 확인
        validateMentorMenteeRelation(mentorId, menteeId);

        List<WeaknessSolution> solutions = weaknessSolutionRepository.findByMentorIdAndMenteeIdAndActivateYn(mentorId, menteeId, "Y");
        return solutions.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // 멘티가 자신의 약점 솔루션 목록 조회
    public List<WeaknessSolutionResponseDto> getWeaknessSolutionsByMentee(Long menteeId) {
        List<WeaknessSolution> solutions = weaknessSolutionRepository.findByMenteeIdAndActivateYn(menteeId, "Y");
        return solutions.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private WeaknessSolutionResponseDto mapToResponseDto(WeaknessSolution weaknessSolution) {
        return new WeaknessSolutionResponseDto(
                weaknessSolution.getId(),
                weaknessSolution.getMenteeId(),
                weaknessSolution.getMentorId(),
                weaknessSolution.getSubject(),
                weaknessSolution.getWeaknessDescription(),
                weaknessSolution.getSolutionContent(),
                Optional.ofNullable(JsonbUtils.fromJson(weaknessSolution.getSolutionFile(), new TypeReference<List<kr.co.cerberus.global.jsonb.FileInfo>>() {})).orElse(List.of()),
                weaknessSolution.getCreateDatetime(),
                weaknessSolution.getUpdateDatetime()
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
        if (!relationRepository.findByMentorIdAndActivateYn(mentorId, "Y").stream()
                .anyMatch(r -> r.getMenteeId().equals(menteeId))) {
            throw new CustomException(ErrorCode.INVALID_PARAMETER, "멘토와 멘티 간의 관계가 존재하지 않습니다.");
        }
    }
}
