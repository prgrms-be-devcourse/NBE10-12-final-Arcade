package com.back.domain.goal.goal.repository;

import com.back.domain.goal.goal.entity.Goal;
import com.back.domain.goal.goal.entity.GoalType;
import com.back.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    // 파티 확정 이벤트가 중복 수신돼도 같은 사람에게 같은 파티의 성취가 두 번 생기지 않게 막는다.
    // GOAL.party_assemble_to_member_id UK와 같은 목적이지만, 그 값이 이벤트에 아직 실려오지 않아 이쪽으로 먼저 방어한다.
    boolean existsByOwnerAndSourcePartyIdAndType(Member owner, Long sourcePartyId, GoalType type);
}
