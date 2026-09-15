package com.back.domain.party.assemble.repository;

import com.back.domain.member.member.entity.Member;
import com.back.domain.party.assemble.entity.PartyAssembleToMember;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.position.entity.PartyStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 파티가 확정된 뒤의 최종 멤버를 담고 있음
 *
 * 확정 명단은 파티장을 맨 앞에 넣고 저장하므로 id 순이 곧 그 순서다.
 */
public interface PartyAssembleToMemberRepository extends JpaRepository<PartyAssembleToMember, Long> {

    @EntityGraph(attributePaths = {"member"})
    List<PartyAssembleToMember> findAllByPartyAssemble_PartyOrderByIdAsc(Party party);

    // 여러 파티를 한 번에 - TOP3처럼 파티가 여러 개일 때 파티마다 쿼리 날리는 걸 방지
    @EntityGraph(attributePaths = {"member", "partyAssemble"})
    List<PartyAssembleToMember> findAllByPartyAssemble_PartyInOrderByIdAsc(List<Party> parties);

    // 마이페이지 요약의 '완료한 파티' 수 - 내가 확정 명단에 든 파티 중 완료된 것.
    @Query("""
            select count(atm) from PartyAssembleToMember atm
            where atm.member = :member
              and atm.partyAssemble.party.status = :partyStatus
            """)
    long countByMemberAndPartyStatus(
            @Param("member") Member member,
            @Param("partyStatus") PartyStatus partyStatus
    );
}
