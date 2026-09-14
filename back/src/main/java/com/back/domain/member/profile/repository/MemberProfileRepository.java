package com.back.domain.member.profile.repository;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.profile.entity.MemberProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface MemberProfileRepository extends JpaRepository<MemberProfile, Integer> {
    Optional<MemberProfile> findByMember(Member member);

    // 지원자 카드가 기술 스택까지 그리므로 함께 당겨온다 - 없으면 프로필 수만큼 추가 쿼리가 나간다.
    @EntityGraph(attributePaths = "techStacks")
    List<MemberProfile> findByMember_IdIn(Collection<Long> memberIds);

    boolean existsByMember(Member member);

    // 배치에서 전체 프로필을 청크 단위로 순회할 때, 엔티티 대신 회원 id만 페이징 조회한다.
    @Query("select mp.member.id from MemberProfile mp order by mp.member.id asc")
    Page<Long> findAllMemberIds(Pageable pageable);

    @EntityGraph(attributePaths = "techStacks")
    Optional<MemberProfile> findWithTechStacksByMember(Member member);
}
