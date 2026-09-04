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
        this.url = normalizeUrl(url);
    }

    /**
     * 스킴이 없으면 https 를 붙인다.
     * 없는 채로 두면 <a href="github.com/x"> 가 상대 경로로 해석돼 링크가 열리지 않는다.
     * 그 밖에는 사용자가 적은 값을 그대로 둔다 - 중복을 막지 않으므로 더 다듬을 이유가 없다.
     */
    private static String normalizeUrl(String url) {
        String trimmed = url.trim();

        return trimmed.matches("(?i)^[a-z][a-z0-9+.-]*://.*") ? trimmed : "https://" + trimmed;
    }
}
