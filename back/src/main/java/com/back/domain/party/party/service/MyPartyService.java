package com.back.domain.party.party.service;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.party.application.entity.PartyMemberStatus;
import com.back.domain.party.application.repository.PartyMemberRepository;
import com.back.domain.party.party.dtos.MyPartyDto;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.domain.party.showcase.repository.PartyShowcaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 마이페이지 '참여 파티 히스토리' (기획서 2.11).
 *
 * 지원 현황(PartyApplicationService)과 나눠 둔 것은 보는 축이 달라서다 -
 * 그쪽은 '내가 넣은 지원서'이고 여기는 '내가 속한 파티'다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPartyService {

    private final PartyRepository partyRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final PartyShowcaseRepository partyShowcaseRepository;

    /**
     * 확정 명단에 내가 있는 파티를 최근 개설순으로. 모집 중인 파티는 나오지 않는다.
     *
     * 목록을 먼저 뽑고 포지션·전시 여부는 각각 한 번씩 더 읽어 채운다 - 파티 수만큼 쿼리가 나가지 않게.
     */
    public List<MyPartyDto> getMyParties(Member actor) {
        List<Party> parties = partyRepository.findParticipatingBy(actor);
        if (parties.isEmpty()) return List.of();

        // 파티장은 PartyMember 로 남지 않아 여기에 없다 - 그래서 role 은 파티 소유자와 비교해 정한다.
        Map<Long, PositionType> myPositionByPartyId = partyMemberRepository
                .findAllByMemberAndStatus(actor, PartyMemberStatus.APPROVED)
                .stream()
                .filter(partyMember -> partyMember.getPosition() != null)
                .collect(Collectors.toMap(
                        partyMember -> partyMember.getParty().getId(),
                        partyMember -> partyMember.getPosition().getType(),
                        (first, ignored) -> first));

        Set<Long> exhibitedPartyIds = Set.copyOf(partyShowcaseRepository.findPublishedPartyIdsIn(
                parties.stream().map(Party::getId).toList()));

        return parties.stream()
                .map(party -> new MyPartyDto(
                        party,
                        party.getOwner().equals(actor)
                                ? MyPartyDto.MyPartyRole.OWNER
                                : MyPartyDto.MyPartyRole.MEMBER,
                        myPositionByPartyId.get(party.getId()),
                        exhibitedPartyIds.contains(party.getId())))
                .toList();
    }
}
