package com.back.domain.member.profile.repository;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.profile.entity.MemberProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface MemberProfileRepository extends JpaRepository<MemberProfile, Integer> {
    Optional<MemberProfile> findByMember(Member member);

    // 지원자 카드가 기술 스택까지 그리므로 함께 당겨온다 - 없으면 프로필 수만큼 추가 쿼리가 나간다.
    @EntityGraph(attributePaths = "techStacks")
    List<MemberProfile> findByMember_IdIn(Collection<Long> memberIds);
}
