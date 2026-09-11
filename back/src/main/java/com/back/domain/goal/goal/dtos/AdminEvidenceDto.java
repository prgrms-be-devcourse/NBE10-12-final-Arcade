package com.back.domain.goal.goal.dtos;

import com.back.domain.goal.goal.entity.EvidenceStatus;
import com.back.domain.goal.goal.entity.PersonalContest;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 관리자 검수 목록의 한 줄.
 *
 * 파일 자체는 여기 없다 - 목록은 "무엇을 검수해야 하는지"만 보여주고,
 * 내용 확인은 다운로드(GET /adm/goals/{goalId}/evidence)로 한다.
 * 반려 사유는 관리자가 직접 쓴 값이라 관리자 화면에는 그대로 보여준다.
 */
public record AdminEvidenceDto(
        long goalId,
        long ownerId,
        String ownerName,
        /** 대회명 */
        String title,
        String result,
        LocalDate awardDate,
        /** 외부 대회 공고·결과 페이지. 증빙과 대조할 근거가 된다 */
        String contestUrl,
        String fileName,
        String mimeType,
        Long size,
        EvidenceStatus status,
        String reviewNote,
        /** 마지막으로 손댄 시각. 업로드 또는 검수 시점이다 */
        LocalDateTime modifyDate
) {
    public AdminEvidenceDto(PersonalContest contest) {
        this(
                contest.getId(),
                contest.getOwner().getId(),
                contest.getOwner().getName(),
                contest.getTitle(),
                contest.getResult(),
                contest.getAwardDate(),
                contest.getContestUrl(),
                contest.getEvidenceFileName(),
                contest.getEvidenceMimeType(),
                contest.getEvidenceSize(),
                contest.getEvidenceStatus(),
                contest.getEvidenceReviewNote(),
                contest.getModifyDate()
        );
    }
}
