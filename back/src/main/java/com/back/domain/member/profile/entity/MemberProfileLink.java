package com.back.domain.member.profile.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 프로필의 외부 링크 한 줄. 경력과 마찬가지로 목록 전체가 통째로 교체된다.
@Entity
@Getter
@NoArgsConstructor
public class MemberProfileLink extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_profile_id", nullable = false)
    private MemberProfile memberProfile;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false, length = 2048)
    private String url;

    MemberProfileLink(MemberProfile memberProfile, String label, String url) {
        this.memberProfile = memberProfile;
        this.label = label;
        this.url = ProfileUrl.normalize(url);
    }
}
