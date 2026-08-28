package com.back.domain.interaction.like.service;

import com.back.domain.interaction.like.dtos.LikeDto;
import com.back.domain.interaction.like.entity.LikeAction;
import com.back.domain.interaction.like.entity.TargetType;
import com.back.domain.interaction.like.repository.LikeActionRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.party.repository.PartyRepository;
import com.back.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {

    private final LikeActionRepository likeActionRepository;
    private final PartyRepository partyRepository;

    @Transactional
    public LikeDto likeParty(long partyId, Member member) {
        Party party = findPartyOrThrow(partyId);

        if (likeActionRepository.existsByMemberAndTargetTypeAndTargetId(member, TargetType.PARTY, partyId)) {
            throw new ServiceException("409-1", "이미 좋아요한 파티입니다.");
        }

        likeActionRepository.save(new LikeAction(member, TargetType.PARTY, partyId));
        party.increaseLikeCount();

        return new LikeDto(TargetType.PARTY, partyId, true, party.getLikeCount());
    }

    @Transactional
    public LikeDto unlikeParty(long partyId, Member member) {
        Party party = findPartyOrThrow(partyId);

        LikeAction likeAction = likeActionRepository
                .findByMemberAndTargetTypeAndTargetId(member, TargetType.PARTY, partyId)
                .orElseThrow(() -> new ServiceException("409-1", "좋아요하지 않은 파티입니다."));

        likeActionRepository.delete(likeAction);
        party.decreaseLikeCount();

        return new LikeDto(TargetType.PARTY, partyId, false, party.getLikeCount());
    }

    private Party findPartyOrThrow(long partyId) {
        return partyRepository.findById(partyId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 파티입니다."));
    }
}
