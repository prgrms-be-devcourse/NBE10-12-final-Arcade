package com.back.domain.goal.goal.dtos;

import com.back.domain.member.member.entity.PositionType;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.partyPr.dtos.PartyPrDto;
import com.back.domain.party.position.entity.PartyStatus;
import com.back.domain.party.showcase.entity.PartyShowcase;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PROJECT 성취가 가리키는 파티 정보.
 *
 * PROJECT 성취 자체는 "내가 이 파티에 참여했다"는 연결 정보만 갖고,
 * 제목·저장소·진행 기록 같은 내용은 전부 파티에 있다. 상세 화면에서 필요한 만큼 조립해 내려준다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectContextDto(
        long partyId,
        String partyName,
        String title,
        PartyStatus partyStatus,
        LocalDateTime deadline,
        /** 등록된 저장소. 없으면 null */
        String githubRepoUrl,
        /** 이 파티에서 맡은 포지션. 파티장은 지원 절차가 없어 값이 없다 */
        PositionType myPositionType,
        /** 파티장인지 여부 */
        boolean partyOwner,
        /** 전시 게시글 제목. 게시 전이면 없다 */
        String showcaseTitle,
        /** 전시 게시글 본문. 게시 전이면 없다 */
        String showcaseDescription,
        /** 동기화된 PR 목록. 본인 성취를 볼 때만 채운다 */
        List<PartyPrDto> pullRequests
) {
    public static ProjectContextDto of(
            Party party,
            PositionType myPositionType,
            boolean partyOwner,
            PartyShowcase showcase,
            List<PartyPrDto> pullRequests
    ) {
        return new ProjectContextDto(
                party.getId(),
                party.getPartyName(),
                party.getTitle(),
                party.getStatus(),
                party.getDeadline(),
                party.getGithubRepoUrl(),
                myPositionType,
                partyOwner,
                showcase == null ? null : showcase.getTitle(),
                showcase == null ? null : showcase.getDescription(),
                pullRequests
        );
    }
}
