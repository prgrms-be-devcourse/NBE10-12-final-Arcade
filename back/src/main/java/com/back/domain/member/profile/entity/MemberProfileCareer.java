package com.back.domain.member.profile.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// 프로필의 경력 한 줄. 수정 요청이 목록 전체를 보내오므로 통째로 교체된다.
@Entity
@Getter
@NoArgsConstructor
public class MemberProfileCareer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_profile_id", nullable = false)
    private MemberProfile memberProfile;

    private LocalDate startDate;

    // null 이면 재직중이다. '2024.03 ~ 재직중' 같은 표기는 화면이 만든다.
    private LocalDate endDate;

    // 직함이 아니라 역할이다. '백엔드 엔지니어'
    @Column(nullable = false)
    private String role;

    private String org;

    @Column(length = 1000)
    private String description;

    MemberProfileCareer(
            MemberProfile memberProfile,
            LocalDate startDate,
            LocalDate endDate,
            String role,
            String org,
            String description
    ) {
        this.memberProfile = memberProfile;
        this.startDate = startDate;
        this.endDate = endDate;
        this.role = role;
        this.org = org;
        this.description = description;
    }

    public boolean isWorking() {
        return endDate == null;
    }
}
