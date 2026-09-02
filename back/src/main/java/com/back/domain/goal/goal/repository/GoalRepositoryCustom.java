package com.back.domain.goal.goal.repository;

import com.back.domain.goal.goal.entity.Goal;
import com.back.domain.goal.goal.entity.GoalSource;
import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.GoalType;
import com.back.domain.member.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GoalRepositoryCustom {

    /**
     * 내 성취 검색. 다섯 필터 모두 선택이며, 넘기지 않은 조건은 쿼리에서 아예 빠진다.
     *
     * @param year    성취의 대표 연도. 어떤 날짜를 기준으로 삼는지는 {@link GoalRepositoryImpl} 참조
     * @param keyword 제목/대회명·결과/메모에 대한 부분 일치(대소문자 무시)
     * @param source 플랫폼 자동기록/자가신고
     */
    Page<Goal> searchMyGoals(
            Member owner,
            GoalStatus status,
            GoalType type,
            GoalSource source,
            Integer year,
            String keyword,
            Pageable pageable
    );
}
