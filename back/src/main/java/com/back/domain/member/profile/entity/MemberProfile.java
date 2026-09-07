package com.back.domain.member.profile.entity;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.profile.dtos.CareerCommand;
import com.back.domain.member.profile.dtos.LinkCommand;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    private String githubUsername;

    // 대표 포지션 하나다. 화면도 select 로 하나만 고르게 돼 있어 목록으로 둘 이유가 없다.
    @Enumerated(EnumType.STRING)
    private PositionType position;

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

    public MemberProfile(Member member, String nickname, String webPage, PositionType position, List<String> techStacks) {
        this.member = member;
        this.nickname = nickname;
        this.webPage = ProfileUrl.normalize(webPage);
        this.position = position;
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
            String githubUsername,
            PositionType position,
            List<String> techStacks,
            List<CareerCommand> careers,
            List<LinkCommand> links
    ) {
        this.nickname = nickname;
        this.webPage = ProfileUrl.normalize(webpage);
        this.bio = bio;
        this.githubUsername = githubUsername;
        this.profileImageUrl = profileImageUrl;

        this.position = position;
        replaceTechStacks(techStacks);
        replaceCareers(careers);
        replaceLinks(links);
    }


    /** 기술 스택도 값으로 비교한다. 중복은 LinkedHashSet 이 걸러낸다. */
    private void replaceTechStacks(List<String> techStacks) {
        Set<String> requested = techStacks == null
                ? Set.of()
                : techStacks.stream()
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

}
