'use client';

import { useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Icon } from '@/components/icons/Icon';
import { FormActions, FormGroup, FormRow, TextAreaField, TextField } from '@/components/ui/Field';
import { RadioChipGroup } from '@/components/ui/RadioChipGroup';
import { Button } from '@/components/ui/Button';
import { ApiError } from '@/lib/api';
import {
  evidenceFilesOf,
  formatFileSize,
  updateGoal,
  type GoalDetailResponse,
  type UpdateGoalPayload,
} from '@/lib/api/goals';
import { GOAL_STATUS_LABELS, GOAL_STATUS_TRANSITIONS, GOAL_TYPE_LABELS } from '@/lib/constants';
import type { GoalStatus } from '@/lib/types';

const TEAM_OPTIONS = ['팀 참가', '개인 참가'] as const;

/**
 * 화면에서 다루는 증빙 파일 한 건.
 *
 * file 은 이번에 고른 파일에만 있다 — 이미 저장돼 있던 건 서버에서 메타데이터만 받아오기 때문이다.
 * 업로드 API(⑤-5)가 붙으면 file 이 있는 것만 실제로 올리면 된다. 메타데이터만 남기면 그때 올릴 게 없다.
 */
interface EvidenceFile {
  name: string;
  type?: string;
  size?: number;
  file?: File;
}

/**
 * 증빙으로 받는 형식 — 수상 확인서(PDF)와 결과 발표 화면 캡처(PNG·JPG).
 * 서버에 업로드 API(⑤-5)가 붙으면 거기서도 같은 규칙을 검증해야 한다. 여기 검사는 1차 안내일 뿐이다.
 */
const EVIDENCE_MIME_TYPES = ['image/png', 'image/jpeg', 'application/pdf'];
const EVIDENCE_EXTENSIONS = ['.png', '.jpg', '.jpeg', '.pdf'];

/** 파일 하나당 크기 상한. 스캔한 확인서·화면 캡처는 보통 몇 MB 안쪽이라 10MB면 넉넉하다 */
const EVIDENCE_MAX_SIZE = 10 * 1024 * 1024;

/** 한 성취에 담을 수 있는 증빙 개수 */
const EVIDENCE_MAX_COUNT = 5;

/** 허용 형식인지 본다. 브라우저가 type 을 비워 보내는 경우가 있어 그때는 확장자로 판단한다 */
function isAllowedType(file: File): boolean {
  if (file.type) return EVIDENCE_MIME_TYPES.includes(file.type);
  const name = file.name.toLowerCase();
  return EVIDENCE_EXTENSIONS.some((extension) => name.endsWith(extension));
}

/** 이름과 크기가 같으면 같은 파일로 본다 — 같은 걸 두 번 고르는 실수를 거른다 */
function isSameFile(a: EvidenceFile, b: EvidenceFile): boolean {
  return a.name === b.name && a.size === b.size;
}

