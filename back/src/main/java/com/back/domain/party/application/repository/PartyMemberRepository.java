package com.back.domain.party.application.repository;

import com.back.domain.member.member.entity.Member;
import com.back.domain.party.application.entity.PartyMember;
import com.back.domain.party.application.entity.PartyMemberStatus;
import com.back.domain.party.party.entity.Party;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartyMemberRepository extends JpaRepository<PartyMember, Long> {
    boolean existsByPartyAndMember(Party party, Member member);

    Optional<PartyMember> findByIdAndParty(long id, Party party);

    // 특정 회원이 그 파티에서 맡은 포지션을 찾을 때 쓴다. 파티 전체를 읽어 메모리에서 거르지 않기 위함.
    @EntityGraph(attributePaths = {"position"})
    Optional<PartyMember> findByPartyAndMember(Party party, Member member);

    @EntityGraph(attributePaths = {"member", "position"})
    List<PartyMember> findAllByParty(Party party);

    boolean existsByPartyAndMemberAndStatus(Party party, Member member, PartyMemberStatus status);

}
