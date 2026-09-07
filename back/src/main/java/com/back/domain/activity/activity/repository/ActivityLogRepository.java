package com.back.domain.activity.activity.repository;

import com.back.domain.activity.activity.entity.ActivityLog;
import com.back.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    Optional<ActivityLog> findByMemberAndActivityDate(Member member, LocalDate activityDate);

    // 스트릭과 히트맵이 같은 조회를 쓴다. 임계값 최대가 200일이라 그만큼만 읽으면 충분하다.
    List<ActivityLog> findAllByMemberAndActivityDateGreaterThanEqual(Member member, LocalDate from);

    boolean existsBy();
}
