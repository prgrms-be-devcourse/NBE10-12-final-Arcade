package com.back.domain.party.application.repository;

import com.back.domain.member.member.entity.Member;
import com.back.domain.party.application.dtos.PartyApplicantCount;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.party.application.entity.PartyMember;
import com.back.domain.party.application.entity.PartyMemberStatus;
import com.back.domain.party.party.entity.Party;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public interface PartyMemberRepository extends JpaRepository<PartyMember, Long> {
    boolean existsByPartyAndMember(Party party, Member member);

    boolean existsByPartyAndMemberAndStatus(Party party, Member member, PartyMemberStatus status);

    // 파티 삭제 전 "파티장을 제외한 승인된 파티원이 하나도 없어야 하는지" 확인할 때 쓴다.
    // 파티 생성 시 파티장도 PartyMember 로 들어가고, 파티장을 APPROVED 로 넣던 시절(ARC-97)에
    // 만들어진 파티에는 그 행이 남아 있다. 그걸 세면 파티장이 자기 파티를 못 지우므로 빼고 센다.
    boolean existsByPartyAndStatusAndMemberNot(Party party, PartyMemberStatus status, Member member);


    Optional<PartyMember> findByIdAndParty(long id, Party party);

    // 특정 회원이 그 파티에서 맡은 포지션을 찾을 때 쓴다. 파티 전체를 읽어 메모리에서 거르지 않기 위함.
    @EntityGraph(attributePaths = {"position"})
    Optional<PartyMember> findByPartyAndMember(Party party, Member member);

    @EntityGraph(attributePaths = {"member"})
    Optional<PartyMember> findByParty_IdAndMember_IdAndStatus(
            long partyId,
            long memberId,
            PartyMemberStatus status
    );

    @EntityGraph(attributePaths = {"member"})
    Optional<PartyMember> findByPartyAndMember_GithubUserIdAndStatus(
            Party party,
            Long githubUserId,
            PartyMemberStatus status
    );

    @EntityGraph(attributePaths = {"member", "position"})
    List<PartyMember> findAllByParty(Party party);

    // 파티를 지우기 전에 먼저 지운다. 안 그러면 position 을 참조하는 FK 때문에 삭제가 실패한다.
    void deleteAllByParty(Party party);

    // 여러 파티의 파티원을 한 번에 조회 - TOP3처럼 파티가 여러 개일 때 파티마다 쿼리 날리는 걸 방지
    @EntityGraph(attributePaths = {"member"})
    List<PartyMember> findAllByPartyIn(List<Party> parties);

    // 마이페이지 '내 지원' 목록. 카드가 파티 이름·포지션을 그리므로 미리 당겨온다.
    // 전체 건수를 쓰지 않는 화면이라 Slice 로 받아 count 쿼리를 아낀다.
    @EntityGraph(attributePaths = {"party", "position"})
    Slice<PartyMember> findAllByMemberAndStatus(Member member, PartyMemberStatus status, Pageable pageable);

    // 참여 파티 히스토리에서 파티별 내 포지션을 채운다. 파티 수만큼 조회하지 않으려고 한 번에 읽는다.
    // party 까지 함께 당기는 건 결과를 파티 id 로 묶기 때문이다 - 빼면 그 접근이 다시 파티마다 쿼리를 낸다.
    @EntityGraph(attributePaths = {"party", "position"})
    List<PartyMember> findAllByMemberAndStatus(Member member, PartyMemberStatus status);

    // 마이페이지 '파티 관리' 목록. 내가 파티장인 파티들에 들어온 지원을 한 번에 본다.
    // partyId·positionType 은 선택 필터라 null 이면 조건이 없는 것으로 친다.
    // 여기도 전체 건수를 쓰지 않는 화면이라 Slice 다.
    // pm.member <> :owner 는 옛 데이터 방어다 - ARC-97 시절 파티에는 파티장의 APPROVED 행이 남아 있어
    // 그냥 두면 파티장이 자기 파티의 지원자로 보인다.
    @EntityGraph(attributePaths = {"member", "position", "party"})
    @Query("""
            select pm from PartyMember pm
            where pm.party.owner = :owner
              and pm.member <> :owner
              and (:partyId is null or pm.party.id = :partyId)
              and (:positionType is null or pm.position.type = :positionType)
            """)
    Slice<PartyMember> findAllByPartyOwner(
            @Param("owner") Member owner,
            @Param("partyId") Long partyId,
            @Param("positionType") PositionType positionType,
            Pageable pageable
    );

    // 목록 카드의 '지원자 N명'. 승인 인원(Position.filledCount)과 다른 값이라 따로 센다 -
    // 거절된 건까지 포함한, 그 파티에 지원한 사람 수 전체다.
    // pm.member <> pm.party.owner 는 옛 데이터 방어다 (findAllByPartyOwner 와 같은 이유).
    @Query("""
            select new com.back.domain.party.application.dtos.PartyApplicantCount(pm.party.id, count(pm))
            from PartyMember pm
            where pm.party.id in :partyIds
              and pm.member <> pm.party.owner
            group by pm.party.id
            """)
    List<PartyApplicantCount> countApplicantsByPartyIdIn(@Param("partyIds") Collection<Long> partyIds);

    // 파티 상세의 '지원자 N명' 단건. 위 집계와 같은 기준(거절 포함, 파티장 제외).
    @Query("""
            select count(pm)
            from PartyMember pm
            where pm.party.id = :partyId
              and pm.member <> pm.party.owner
            """)
    long countApplicantsByPartyId(@Param("partyId") long partyId);

    // 위 집계를 카드 조립에서 바로 쓰기 좋은 모양으로. 지원자가 없는 파티는 행이 아예 없어 getOrDefault 로 읽는다.
    // 목록이 비면 여기서 끊는다 - @Query 라 빈 in 절이어도 DB 까지 나가고, 검색 0건이나
    // 관련 파티 없는 대회처럼 빈 목록으로 부르는 화면이 흔하다. 호출처마다 막지 않고 여기 한 곳에서 막는다.
    default Map<Long, Long> countApplicantsByPartyIds(Collection<Long> partyIds) {
        if (partyIds.isEmpty()) {
            return Map.of();
        }

        return countApplicantsByPartyIdIn(partyIds).stream()
                .collect(Collectors.toMap(PartyApplicantCount::partyId, PartyApplicantCount::count));
    }
}
