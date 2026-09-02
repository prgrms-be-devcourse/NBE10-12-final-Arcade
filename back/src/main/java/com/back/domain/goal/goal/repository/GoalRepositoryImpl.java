package com.back.domain.goal.goal.repository;

import com.back.domain.goal.goal.entity.Goal;
import com.back.domain.goal.goal.entity.GoalSource;
import com.back.domain.goal.goal.entity.GoalStatus;
import com.back.domain.goal.goal.entity.GoalType;
import com.back.domain.goal.goal.entity.QGoal;
import com.back.domain.goal.goal.entity.QPersonalChecklist;
import com.back.domain.goal.goal.entity.QPersonalContest;
import com.back.domain.goal.goal.entity.QProject;
import com.back.domain.member.member.entity.Member;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class GoalRepositoryImpl implements GoalRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QGoal goal = QGoal.goal;
    private static final QProject project = QProject.project;
    private static final QPersonalContest contest = QPersonalContest.personalContest;
    private static final QPersonalChecklist checklist = QPersonalChecklist.personalChecklist;

    @Override
    public List<Goal> searchMyGoals(
            Member owner,
            GoalStatus status,
            GoalType type,
            GoalSource source,
            Integer year,
            String keyword
    ) {
        BooleanBuilder where = new BooleanBuilder()
                .and(goal.owner.eq(owner))
                .and(eqStatus(status))
                .and(eqType(type))
                .and(eqSource(source))
                .and(eqYear(year))
                .and(containsKeyword(keyword));

        // 같은 시각에 만들어진 행끼리 순서가 흔들리지 않게 id 를 보조키로 둔다.
        return withSubTypes(queryFactory.selectFrom(goal))
                .where(where)
                .orderBy(goal.createDate.desc(), goal.id.desc())
                .fetch();
    }

    private <T> JPAQuery<T> withSubTypes(JPAQuery<T> query) {
        return query
                .leftJoin(project).on(project.id.eq(goal.id))
                .leftJoin(contest).on(contest.id.eq(goal.id))
                .leftJoin(checklist).on(checklist.id.eq(goal.id));
    }

    private BooleanExpression eqStatus(GoalStatus status) {
        return status == null ? null : goal.status.eq(status);
    }

    private BooleanExpression eqType(GoalType type) {
        return type == null ? null : goal.type.eq(type);
    }

    private BooleanExpression eqSource(GoalSource source) {
        return source == null ? null : goal.source.eq(source);
    }

    /**
     * 성취의 대표 연도.
     *
     * 타입마다 기준이 되는 날짜가 다르고(참여 시작 · 수상 · 목표일), 아직 날짜가 없는 성취도 있어서
     * 처음 잡히는 값을 쓰고 마지막에는 등록일로 떨어진다.
     * 마이페이지 연혁이 연도별로 묶을 때 쓰는 기준과 같아야 해서
     * (front/lib/api/goals.ts 의 startDate ?? awardDate ?? targetDate ?? createDate)
     * 한쪽을 바꾸면 다른 쪽도 같이 바꿔야 한다.
     */
    private BooleanExpression eqYear(Integer year) {
        if (year == null) return null;

        NumberExpression<Integer> referenceYear = project.startDate.year()
                .coalesce(contest.awardDate.year())
                .coalesce(checklist.targetDate.year())
                .coalesce(goal.createDate.year());

        return referenceYear.eq(year);
    }

    /**
     * 제목·대회명·결과·메모에 대한 부분 일치.
     *
     * 마이페이지 성취 카드가 보여주는 두 줄(제목 = title ?? contestName, 설명 = result ?? memo)이
     * 검색 대상과 같도록 맞췄다. 화면에 보이는 글자로 찾을 수 있어야 하기 때문이다.
     */
    private BooleanExpression containsKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;

        String trimmed = keyword.trim();

        return project.title.containsIgnoreCase(trimmed)
                .or(project.result.containsIgnoreCase(trimmed))
                .or(contest.contestName.containsIgnoreCase(trimmed))
                .or(contest.result.containsIgnoreCase(trimmed))
                .or(checklist.title.containsIgnoreCase(trimmed))
                .or(checklist.memo.containsIgnoreCase(trimmed));
    }
}
