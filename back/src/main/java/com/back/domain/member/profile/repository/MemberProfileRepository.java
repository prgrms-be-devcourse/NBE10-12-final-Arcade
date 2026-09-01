package com.back.domain.member.profile.repository;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.profile.entity.MemberProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface MemberProfileRepository extends JpaRepository<MemberProfile, Integer> {
    Optional<MemberProfile> findByMember(Member member);

    List<MemberProfile> findByMember_IdIn(Collection<Long> memberIds);
}
