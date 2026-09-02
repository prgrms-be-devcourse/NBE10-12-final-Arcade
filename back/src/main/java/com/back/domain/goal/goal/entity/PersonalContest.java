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

    // 크루온 밖 대회의 원본 공고·결과 발표 페이지 주소.
    // 자기신고 성취는 대회 허브에 없는 외부 대회를 적는 것이라(targetContestId가 항상 null),
    // 나중에 이 기록을 확인할 근거가 링크뿐이다.
    private String contestUrl;

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
            LocalDate awardDate,
            String contestUrl
    ) {
        super(owner, GoalType.CONTEST, status, GoalSource.SELF_REPORTED, null, null);
        this.contestName = contestName;
        this.isTeam = isTeam;
        this.result = result;
        this.awardDate = awardDate;
        this.contestUrl = contestUrl;
    }

    // 수정은 타입의 필드를 통째로 덮어쓴다. 화면이 항상 그 타입의 detail 전체를 보내오기 때문에,
    // 넘어오지 않은 값은 "그대로 두기"가 아니라 "비우기"다.
    public void update(String contestName, boolean isTeam, String result, LocalDate awardDate, String contestUrl) {
        this.contestName = contestName;
        this.isTeam = isTeam;
        this.result = result;
        this.awardDate = awardDate;
        this.contestUrl = contestUrl;
    }

    /**
     * 증빙 파일의 메타데이터만 갱신한다.
     *
     * 파일 업로드 API가 아직 없어(작업표 미정) 화면이 보낼 수 있는 건 파일명·형식·크기뿐이다.
     * storageKey는 실제 업로드가 붙을 때 attachEvidence()로 채우는 값이라 여기서 건드리지 않는다.
     */
    public void updateEvidenceMetadata(String fileName, String mimeType, Long size) {
        this.evidenceFileName = fileName;
        this.evidenceMimeType = mimeType;
        this.evidenceSize = size;
    }

    public void attachEvidence(String storageKey, String fileName, String mimeType, Long size) {
        this.evidenceStorageKey = storageKey;
        this.evidenceFileName = fileName;
        this.evidenceMimeType = mimeType;
        this.evidenceSize = size;
    }
}
