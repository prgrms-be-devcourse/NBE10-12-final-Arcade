package com.back.global.jpa;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Querydsl 동적 쿼리용 {@link JPAQueryFactory} 빈.
 *
 * 필터가 여러 개이고 그중 일부만 넘어오는 조회는 JPQL 문자열의 {@code (:param is null or ...)} 로 쓰면
 * 조건 수만큼 식이 늘어나는 데다, 옵티마이저가 그 OR 를 인덱스로 풀지 못한다.
 * 넘어온 조건만 골라 붙이는 편이 읽기도 낫고 실행 계획도 낫다.
 */
@Configuration
public class QuerydslConfig {

    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
