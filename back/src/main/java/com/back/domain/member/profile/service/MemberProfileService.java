package com.back.domain.member.profile.service;

import com.back.domain.member.profile.dtos.MemberProfileDto;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.profile.entity.MemberProfile;
import com.back.domain.member.profile.repository.MemberProfileRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberProfileService {

    private final MemberProfileRepository memberProfileRepository;

    @Transactional
    public MemberProfileDto me(Member actor) {

        MemberProfile profile = memberProfileRepository.findByMember(actor)
                .orElseGet(() -> memberProfileRepository.save(new MemberProfile(actor)));

        return new MemberProfileDto(profile);

    }

    @Transactional
    public MemberProfileDto modifyProfile(
            Member actor, String nickname, String webpage, String profileImageUrl,
            List<String> positions, List<String> techStacks) {

        MemberProfile profile = memberProfileRepository.findByMember(actor)
                .orElseGet(() -> memberProfileRepository.save(new MemberProfile(actor)));

        profile.modify(nickname, webpage, profileImageUrl, positions, techStacks);

        try {
            memberProfileRepository.saveAndFlush(profile);
        } catch (DataIntegrityViolationException e) {
            throw new ServiceException("409-1", "이미 사용 중인 닉네임입니다.");
        }

        return new MemberProfileDto(profile);
    }
}
