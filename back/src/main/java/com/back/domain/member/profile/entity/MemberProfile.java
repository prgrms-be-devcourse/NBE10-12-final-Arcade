package com.back.domain.member.profile.entity;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Getter
@NoArgsConstructor
public class MemberProfile extends BaseEntity {

    @OneToOne
    private Member member;
    @Column(unique = true)
    private String nickname;
    private String webPage;

    @OneToMany(mappedBy = "memberProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<MemberProfilePosition> positions = new ArrayList<>();

    @OneToMany(mappedBy = "memberProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<MemberProfileTechStack> techStacks = new ArrayList<>();

    public MemberProfile(Member member) {
        this.member = member;
        this.nickname = null;
        this.webPage = null;
    }

    public MemberProfile(Member member, String nickname, String webPage, List<String> positionTypes, List<String> techStacks) {
        this.member = member;
        this.nickname = nickname;
        this.webPage = webPage;
        toValidPositionTypes(positionTypes).forEach(positionType ->
                this.positions.add(new MemberProfilePosition(this, positionType))
        );
        techStacks.forEach(techStack -> this.techStacks.add(new MemberProfileTechStack(this, techStack)));
    }

    public void modify(
            String nickname,
            String webpage,
            String profileImageUrl,
            List<String> positions,
            List<String> techStacks
    ) {
        this.nickname = nickname;
        this.webPage = webpage;
        member.setProfileImgUrl(profileImageUrl);

        Set<PositionType> requestedPositions = toValidPositionTypes(positions);

        this.positions.removeIf(position ->
                !requestedPositions.contains(position.getPositionType()));

        Set<PositionType> existingPositions = this.positions.stream()
                .map(MemberProfilePosition::getPositionType)
                .collect(Collectors.toSet());

        requestedPositions.stream()
                .filter(position -> !existingPositions.contains(position))
                .forEach(position -> this.positions.add(new MemberProfilePosition(this, position)));

        Set<String> requestedTechStacks = new LinkedHashSet<>(techStacks);

        // orphanRemoval에 의해 요청에서 빠진 항목만 삭제된다.
        this.techStacks.removeIf(techStack ->
                !requestedTechStacks.contains(techStack.getTechStack()));

        Set<String> existingTechStacks = this.techStacks.stream()
                .map(MemberProfileTechStack::getTechStack)
                .collect(Collectors.toSet());

        requestedTechStacks.stream()
                .filter(techStack -> !existingTechStacks.contains(techStack))
                .forEach(techStack -> this.techStacks.add(new MemberProfileTechStack(this, techStack)));

    }

    private Set<PositionType> toValidPositionTypes(List<String> positionTypes) {
        return positionTypes.stream()
                .flatMap(positionType -> {
                    if (positionType == null) {
                        return Stream.empty();
                    }

                    try {
                        return Stream.of(PositionType.valueOf(positionType));
                    } catch (IllegalArgumentException e) {
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
