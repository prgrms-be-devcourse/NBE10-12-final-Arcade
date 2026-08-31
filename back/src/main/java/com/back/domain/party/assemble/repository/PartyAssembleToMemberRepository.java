package com.back.domain.party.assemble.repository;

import com.back.domain.party.assemble.entity.PartyAssembleToMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartyAssembleToMemberRepository extends JpaRepository<PartyAssembleToMember, Long> {
}
