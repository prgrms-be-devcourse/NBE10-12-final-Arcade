package com.back.domain.member.member.service;

import com.back.domain.member.member.dtos.MemberDetailDto;
import com.back.domain.member.member.dtos.MemberListItemDto;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.entity.PositionType;
import com.back.domain.member.member.entity.Role;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.profile.entity.MemberProfile;
import com.back.domain.member.profile.repository.MemberProfileRepository;
import com.back.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class MemberServiceAdminTest {

    @Autowired
    private MemberService memberService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private MemberProfileRepository memberProfileRepository;

    private Member target;

    @BeforeEach
    void setUp() {
        target = memberRepository.save(new Member("adm-member-target@test.com", "pw", "홍길동", null));
        memberProfileRepository.save(new MemberProfile(target, "길동이", null, PositionType.BACK, List.of("Java", "Spring")));
    }

    @Test
    void getListForAdmin_returnsMatchingMembers() {
        Page<MemberListItemDto> result = memberService.getListForAdmin(
                "adm-member-target", null, null, PageRequest.of(0, 20)
        );

        assertThat(result.getContent())
                .extracting(MemberListItemDto::email)
                .contains("adm-member-target@test.com");
    }

    @Test
    void getListForAdmin_filtersByActive() {
        memberService.updateStatus(target.getId(), false, "테스트 정지");

        Page<MemberListItemDto> activeOnly = memberService.getListForAdmin(
                null, null, true, PageRequest.of(0, 100)
        );

        assertThat(activeOnly.getContent())
                .extracting(MemberListItemDto::id)
                .doesNotContain(target.getId());
    }

    @Test
    void getDetailForAdmin_includesProfileFields() {
        MemberDetailDto detail = memberService.getDetailForAdmin(target.getId());

        assertThat(detail.nickname()).isEqualTo("길동이");
        assertThat(detail.position()).isEqualTo(PositionType.BACK);
        assertThat(detail.techStacks()).containsExactly("Java", "Spring");
    }

    @Test
    void updateStatus_suspendsAndStoresReason() {
        memberService.updateStatus(target.getId(), false, "부적절한 활동");

        Member updated = memberRepository.findById(target.getId()).orElseThrow();
        assertThat(updated.isActive()).isFalse();
        assertThat(updated.getSuspendReason()).isEqualTo("부적절한 활동");
    }

    @Test
    void updateStatus_activateClearsReason() {
        memberService.updateStatus(target.getId(), false, "부적절한 활동");
        memberService.updateStatus(target.getId(), true, null);

        Member updated = memberRepository.findById(target.getId()).orElseThrow();
        assertThat(updated.isActive()).isTrue();
        assertThat(updated.getSuspendReason()).isNull();
    }

    @Test
    void suspendedMemberCannotLogin() {
        // login()은 인코딩된 비밀번호를 요구하므로 join()을 통해 만든다
        memberService.join("adm-member-login@test.com", "1234", "로그인테스트");
        Member loginTarget = memberRepository.findByEmail("adm-member-login@test.com").orElseThrow();
        memberService.updateStatus(loginTarget.getId(), false, "정지 테스트");

        assertThatThrownBy(() -> memberService.login("adm-member-login@test.com", "1234"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("403-2");
    }

    @Test
    void updateStatus_adminCannotBeSuspended() {
        Member admin = memberRepository.findByEmail("admin").orElseThrow();

        assertThatThrownBy(() -> memberService.updateStatus(admin.getId(), false, "테스트"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("409-2");
    }
}
