'use client';

import { useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Icon } from '@/components/icons/Icon';
import { FormActions, FormGroup, FormRow, TextAreaField, TextField } from '@/components/ui/Field';
import { RadioChipGroup } from '@/components/ui/RadioChipGroup';
import { Button } from '@/components/ui/Button';
import { ApiError } from '@/lib/api';
import { updateGoal, type GoalDetailResponse, type UpdateGoalPayload } from '@/lib/api/goals';
import { GOAL_STATUS_LABELS, GOAL_STATUS_TRANSITIONS, GOAL_TYPE_LABELS } from '@/lib/constants';
import type { GoalStatus } from '@/lib/types';

const TEAM_OPTIONS = ['팀 참가', '개인 참가'] as const;

/** 화면에서 다루는 증빙 파일 한 건 */
interface EvidenceFile {
  name: string;
  type?: string;
  size?: number;
}

function formatBytes(size?: number): string {
  if (size == null) return '';
  if (size < 1024) return `${size}B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)}KB`;
  return `${(size / 1024 / 1024).toFixed(1)}MB`;
}

/**
 * 지금 상태에서 고를 수 있는 상태들.
 *
 * 서버가 전이 규칙을 강제하므로(409-2) 화면에서도 같은 규칙으로 줄여 보여준다.
 * ACHIEVED 는 종료 상태라 자기 자신만 남는다.
 */
function selectableStatuses(current: GoalStatus): GoalStatus[] {
  return [current, ...GOAL_STATUS_TRANSITIONS[current]];
}

/** yyyy-MM-dd 로 자른다. input[type=date] 가 그 형식만 받는다 */
function dateValue(value?: string): string {
  return value ? value.slice(0, 10) : '';
}

/** 빈 문자열은 보내지 않는다 — 서버에서 빈 값으로 덮어쓰지 않게 */
function trimmed(value: string): string | undefined {
  const next = value.trim();
  return next === '' ? undefined : next;
}

/**
 * 성취 수정 폼 — 자기신고 성취(CONTEST · CHECKLIST)만 다룬다.
 *
 * PROJECT 는 파티 확정 시 시스템이 만드는 타입이라 이 폼으로 오지 않는다.
 * type 은 바꿀 수 없어 읽기 전용으로만 보여준다.
 */
