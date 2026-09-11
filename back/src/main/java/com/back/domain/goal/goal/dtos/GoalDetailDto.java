package com.back.domain.goal.goal.dtos;

import com.back.domain.goal.goal.entity.EvidenceStatus;
import com.back.domain.goal.goal.entity.Goal;
import com.back.domain.goal.goal.entity.PersonalChecklist;
import com.back.domain.goal.goal.entity.PersonalContest;
import com.back.domain.goal.goal.entity.Project;
import com.back.domain.member.member.entity.PositionType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

// 타입별 세부 필드를 하나의 detail 객체로 조립한다. 해당 타입에 없는 필드는 응답에서 빠진다.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoalDetailDto(
        // 세 타입 공용 - PROJECT 는 파티 이름, CONTEST 는 대회명, CHECKLIST 는 목표 제목
        String title,

        // PROJECT + CONTEST 공용
        String result,

        // PROJECT
        PositionType positionType,
        LocalDate startDate,
        LocalDate endDate,
        /**
         * 파티장이 전시글을 실제로 게시했는지. PROJECT 가 아니면 null 이다.
         *
         * 완료(ACHIEVED)만으로는 부족하다 - 전시를 올리지 않은 완료 파티에 '전시 페이지 보기' 를 달면
         * 빈 초안이 열린다. 판정은 `Project.isExhibited()` 하나를 쓴다(전시관 노출·좋아요와 같은 기준).
         *
         * 목록 매핑 중에 불려도 추가 쿼리가 없다 - LAZY 연관의 null 여부는 PROJECT 행이 이미 들고 있는
         * FK 컬럼으로 판정되고, 프록시는 초기화되지 않는다.
         */
        Boolean exhibited,

        // CONTEST
        Boolean isTeam,
        LocalDate awardDate,
        String contestUrl,
        Long targetContestId,
        /*
         * 증빙자료는 파일명만 내려준다.
         *
         * 스토리지 키는 싣지 않는다 - 키가 곧 파일 주소라 알려주는 순간 누구나 받아갈 수 있다.
         * 증빙에는 실명·소속이 찍혀 있고, 올린 본인은 원본을 이미 갖고 있으니 화면에서 받을 일이 없다.
         * 확인이 필요한 건 관리자뿐이라 다운로드는 관리자 API 에만 둔다.
         */
        String evidenceFileName,
        String evidenceMimeType,
        Long evidenceSize,
        /** 관리자 검수 결과. 파일이 없으면 null. 프로필의 '증빙 승인' 뱃지가 이 값을 본다 */
        EvidenceStatus evidenceStatus,
        /** 반려 사유. 본인이 볼 때만 채운다 - 남의 성취를 볼 때는 왜 반려됐는지까지 알 일이 없다 */
        String evidenceReviewNote,

        // CHECKLIST
        String memo,
        LocalDate targetDate,
        /** 연결된 개인 TODO. 항목까지 보려면 상세 응답의 todo 블록을 쓴다 */
        Long todoId
) {
    /** 누가 보든 안전한 값만 담는다. 남의 성취가 섞일 수 있는 목록·공개 프로필이 이쪽을 쓴다. */
    public static GoalDetailDto from(Goal goal) {
        return from(goal, false);
    }

    /** owner 가 true 면 본인만 볼 수 있는 값(반려 사유)까지 담는다. */
    public static GoalDetailDto from(Goal goal, boolean owner) {
        if (goal instanceof Project project) {
            return new GoalDetailDto(
                    // PROJECT 의 result 는 없앴다 - 전시글 내용은 상세 응답의 project 블록에서 온다
                    goal.getTitle(), null,
                    project.getPositionType(), project.getStartDate(), project.getEndDate(),
                    project.isExhibited(),
                    null, null, null, null, null, null, null, null, null,
                    null, null, null
            );
        }

        if (goal instanceof PersonalContest contest) {
            return new GoalDetailDto(
                    goal.getTitle(), contest.getResult(),
                    null, null, null, null,
                    contest.isTeam(), contest.getAwardDate(),
                    contest.getContestUrl(), contest.getTargetContestId(),
                    contest.getEvidenceFileName(), contest.getEvidenceMimeType(),
                    contest.getEvidenceSize(), contest.getEvidenceStatus(),
                    owner ? contest.getEvidenceReviewNote() : null,
                    null, null, null
            );
        }

        if (goal instanceof PersonalChecklist checklist) {
            return new GoalDetailDto(
                    goal.getTitle(), null,
                    null, null, null, null,
                    null, null, null, null, null, null, null, null, null,
                    checklist.getMemo(), checklist.getTargetDate(),
                    checklist.getPersonalTodo() == null ? null : checklist.getPersonalTodo().getId()
            );
        }

        throw new IllegalStateException("알 수 없는 성취 타입입니다: " + goal.getClass());
    }
}
