package com.back.domain.goal.goal.repository;

import com.back.domain.goal.goal.entity.Goal;
import com.back.domain.goal.goal.entity.GoalSource;
import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.GoalType;
import com.back.domain.member.member.entity.Member;
import java.util.List;

public interface GoalRepositoryCustom {

    /**
     * 내 성취 검색. 다섯 필터 모두 선택이며, 넘기지 않은 조건은 쿼리에서 아예 빠진다.
     *
     * 페이징하지 않고 전부 돌려준다 - 연혁은 이력 전체를 연도별로 묶어 보여주는 화면이라
     * 끊어서 주면 연도 그룹과 연도 선택지가 잘린다.
     *
     * @param year    성취의 대표 연도. 어떤 날짜를 기준으로 삼는지는 {@link GoalRepositoryImpl} 참조
     * @param keyword 제목/대회명·결과/메모에 대한 부분 일치(대소문자 무시)
     * @param source 플랫폼 자동기록/자가신고
     */
    List<Goal> searchMyGoals(
            Member owner,
            GoalStatus status,
            GoalType type,
            GoalSource source,
            Integer year,
            String keyword
    );
}
