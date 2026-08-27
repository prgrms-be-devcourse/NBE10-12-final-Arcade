package com.back.domain.member.member.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class MemberProfile extends BaseEntity {

    @OneToOne
    private Member member;
    private String nickname;
    private String webPage;

    @OneToMany(mappedBy = "memberProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<MemberProfilePosition> positions = new ArrayList<>();

    @OneToMany(mappedBy = "memberProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<MemberProfileTechStack> techStacks = new ArrayList<>();


    public MemberProfile(Member member, String nickname, String webPage, List<Position> positions, List<String> techStacks) {
        this.member = member;
        this.nickname = nickname;
        this.webPage = webPage;
        positions.forEach(position -> this.positions.add(new MemberProfilePosition(this, position)));
        techStacks.forEach(techStack -> this.techStacks.add(new MemberProfileTechStack(this, techStack)));
    }

}
