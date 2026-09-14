package com.back.domain.search.search.repository;

import com.back.domain.member.member.entity.Member;
import com.back.domain.search.search.entity.SearchLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {
    // 파티 추천 후보 선정용 - 최근 30일 이내 검색어 중 최신순 상위 N개(호출 측에서 Pageable로 개수 제한).
    List<SearchLog> findByMemberAndCreateDateAfterOrderByCreateDateDesc(
            Member member, LocalDateTime since, Pageable pageable
    );
}
