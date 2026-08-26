package com.back.domain.member.member.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
public class MemberProfile extends BaseEntity {

    @OneToOne
    private Member member;
    private String nickname;
    private String webPage;

    @ElementCollection
    private List<Position> positions;
    @ElementCollection
    private List<String> techStacks;


    public MemberProfile(Member member, String nickname, String webPage, List<Position> positions, List<String> techStacks) {
        this.member = member;
        this.nickname = nickname;
        this.webPage = webPage;
        this.positions = positions;
        this.techStacks = techStacks;
    }

}