export function GoalEditForm({ goal }: { goal: GoalDetailResponse }) {
  const router = useRouter();
  const { detail } = goal;

  const statuses = selectableStatuses(goal.status);
  const [status, setStatus] = useState<GoalStatus>(goal.status);

  // CONTEST
  const [contestName, setContestName] = useState(detail.contestName ?? '');
  const [teamLabel, setTeamLabel] = useState<string>(detail.isTeam ? TEAM_OPTIONS[0] : TEAM_OPTIONS[1]);
  const [result, setResult] = useState(detail.result ?? '');
  const [awardDate, setAwardDate] = useState(dateValue(detail.awardDate));
  const [contestUrl, setContestUrl] = useState(detail.contestUrl ?? '');

  // CONTEST — 증빙 자료. 서버는 아직 한 건만 보관하지만 화면은 여러 건을 다룬다
  const [evidence, setEvidence] = useState<EvidenceFile[]>(
    detail.evidenceFileName ?? detail.evidenceStorageKey
      ? [
          {
            name: detail.evidenceFileName ?? detail.evidenceStorageKey!,
            type: detail.evidenceMimeType,
            size: detail.evidenceSize,
          },
        ]
      : [],
  );
  const fileInputRef = useRef<HTMLInputElement>(null);

  // CHECKLIST
  const [title, setTitle] = useState(detail.title ?? '');
  const [memo, setMemo] = useState(detail.memo ?? '');
  const [targetDate, setTargetDate] = useState(dateValue(detail.targetDate));

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitError, setSubmitError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const isContest = goal.type === 'CONTEST';

  const addFiles = (files: FileList | null) => {
    if (!files || files.length === 0) return;
    const picked = Array.from(files).map((file) => ({
      name: file.name,
      type: file.type || undefined,
      size: file.size,
    }));
    // 같은 파일을 두 번 고르면 한 번만 남긴다
    setEvidence((prev) => [
      ...prev,
      ...picked.filter((file) => !prev.some((item) => item.name === file.name)),
    ]);
  };

  // 서버가 타입별로 요구하는 필수 항목과 같은 규칙 (400-4)
  const validate = () => {
    const next: Record<string, string> = {};
    if (isContest && !contestName.trim()) next.contestName = '대회명을 입력해 주세요.';
    // 링크는 선택이지만, 적었다면 열리는 주소여야 한다
    if (isContest && contestUrl.trim() && !/^https?:\/\//i.test(contestUrl.trim())) {
      next.contestUrl = 'http:// 또는 https:// 로 시작하는 주소를 넣어주세요.';
    }
    if (!isContest && !title.trim()) next.title = '목표 제목을 입력해 주세요.';
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const submit = async () => {
    if (!validate()) return;

    const payload: UpdateGoalPayload = {
      status,
      detail: isContest
        ? {
            contestName: contestName.trim(),
            isTeam: teamLabel === TEAM_OPTIONS[0],
            result: trimmed(result),
            awardDate: trimmed(awardDate),
            contestUrl: trimmed(contestUrl) ?? null,
            // 서버는 아직 한 건만 보관한다. 목록의 첫 파일을 보내고 비었으면 지운다
            evidenceFileName: evidence[0]?.name ?? null,
            evidenceMimeType: evidence[0]?.type ?? null,
            evidenceSize: evidence[0]?.size ?? null,
          }
        : {
            title: title.trim(),
            memo: trimmed(memo),
            targetDate: trimmed(targetDate),
          },
    };

    setSubmitting(true);
    setSubmitError('');
    try {
      await updateGoal(goal.id, payload);
      router.push(`/goals/${goal.id}`);
      router.refresh();
    } catch (error) {
      // 서버 예외는 msg 를 그대로 쓸 수 있는 봉투로 온다
      setSubmitError(
        error instanceof ApiError ? error.message : '수정하지 못했어요. 잠시 후 다시 시도해 주세요.',
      );
      setSubmitting(false);
    }
  };

  return (
    <>
      <div className="form-mode-banner">
        <b>{GOAL_TYPE_LABELS[goal.type]}</b> 성취를 수정하고 있어요. 유형은 바꿀 수 없어요 — 다른
        유형으로 남기려면 새로 등록해 주세요.
      </div>

      <FormGroup
        label="진행 상태"
        hint={
          statuses.length === 1
            ? '달성은 마지막 상태라 더 바꿀 수 없어요.'
            : '달성으로 바꾸면 되돌릴 수 없어요.'
        }
      >
        <RadioChipGroup
          options={statuses.map((value) => GOAL_STATUS_LABELS[value])}
          value={GOAL_STATUS_LABELS[status]}
          onChange={(label) =>
            setStatus(statuses.find((value) => GOAL_STATUS_LABELS[value] === label) ?? status)
          }
        />
      </FormGroup>

      {isContest ? (
        <>
          <FormGroup
            label="대회명"
            htmlFor="goalContestName"
            required
            hint="크루온 밖에서 참가한 대회를 직접 적는 자리예요."
            error={errors.contestName}
          >
            <TextField
              id="goalContestName"
              value={contestName}
              placeholder="예: 2025 공공데이터 활용 공모전"
              onChange={(event) => setContestName(event.target.value)}
            />
          </FormGroup>

          <FormGroup label="참가 형태">
            <RadioChipGroup options={TEAM_OPTIONS} value={teamLabel} onChange={setTeamLabel} />
          </FormGroup>

          <FormGroup
            label="대회 링크"
            htmlFor="goalContestUrl"
            hint="대회 공고나 결과 발표 페이지 주소예요. 나중에 이 기록을 확인할 때 근거가 됩니다."
            error={errors.contestUrl}
          >
            <TextField
              id="goalContestUrl"
              type="url"
              inputMode="url"
              value={contestUrl}
              placeholder="https://example.com/contest"
              onChange={(event) => setContestUrl(event.target.value)}
            />
          </FormGroup>

          <FormRow>
            <FormGroup label="수상 결과" htmlFor="goalResult" hint="수상하지 않았다면 비워두세요.">
              <TextField
                id="goalResult"
                value={result}
                placeholder="예: 우수상"
                onChange={(event) => setResult(event.target.value)}
              />
            </FormGroup>

            <FormGroup label="수상일" htmlFor="goalAwardDate" hint="성취 리스트의 연도 기준이에요.">
              <TextField
                id="goalAwardDate"
                type="date"
                value={awardDate}
                onChange={(event) => setAwardDate(event.target.value)}
              />
            </FormGroup>
          </FormRow>
          <FormGroup
            label="증빙 자료"
            hint="수상 확인서·결과 발표 화면처럼 수상을 확인할 수 있는 파일이에요. 여러 개 올릴 수 있어요."
          >
            {evidence.length > 0 ? (
              <ul className="goal-evidence-list">
                {evidence.map((file) => (
                  <li key={file.name}>
                    <span className="goal-file-name">{file.name}</span>
                    <span className="goal-file-type">
                      {[file.type, formatBytes(file.size)].filter(Boolean).join(' · ')}
                    </span>
                    <button
                      type="button"
                      className="editor-del"
                      aria-label={`${file.name} 삭제`}
                      onClick={() =>
                        setEvidence((prev) => prev.filter((item) => item.name !== file.name))
                      }
                    >
                      <Icon name="i-x" />
                    </button>
                  </li>
                ))}
              </ul>
            ) : null}

            <button type="button" className="editor-add" onClick={() => fileInputRef.current?.click()}>
              <Icon name="i-plus" />
              증빙 파일 추가
            </button>
            <input
              ref={fileInputRef}
              type="file"
              multiple
              hidden
              accept="image/png,image/jpeg,application/pdf"
              onChange={(event) => {
                addFiles(event.target.files);
                // 같은 파일을 다시 고를 수 있게 비워둔다
                event.target.value = '';
              }}
            />
          </FormGroup>

        </>
      ) : (
        <>
          <FormGroup label="목표 제목" htmlFor="goalTitle" required error={errors.title}>
            <TextField
              id="goalTitle"
              value={title}
              placeholder="예: 정보처리기사 취득"
              onChange={(event) => setTitle(event.target.value)}
            />
          </FormGroup>

          <FormGroup label="목표일" htmlFor="goalTargetDate" hint="언제까지 해낼 계획인지 적어두세요.">
            <TextField
              id="goalTargetDate"
              type="date"
              value={targetDate}
              onChange={(event) => setTargetDate(event.target.value)}
            />
          </FormGroup>

          <FormGroup label="메모" htmlFor="goalMemo" hint="준비 방법이나 남길 말을 자유롭게 적어요.">
            <TextAreaField
              id="goalMemo"
              rows={5}
              value={memo}
              onChange={(event) => setMemo(event.target.value)}
            />
          </FormGroup>
        </>
      )}

      {submitError ? <p className="form-error">{submitError}</p> : null}

      <FormActions>
        <Button onClick={submit} disabled={submitting}>
          {submitting ? '저장 중…' : '저장'}
        </Button>
        <Button variant="ghost" onClick={() => router.push(`/goals/${goal.id}`)}>
          취소
        </Button>
      </FormActions>
    </>
  );
}
