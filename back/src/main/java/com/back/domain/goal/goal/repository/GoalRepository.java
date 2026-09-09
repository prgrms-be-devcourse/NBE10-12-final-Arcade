package com.back.domain.goal.goal.repository;

import com.back.domain.goal.goal.dtos.OwnerAchievementCount;
import com.back.domain.goal.goal.entity.Goal;
import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.GoalType;
import com.back.domain.goal.goal.entity.PersonalChecklist;
import com.back.domain.goal.goal.entity.Project;
import com.back.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GoalRepository extends JpaRepository<Goal, Long>, GoalRepositoryCustom {

    // 파티 확정 이벤트가 중복 수신돼도 같은 사람에게 같은 파티의 성취가 두 번 생기지 않게 막는다.
    // GOAL.party_assemble_to_member_id UK와 같은 목적이지만, 그 값이 이벤트에 아직 실려오지 않아 이쪽으로 먼저 방어한다.
    //
    // 참여자마다 exists 쿼리를 날리면 인원수만큼 쿼리가 나가므로, 이미 생성된 소유자 id만 한 번에 뽑아 메모리에서 거른다.
    @Query("select g.owner.id from Goal g where g.sourcePartyId = :sourcePartyId and g.type = :type")
    List<Long> findOwnerIdsBySourcePartyIdAndType(
            @Param("sourcePartyId") Long sourcePartyId,
            @Param("type") GoalType type
    );

    // 파티 완료 시 그 파티에서 나온 성취를 한 번에 가져온다.
    // partyAssembleToMemberId를 타고 들어갈 수도 있지만 PARTY_ASSEMBLE_TO_MEMBER를 한 번 더 읽어야 하고,
    // sourcePartyId는 이미 idx_goal_source_party가 걸려 있어 여기로 바로 찾는 편이 싸다.
    @Query("select g from Goal g where g.sourcePartyId = :sourcePartyId and g.type = :type")
    List<Goal> findAllBySourcePartyIdAndType(
            @Param("sourcePartyId") Long sourcePartyId,
            @Param("type") GoalType type
    );

    /**
     * 이 개인 TODO에 연결된 성취. 없으면 비어 있다.
     *
     * FK가 성취 쪽에 있어서 TODO는 자기가 연결됐는지 모르기 때문에 확인 단계
     * - TODO 삭제 전: 연결된 성취를 찾아 null로 변경
     */
    @Query("select c from PersonalChecklist c where c.personalTodo.id = :personalTodoId")
    Optional<PersonalChecklist> findChecklistByPersonalTodoId(@Param("personalTodoId") Long personalTodoId);

    // 좋아요 카운터는 Party.likeCount와 동일하게 동시 요청 시 Lost Update를 막기 위해 원자적 UPDATE로 처리
    // flushAutomatically=true는 좋아요 저장/삭제가 먼저 flush되게 하고
    // clearAutomatically=true는 이후 조회가 최신값을 읽게 함
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Goal g set g.likeCount = g.likeCount + 1 where g.id = :id")
    void increaseLikeCount(@Param("id") long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Goal g set g.likeCount = case when g.likeCount > 0 then g.likeCount - 1 else 0 end where g.id = :id")
    void decreaseLikeCount(@Param("id") long id);

    // 지원자 카드의 성취 건수. 화면이 건수만 쓰므로 Goal을 엔티티로 읽지 않는다 -
    // JOINED 상속이라 엔티티 조회는 자식 테이블 3개를 조인한다.
    // 지원자마다 세면 카드 수만큼 쿼리가 나가므로 소유자 id를 모아 한 번에 집계한다.
    @Query("""
            select new com.back.domain.goal.goal.dtos.OwnerAchievementCount(
                g.owner.id,
                sum(case when g.source = com.back.domain.goal.goal.entity.GoalSource.PLATFORM_VERIFIED then 1L else 0L end),
                sum(case when g.source = com.back.domain.goal.goal.entity.GoalSource.SELF_REPORTED then 1L else 0L end))
            from Goal g
            where g.owner.id in :ownerIds
            group by g.owner.id
            """)
    List<OwnerAchievementCount> countAchievementsByOwnerIdIn(@Param("ownerIds") Collection<Long> ownerIds);

    // 마이페이지 요약의 '수상' 건수. idx_goal_owner를 탄다.
    @Query("select count(g) from Goal g where g.owner = :owner and g.type = :type and g.status = :status")
    long countByOwnerAndTypeAndStatus(
            @Param("owner") Member owner,
            @Param("type") GoalType type,
            @Param("status") GoalStatus status
    );

    // 북마크함 카드 조립용 - 같은 전시글(PARTY_SHOWCASE)에 파티원 수만큼 Project가 존재해 대표 1건만 필요하다
    // id가 가장 작은 것을 대표로 고정한다
    @Query("""
        select p from Project p
        join fetch p.partyShowcase ps
        join fetch ps.party
        where p.id in (
            select min(p2.id) from Project p2
            where p2.partyShowcase.id in :showcaseIds
            group by p2.partyShowcase.id
        )
        """)
    List<Project> findRepresentativeProjectsByShowcaseIds(@Param("showcaseIds") Collection<Long> showcaseIds);
}
