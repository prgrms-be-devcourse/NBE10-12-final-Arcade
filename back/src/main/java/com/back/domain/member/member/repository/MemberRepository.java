package com.back.domain.member.member.repository;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.Role;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);

    Optional<Member> findByApiKey(String apiKey);
    Optional<Member> findByGithubProviderUserId(String githubProviderUserId);

    Optional<Member> findByGithubUserId(Long githubUserId);

    // 프로필 최초 생성을 한 줄로 세울 때만 쓴다(MemberProfileService.getOrCreateProfile).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :id")
    Optional<Member> findByIdForUpdate(@Param("id") long id);

    // 관리자 목록 - keyword는 이메일/이름 부분일치, role/active는 null이면 전체
    @Query("""
        select m from Member m
        where (:keyword is null or m.email like concat('%', cast(:keyword as string), '%') or m.name like concat('%', cast(:keyword as string), '%'))
          and (:role is null or m.role = :role)
          and (:active is null or m.active = :active)
        """)
    Page<Member> searchForAdmin(
            @Param("keyword") String keyword,
            @Param("role") Role role,
            @Param("active") Boolean active,
            Pageable pageable
    );
}
