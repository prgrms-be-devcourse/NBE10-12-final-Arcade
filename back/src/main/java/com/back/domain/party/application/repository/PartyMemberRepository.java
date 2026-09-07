package com.back.domain.party.application.repository;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.party.application.entity.PartyMember;
import com.back.domain.party.application.entity.PartyMemberStatus;
import com.back.domain.party.party.entity.Party;
import com.back.domain.party.position.entity.PartyStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PartyMemberRepository extends JpaRepository<PartyMember, Long> {
    boolean existsByPartyAndMember(Party party, Member member);

    boolean existsByPartyAndMemberAndStatus(Party party, Member member, PartyMemberStatus status);

    // 파티 삭제 전 "승인된 파티원이 하나도 없어야 하는지" 확인할 때 쓴다.
    boolean existsByPartyAndStatus(Party party, PartyMemberStatus status);

    // 파티 삭제 시 남은 PENDING/REJECTED 지원 기록을 함께 정리한다 (APPROVED는 이미 없는 상태여야 호출됨).
    void deleteAllByParty(Party party);

    Optional<PartyMember> findByIdAndParty(long id, Party party);

    // 특정 회원이 그 파티에서 맡은 포지션을 찾을 때 쓴다. 파티 전체를 읽어 메모리에서 거르지 않기 위함.
    @EntityGraph(attributePaths = {"position"})
    Optional<PartyMember> findByPartyAndMember(Party party, Member member);

    @EntityGraph(attributePaths = {"member", "position"})
    List<PartyMember> findAllByParty(Party party);

    // 여러 파티의 파티원을 한 번에 조회 - TOP3처럼 파티가 여러 개일 때 파티마다 쿼리 날리는 걸 방지
    @EntityGraph(attributePaths = {"member"})
    List<PartyMember> findAllByPartyIn(List<Party> parties);

    // 마이페이지 '내 지원' 목록. 카드가 파티 이름·포지션을 그리므로 미리 당겨온다.
    // 전체 건수를 쓰지 않는 화면이라 Slice 로 받아 count 쿼리를 아낀다.
    @EntityGraph(attributePaths = {"party", "position"})
    Slice<PartyMember> findAllByMemberAndStatus(Member member, PartyMemberStatus status, Pageable pageable);

    // 마이페이지 '파티 관리' 목록. 내가 파티장인 파티들에 들어온 지원을 한 번에 본다.
    // partyId·positionType 은 선택 필터라 null 이면 조건이 없는 것으로 친다.
    // 여기도 전체 건수를 쓰지 않는 화면이라 Slice 다.
    @EntityGraph(attributePaths = {"member", "position", "party"})
    @Query("""
            select pm from PartyMember pm
            where pm.party.owner = :owner
              and (:partyId is null or pm.party.id = :partyId)
              and (:positionType is null or pm.position.type = :positionType)
            """)
    Slice<PartyMember> findAllByPartyOwner(
            @Param("owner") Member owner,
            @Param("partyId") Long partyId,
            @Param("positionType") PositionType positionType,
            Pageable pageable
    );

    // 마이페이지 요약의 '완료한 파티' 수.
    @Query("""
            select count(pm) from PartyMember pm
            where pm.member = :member
              and pm.status = :status
              and pm.party.status = :partyStatus
            """)
    long countByMemberAndStatusAndPartyStatus(
            @Param("member") Member member,
            @Param("status") PartyMemberStatus status,
            @Param("partyStatus") PartyStatus partyStatus
    );
}
