package com.back.domain.goal.goal.repository;

import com.back.domain.goal.goal.entity.Goal;
import com.back.domain.goal.goal.entity.GoalType;
import com.back.domain.goal.goal.entity.PersonalChecklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * FK가 성취 쪽에 있어서 TODO는 자기가 연결됐는지 모른다. 두 곳에서 이 조회가 필요하다.
     * - TODO 삭제 전: 연결된 성취를 찾아 detachTodo()로 먼저 떼어내야 한다
     * - 성취 등록 시: personal_todo_id가 UNIQUE인데 JOINED + IDENTITY 채번이라 제약 위반이 save() 시점에
     *   DataIntegrityViolationException으로 터진다. 메시지를 다듬으려면 저장 전에 미리 걸러야 한다
     */
    @Query("select c from PersonalChecklist c where c.personalTodo.id = :personalTodoId")
    Optional<PersonalChecklist> findChecklistByPersonalTodoId(@Param("personalTodoId") Long personalTodoId);

}
