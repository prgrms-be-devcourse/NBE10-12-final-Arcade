package com.back.domain.member.profile.entity;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.profile.dtos.CareerCommand;
import com.back.domain.member.profile.dtos.LinkCommand;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_member_profile_nickname", columnNames = "nickname"))
@Getter
@NoArgsConstructor
public class MemberProfile extends BaseEntity {

    @OneToOne
    @JoinColumn(unique = true)
    private Member member;
    private String nickname;
    private String webPage;

    // 자기소개. 화면의 여러 줄 입력이라 길이를 넉넉히 둔다.
    @Column(length = 1000)
    private String bio;

    /**
     * 사용자가 직접 올린 프로필 이미지. 오브젝트 스토리지 주소이고, 올리지 않았으면 null 이다.
     *
     * Member.profileImgUrl 과 따로 둔다 - 그쪽은 OAuth 가 준 아바타(GitHub avatar_url)라
     * 여기서 덮으면 소셜에서 받아온 값이 사라진다.
     * 표시할 때는 이 값이 있으면 이것을, 없으면 Member 쪽을 쓴다(MemberProfileDto).
     */
    private String profileImageUrl;

    // 지원자 목록처럼 프로필을 여러 건 읽는 화면이 붙으면 컬렉션마다 N+1 이 난다.
    // 배치로 묶으면 프로필 N 개를 읽어도 컬렉션당 쿼리 하나로 끝난다.
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "memberProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<MemberProfilePosition> positions = new ArrayList<>();

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "memberProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<MemberProfileTechStack> techStacks = new ArrayList<>();

    // 경력·링크는 포지션·기술스택과 달리 값으로 비교할 수 없어(같은 회사 두 줄이 있을 수 있다)
    // 요청이 보내온 목록으로 통째로 교체한다. 순서는 다시 넣은 순서 = id 순이다.
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "memberProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private final List<MemberProfileCareer> careers = new ArrayList<>();

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "memberProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private final List<MemberProfileLink> links = new ArrayList<>();

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
        techStacks.stream()
                .filter(Objects::nonNull)
                .forEach(techStack -> this.techStacks.add(new MemberProfileTechStack(this, techStack)));
    }

    /** 경력·링크는 넘어온 목록이 곧 저장될 목록이다. 빈 목록을 보내면 전부 지운다. */
    public void modify(
            String nickname,
            String webpage,
            String profileImageUrl,
            String bio,
            List<String> positions,
            List<String> techStacks,
            List<CareerCommand> careers,
            List<LinkCommand> links
    ) {
        this.nickname = nickname;
        this.webPage = webpage;
        this.bio = bio;
        this.profileImageUrl = profileImageUrl;

        replacePositions(positions);
        replaceTechStacks(techStacks);
        replaceCareers(careers);
        replaceLinks(links);
    }

    /**
     * 포지션은 값(enum)으로 어느 행인지 알 수 있어, 없어진 것만 지우고 새 것만 넣는다.
     * 정의되지 않은 문자열은 조용히 버린다.
     */
    private void replacePositions(List<String> positions) {
        Set<PositionType> requested = toValidPositionTypes(positions);

        // orphanRemoval 에 의해 요청에서 빠진 항목만 삭제된다.
        this.positions.removeIf(position -> !requested.contains(position.getPositionType()));

        Set<PositionType> existing = this.positions.stream()
                .map(MemberProfilePosition::getPositionType)
                .collect(Collectors.toSet());

        requested.stream()
                .filter(position -> !existing.contains(position))
                .forEach(position -> this.positions.add(new MemberProfilePosition(this, position)));
    }

    /** 기술 스택도 값으로 비교한다. 중복은 LinkedHashSet 이 걸러낸다. */
    private void replaceTechStacks(List<String> techStacks) {
        Set<String> requested = techStacks.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        this.techStacks.removeIf(techStack -> !requested.contains(techStack.getTechStack()));

        Set<String> existing = this.techStacks.stream()
                .map(MemberProfileTechStack::getTechStack)
                .collect(Collectors.toSet());

        requested.stream()
                .filter(techStack -> !existing.contains(techStack))
                .forEach(techStack -> this.techStacks.add(new MemberProfileTechStack(this, techStack)));
    }

    /** id 가 화면에서 만든 값이라 서버 행과 짝지을 수 없어 통째로 교체한다. */
    private void replaceCareers(List<CareerCommand> commands) {
        this.careers.clear();
        if (commands == null) return;

        commands.stream()
                .filter(command -> command != null && isNotBlank(command.role()))
                .forEach(command -> this.careers.add(new MemberProfileCareer(
                        this, command.startDate(), command.endDate(),
                        command.role(), command.org(), command.description())));
    }

    private void replaceLinks(List<LinkCommand> commands) {
        this.links.clear();
        if (commands == null) return;

        commands.stream()
                .filter(command -> command != null && isNotBlank(command.label()) && isNotBlank(command.url()))
                .forEach(command -> this.links.add(new MemberProfileLink(this, command.label(), command.url())));
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private Set<PositionType> toValidPositionTypes(List<String> positionTypes) {
        return positionTypes.stream()
                .filter(Objects::nonNull)
                .flatMap(positionType -> {
                    try {
                        return Stream.of(PositionType.valueOf(positionType));
                    } catch (IllegalArgumentException e) {
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