/** 파일 이름 옆에 붙는 형식 · 크기 문구 */
function fileMeta(file: EvidenceFile): string {
  return [file.type, formatFileSize(file.size)].filter(Boolean).join(' · ');
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

  // CONTEST — 증빙 자료. 수상 확인서 + 결과 발표 화면처럼 여러 건을 올릴 수 있다
  const [evidence, setEvidence] = useState<EvidenceFile[]>(() =>
    evidenceFilesOf(detail).map((file) => ({
      name: file.fileName,
      type: file.mimeType,
      size: file.size,
    })),
  );
  // 형식·용량·개수에 걸려 못 담은 파일의 사유. 담기지 않았을 뿐이라 저장을 막지는 않는다
  const [evidenceError, setEvidenceError] = useState('');
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

    const problems: string[] = [];
    const picked: EvidenceFile[] = [];

    for (const file of Array.from(files)) {
      if (!isAllowedType(file)) {
        problems.push(`${file.name} — PNG·JPG·PDF 만 올릴 수 있어요`);
        continue;
      }
      if (file.size > EVIDENCE_MAX_SIZE) {
        problems.push(`${file.name} — 파일 하나는 ${formatFileSize(EVIDENCE_MAX_SIZE)} 까지예요`);
        continue;
      }

      const next: EvidenceFile = {
        name: file.name,
        type: file.type || undefined,
        size: file.size,
        // 업로드 API 가 붙으면 이 File 을 그대로 올린다. 메타데이터만 남기면 올릴 실체가 없어진다
        file,
      };
      // 이미 목록에 있거나 이번에 같이 고른 파일은 한 번만 남긴다
      const already = [...evidence, ...picked].some((item) => isSameFile(item, next));
      if (!already) picked.push(next);
    }

    // 남은 자리보다 많이 골랐으면 자리가 차는 데까지만 담는다
    const room = Math.max(EVIDENCE_MAX_COUNT - evidence.length, 0);
    if (picked.length > room) {
      problems.push(`증빙은 최대 ${EVIDENCE_MAX_COUNT}개까지 올릴 수 있어요`);
    }

    setEvidence([...evidence, ...picked.slice(0, room)]);
    setEvidenceError(problems.join(' · '));
  };

  const removeFileAt = (index: number) => {
    setEvidence((prev) => prev.filter((_, at) => at !== index));
    // 자리를 비웠으니 개수 안내는 더 이상 맞지 않는다
    setEvidenceError('');
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
            // 화면에 남아 있는 목록이 곧 저장될 목록이다. 지운 파일은 여기서 빠져 있다
            evidences: evidence.map((file) => ({
              fileName: file.name,
              mimeType: file.type ?? null,
              size: file.size ?? null,
            })),
            // 서버가 아직 한 건만 보관해서(⑤-4) 첫 파일은 예전 필드로도 같이 보낸다. 비었으면 지운다
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
            hint={`수상 확인서·결과 발표 화면처럼 수상을 확인할 수 있는 파일이에요. PNG·JPG·PDF 를 파일당 ${formatFileSize(
              EVIDENCE_MAX_SIZE,
            )}, 최대 ${EVIDENCE_MAX_COUNT}개까지 올릴 수 있어요.`}
            error={evidenceError}
          >
            {evidence.length > 0 ? (
              <ul className="goal-evidence-list">
                {evidence.map((file, index) => (
                  <li key={`${file.name}:${file.size ?? ''}`}>
                    <span className="goal-file-name">{file.name}</span>
                    <span className="goal-file-type">{fileMeta(file)}</span>
                    <button
                      type="button"
                      className="editor-del"
                      aria-label={`${file.name} 삭제`}
                      onClick={() => removeFileAt(index)}
                    >
                      <Icon name="i-x" />
                    </button>
                  </li>
                ))}
              </ul>
            ) : null}

            {/* 서버가 GOAL_EVIDENCE 로 나뉘기 전까지는 첫 파일만 저장된다. 여러 개를 담았을 때만 알린다 */}
            {evidence.length > 1 ? (
              <p className="goal-note">
                지금은 서버가 증빙을 한 건만 보관해서 <b>{evidence[0].name}</b> 만 저장돼요. 나머지는
                여러 건 저장이 열리면 함께 올라갑니다.
              </p>
            ) : null}

            <button
              type="button"
              className="editor-add"
              disabled={evidence.length >= EVIDENCE_MAX_COUNT}
              onClick={() => fileInputRef.current?.click()}
            >
              <Icon name="i-plus" />
              {evidence.length >= EVIDENCE_MAX_COUNT
                ? `증빙은 ${EVIDENCE_MAX_COUNT}개까지예요`
                : '증빙 파일 추가'}
            </button>
            <input
              ref={fileInputRef}
              type="file"
              multiple
              hidden
              accept={EVIDENCE_MIME_TYPES.join(',')}
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
