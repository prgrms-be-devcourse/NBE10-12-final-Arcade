package com.back.domain.goal.goal.entity;

import com.back.domain.member.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// 수상·대회 성취. 결과와 증빙을 구조화해 포트폴리오에 노출한다.
// 지금은 자기신고 경로만 구현한다 - 대회 수상 자동생성(PLATFORM_VERIFIED) 흐름은 팀 재논의 후 붙인다.
@Entity
@Getter
@NoArgsConstructor
@PrimaryKeyJoinColumn(name = "goal_id")
public class PersonalContest extends Goal {

    @Column(nullable = false)
    private String contestName;

    @Column(nullable = false)
    private boolean isTeam;

    private String result;

    // 수상 시점. 프로필 성취 리스트의 기간 표기·연도 그룹핑 기준이 된다(2.11).
    // 아직 나가지 않은 대회를 WANT로 등록할 수도 있어 nullable로 둔다.
    private LocalDate awardDate;

    // 증빙자료는 Object Storage에 두고 여기엔 메타데이터만 남긴다
    private String evidenceStorageKey;
    private String evidenceFileName;
    private String evidenceMimeType;
    private Long evidenceSize;

    // 대회 허브에 등록된 CONTEST를 가리킨다. 자기신고는 연결할 행이 없어 null이고 contestName 자유입력만 남는다.
    // 자동생성 흐름이 확정되면 그때 값이 채워진다.
    @Column(name = "target_contest_id")
    private Long targetContestId;

    public PersonalContest(
            Member owner,
            GoalStatus status,
            String contestName,
            boolean isTeam,
            String result,
            LocalDate awardDate
    ) {
        super(owner, GoalType.CONTEST, status, GoalSource.SELF_REPORTED, null, null);
        this.contestName = contestName;
        this.isTeam = isTeam;
        this.result = result;
        this.awardDate = awardDate;
    }

    public void update(String contestName, boolean isTeam, String result, LocalDate awardDate) {
        this.contestName = contestName;
        this.isTeam = isTeam;
        this.result = result;
        this.awardDate = awardDate;
    }

    public void attachEvidence(String storageKey, String fileName, String mimeType, Long size) {
        this.evidenceStorageKey = storageKey;
        this.evidenceFileName = fileName;
        this.evidenceMimeType = mimeType;
        this.evidenceSize = size;
    }
}
