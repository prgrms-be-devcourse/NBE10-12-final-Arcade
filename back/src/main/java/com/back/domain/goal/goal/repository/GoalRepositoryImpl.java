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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

@RequiredArgsConstructor
public class GoalRepositoryImpl implements GoalRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QGoal goal = QGoal.goal;
    private static final QProject project = QProject.project;
    private static final QPersonalContest contest = QPersonalContest.personalContest;
    private static final QPersonalChecklist checklist = QPersonalChecklist.personalChecklist;

    @Override
    public Page<Goal> searchMyGoals(
            Member owner,
            GoalStatus status,
            GoalType type,
            GoalSource source,
            Integer year,
            String keyword,
            Pageable pageable
    ) {
        BooleanBuilder where = new BooleanBuilder()
                .and(goal.owner.eq(owner))
                .and(eqStatus(status))
                .and(eqType(type))
                .and(eqSource(source))
                .and(eqYear(year))
                .and(containsKeyword(keyword));

        List<Goal> content = withSubTypes(queryFactory.selectFrom(goal))
                .where(where)
                .orderBy(goal.createDate.desc(), goal.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // count 는 자식 테이블을 읽을 필요가 없지만, year·keyword 조건이 자식 컬럼을 보므로 조인은 그대로 둔다.
        JPAQuery<Long> countQuery = withSubTypes(queryFactory.select(goal.count()).from(goal))
                .where(where);

        // 마지막 페이지이거나 첫 페이지가 다 안 찼으면 count 쿼리를 아예 날리지 않는다.
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 자식 테이블을 조건절에서 가리킬 수 있게 명시적으로 붙인다.
     *
     * Goal 은 JOINED 상속이라 Goal 만 조회해도 하이버네이트가 자식 테이블을 함께 읽지만,
     * 그건 조회 결과를 조립하기 위한 조인이라 조건절에서 쓸 이름이 없다.
     *
     * 그래서 실제 SQL 에는 자식 테이블 조인이 두 벌(조립용 3 + 조건용 3) 나간다.
     * 전부 PK 동등 조인이고 성취는 회원당 수십 건 규모라 비용은 크지 않다.
     *
     * 원래는 treat() 로 조립용 조인을 그대로 쓰는 게 맞지만 지금 스택에서는 쓸 수 없다 -
     * Querydsl 5.1.0 의 as() 다운캐스트는 HQL treat() 을 만들지 않고 부모 경로를 그대로 내보내서,
     * Project 와 PersonalChecklist 에 모두 있는 title 에서 "declared in multiple subtypes" 로 깨진다.
     * Querydsl 이 treat() 을 제대로 내보내게 되면 이 헬퍼를 지우고 다운캐스트로 바꾸면 된다.
     */
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
