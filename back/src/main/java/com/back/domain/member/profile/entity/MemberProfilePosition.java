package com.back.domain.member.profile.entity;

import com.back.domain.member.member.entity.PositionType;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class MemberProfilePosition extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_profile_id", nullable = false)
    private MemberProfile memberProfile;

    @Enumerated(EnumType.STRING)
    private PositionType positionType;

    public MemberProfilePosition(MemberProfile memberProfile, PositionType positionType) {
        this.memberProfile = memberProfile;
        this.positionType = positionType;
    }
}
