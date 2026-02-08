package kr.co.cerberus.feature.member.service;

import jakarta.persistence.EntityNotFoundException;
import kr.co.cerberus.feature.member.Member;
import kr.co.cerberus.feature.member.dto.LoginResponseDto;
import kr.co.cerberus.feature.member.repository.MemberRepository;
import kr.co.cerberus.feature.member.dto.MemberDetailResponseDto;
import kr.co.cerberus.feature.member.dto.MemberListResponseDto;
import kr.co.cerberus.global.error.CustomException;
import kr.co.cerberus.global.error.ErrorCode;
import kr.co.cerberus.global.error.GlobalExceptionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import kr.co.cerberus.global.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true) // TODO: 성능 최적화를 위해 기본값을 readOnly로 설정하고, CUD 메서드에만 별도 @Transactional 적용
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
	
    public Member findById(Long id) throws EntityNotFoundException {
		return memberRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("검색하신 ID의 Member가 없습니다."));
    }
	
    public MemberDetailResponseDto findMemberDetail(Long id) {
        Member member = findById(id);
	    
        return MemberDetailResponseDto.builder()
                .id(member.getId())
                .name(member.getName())
                .createdTime(member.getCreateDatetime())
                .build();
    }

    @Transactional
    public void delete(Long id) {
        Member member = findById(id);
        member.delete();
    }

    public MemberListResponseDto findAll() {
        List<MemberListResponseDto.MemberDto> memberList = memberRepository.findByDeleteYn("N").stream()
                .map(member -> MemberListResponseDto.MemberDto.builder()
                        .id(member.getId())
                        .name(member.getName())
                        .build())
                .collect(Collectors.toList());

        long totalMembers = memberList.size();

        return MemberListResponseDto.builder()
                .members(memberList)
                .totalMembers(totalMembers)
                .build();
    }

    public LoginResponseDto login(String name, String password) {
        Member member = memberRepository.findByName(name).orElseThrow(() ->
                new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        if (!PasswordUtil.matches(password, member.getPassword())) {
            throw new CustomException(ErrorCode.LOCAL_LOGIN_FAIL);
        }

        return LoginResponseDto.from(member);

    }
}

