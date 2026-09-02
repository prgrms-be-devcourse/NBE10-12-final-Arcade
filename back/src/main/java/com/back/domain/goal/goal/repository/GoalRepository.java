package com.back.domain.goal.goal.repository;

import com.back.domain.goal.goal.entity.Goal;
import com.back.domain.goal.goal.entity.GoalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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

}
