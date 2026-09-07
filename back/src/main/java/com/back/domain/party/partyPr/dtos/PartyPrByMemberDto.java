package com.back.domain.party.partyPr.dtos;

import com.back.domain.member.member.entity.Member;

import java.util.List;

/**
 * 파티 PR을 GitHub 계정별로 묶은 응답이다.
 * memberId가 null이면 현재 파티원과 연결되지 않은 외부 GitHub 작성자다.
 */
public record PartyPrByMemberDto(
        Long memberId,
        String memberName,
        Long githubUserId,
        String githubLogin,
        boolean owner,
        List<PartyPrDto> pullRequests
) {
    public static PartyPrByMemberDto member(Member member, boolean owner, List<PartyPrDto> pullRequests) {
        return new PartyPrByMemberDto(
                member.getId(), member.getName(), member.getGithubUserId(), null, owner, List.copyOf(pullRequests)
        );
    }

    public static PartyPrByMemberDto external(
            Long githubUserId,
            String githubLogin,
            List<PartyPrDto> pullRequests
    ) {
        return new PartyPrByMemberDto(null, null, githubUserId, githubLogin, false, List.copyOf(pullRequests));
    }
}
