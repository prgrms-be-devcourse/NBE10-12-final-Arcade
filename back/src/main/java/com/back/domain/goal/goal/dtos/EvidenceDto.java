package com.back.domain.goal.goal.dtos;

import com.back.domain.goal.goal.entity.EvidenceStatus;
import com.back.domain.goal.goal.entity.PersonalContest;

/** 증빙 업로드 결과. 파일 자체는 내려주지 않는다 - 저장된 메타데이터와 검수 상태만 알린다. */
public record EvidenceDto(
        String fileName,
        String mimeType,
        Long size,
        EvidenceStatus status
) {
    public EvidenceDto(PersonalContest contest) {
        this(
                contest.getEvidenceFileName(),
                contest.getEvidenceMimeType(),
                contest.getEvidenceSize(),
                contest.getEvidenceStatus()
        );
    }
}
