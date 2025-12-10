package kr.co.boilerplate.demo.feature.member.Service;

import jakarta.persistence.EntityNotFoundException;
import kr.co.boilerplate.demo.feature.member.Dto.*;
import kr.co.boilerplate.demo.feature.member.Member;
import kr.co.boilerplate.demo.feature.member.Repository.MemberRepository;
import kr.co.boilerplate.demo.feature.member.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
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
                .nickName(member.getNickName())
                .email(member.getEmail())
                .createdTime(member.getCreatedTime())
                .build();
    }


    public void update(Long id, MemberUpdateRequestDto memberUpdateRequestDto) {
        Member member = findById(id);
		
        member.update(
                memberUpdateRequestDto.getEmail(),
                memberUpdateRequestDto.getImageUrl()
        );
        memberRepository.save(member);
    }


    public void delete(Long id) {
        Member member = findById(id);
        member.deleteMember();
    }

    public MemberListResponseDto findAll() {
        List<MemberListResponseDto.MemberDto> memberList = memberRepository.findByDeleteYn("N").stream()
                .map(member -> MemberListResponseDto.MemberDto.builder()
                        .id(member.getId())
                        .nickName(member.getNickName())
                        .email(member.getEmail())
                        .build())
                .collect(Collectors.toList());

        // 삭제되지 않은 회원만 카운트
        long totalMembers = memberList.size();

        return MemberListResponseDto.builder()
                .members(memberList)
                .totalMembers(totalMembers)
                .build();
    }
}

