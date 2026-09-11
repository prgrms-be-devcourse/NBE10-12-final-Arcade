package com.back.domain.goal.goal.entity;

import com.back.domain.goal.goal.dtos.GoalDetailDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 증빙의 노출 규칙과 검수 상태.
 *
 * 스프링을 띄우지 않는다 - 소유자는 판정에 쓰이지 않아 owner 없이 만든 엔티티로 충분하다.
 */
class PersonalContestEvidenceTest {

    private PersonalContest contest() {
        return new PersonalContest(
                null, GoalStatus.ACHIEVED, "전국 대학생 해커톤", false,
                "장려상", LocalDate.of(2023, 11, 15), "https://example.com/contest");
    }

    @Test
    @DisplayName("증빙을 올리면 검수 대기(PENDING)로 시작한다")
    void uploadStartsPending() {
        PersonalContest contest = contest();

        assertThat(GoalDetailDto.from(contest, true).evidenceStatus()).isNull();

        contest.attachEvidence("goal-evidence/uuid-1", "수상확인서.pdf", "application/pdf", 204800L);

        assertThat(contest.getEvidenceStatus()).isEqualTo(EvidenceStatus.PENDING);
    }

    @Test
    @DisplayName("반려 사유는 본인에게만 내려간다 - 상태와 파일명은 공개다")
    void reviewNoteIsOwnerOnly() {
        PersonalContest contest = contest();
        contest.attachEvidence("goal-evidence/uuid-1", "수상확인서.pdf", "application/pdf", 204800L);
        contest.review(EvidenceStatus.REJECTED, "확인서가 흐릿해서 대회명이 안 보여요");

        GoalDetailDto mine = GoalDetailDto.from(contest, true);
        GoalDetailDto theirs = GoalDetailDto.from(contest, false);

        assertThat(mine.evidenceReviewNote()).isEqualTo("확인서가 흐릿해서 대회명이 안 보여요");
        assertThat(theirs.evidenceReviewNote()).isNull();

        // 파일명과 검수 상태는 누가 봐도 보인다 - 프로필의 '증빙 승인' 뱃지가 상태를 쓴다
        assertThat(theirs.evidenceFileName()).isEqualTo("수상확인서.pdf");
        assertThat(theirs.evidenceStatus()).isEqualTo(EvidenceStatus.REJECTED);
    }

    @Test
    @DisplayName("승인 뒤 파일을 바꾸면 검수가 처음으로 돌아간다")
    void reuploadResetsReview() {
        PersonalContest contest = contest();
        contest.attachEvidence("goal-evidence/uuid-1", "수상확인서.pdf", "application/pdf", 204800L);
        contest.review(EvidenceStatus.APPROVED, null);

        contest.attachEvidence("goal-evidence/uuid-2", "다른파일.pdf", "application/pdf", 100L);

        // 안 그러면 승인 뱃지를 단 채로 아무 문서나 들고 있을 수 있다
        assertThat(contest.getEvidenceStatus()).isEqualTo(EvidenceStatus.PENDING);
        assertThat(contest.getEvidenceReviewNote()).isNull();
    }
}
