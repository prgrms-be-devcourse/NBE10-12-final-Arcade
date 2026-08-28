'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Icon } from '@/components/icons/Icon';
import {
  FormActions,
  FormGroup,
  SelectField,
  TextAreaField,
  TextField,
} from '@/components/ui/Field';
import { RadioChipGroup } from '@/components/ui/RadioChipGroup';
import { useConfirm } from '@/components/ui/ConfirmDialog';
import { CoverUpload } from '@/components/ui/CoverUpload';
import { createParty, fetchContests, searchContests, updateParty } from '@/lib/api';
import {
  CONTEST_FORMATS,
  CONTEST_FORMAT_LABELS,
  POSITION_LABELS,
  POSITION_TYPES,
  TOPIC_TYPES,
  TOPIC_TYPE_LABELS,
} from '@/lib/constants';
import type { Contest, ContestFormat, PositionType, TopicType } from '@/lib/types';

interface PositionRow {
  key: string;
  type: PositionType;
  /** 빈 칸으로 시작할 수 있도록 문자열로 다룬다 */
  capacity: string;
}

export function PartyCreateForm({ editId }: { editId?: string }) {
  const router = useRouter();
  const { confirm, dialog } = useConfirm();
  const [topicType, setTopicType] = useState<TopicType>('CONTEST');
  const [contestFormat, setContestFormat] = useState<ContestFormat>('COMPETITION');
  const [contestLinkUrl, setContestLinkUrl] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [repositoryUrl, setRepositoryUrl] = useState('');
  const [coverFileName, setCoverFileName] = useState<string | null>(null);
  const [positions, setPositions] = useState<PositionRow[]>([
    { key: 'p1', type: 'BACK', capacity: '' },
  ]);
  const [errors, setErrors] = useState<Record<string, string>>({});

  // 공모전 연동 검색
  const [contestKeyword, setContestKeyword] = useState('');
  const [pickedContest, setPickedContest] = useState<Contest | null>(null);
  const [submitting, setSubmitting] = useState(false);
  /** 검색 결과를 요청한 키워드와 함께 보관해, 입력이 바뀌면 자동으로 무효화되게 한다 */
  const [search, setSearch] = useState<{ keyword: string; results: Contest[] }>({
    keyword: '',
    results: [],
  });

  const trimmedKeyword = contestKeyword.trim();
  const pickerOpen = topicType === 'CONTEST' && !pickedContest;
  const contestResults = pickerOpen && search.keyword === trimmedKeyword ? search.results : [];

  /**
   * 대회 목록을 API 에서 불러온다.
   * 검색어가 비어 있으면 전체 목록을, 입력하면 검색 결과를 보여준다.
   */
  useEffect(() => {
    if (!pickerOpen) return;
    let alive = true;
    const timer = window.setTimeout(
      () => {
        const loader = trimmedKeyword ? searchContests(trimmedKeyword) : fetchContests();
        loader.then((results) => {
          if (alive) setSearch({ keyword: trimmedKeyword, results });
        });
      },
      trimmedKeyword ? 200 : 0,
    );
    return () => {
      alive = false;
      window.clearTimeout(timer);
    };
  }, [pickerOpen, trimmedKeyword]);

  const addPosition = () =>
    setPositions((prev) => [...prev, { key: `p${Date.now()}`, type: 'BACK', capacity: '' }]);

  const removePosition = async (key: string) => {
    const ok = await confirm({
      title: '이 포지션을 삭제할까요?',
      description: '모집 정원 설정이 함께 사라져요.',
    });
    if (ok) setPositions((prev) => prev.filter((position) => position.key !== key));
  };

  const patchPosition = (key: string, next: Partial<PositionRow>) =>
    setPositions((prev) =>
      prev.map((position) => (position.key === key ? { ...position, ...next } : position)),
    );

  /** 스터디 · 기타는 포지션을 나눠 뽑지 않으므로 모집 포지션을 필수로 보지 않는다 */
  const positionRequired = topicType === 'CONTEST' || topicType === 'PROJECT';

  const validate = () => {
    const next: Record<string, string> = {};
    if (!title.trim()) next.title = '모집글 제목을 입력해 주세요.';
    if (!description.trim()) next.description = '파티 소개를 입력해 주세요.';
    if (positionRequired) {
      const filled = positions.filter((row) => Number(row.capacity) > 0);
      if (filled.length === 0) next.positions = '모집 포지션과 정원을 한 개 이상 입력해 주세요.';
    }
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const submit = async () => {
    if (!validate()) return;
    setSubmitting(true);
    const payload = {
      topicType,
      contestFormat: topicType === 'CONTEST' ? contestFormat : undefined,
      contestId: pickedContest?.id,
      contestName: pickedContest?.title ?? (topicType === 'CONTEST' ? contestKeyword : undefined),
      contestLinkUrl: pickedContest?.linkUrl ?? (contestLinkUrl || undefined),
      title,
      description,
      coverFileName: coverFileName ?? undefined,
      positions: positions
        .filter((row) => Number(row.capacity) > 0)
        .map(({ type, capacity }) => ({ type, capacity: Number(capacity) })),
      repositoryUrl: repositoryUrl || undefined,
    };
    try {
      const result = editId ? await updateParty(editId, payload) : await createParty(payload);
      router.push(`/party/${result.id}`);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form style={{ marginTop: '2rem' }} onSubmit={(event) => event.preventDefault()}>
      {dialog}
      <FormGroup
        label="주제 유형"
        hint="'대회'를 선택하면 등록된 대회를 검색해 연결할 수 있어요. 나머지 유형은 제목을 자유롭게 입력합니다."
      >
        <RadioChipGroup
          options={TOPIC_TYPES.map((type) => TOPIC_TYPE_LABELS[type])}
          value={TOPIC_TYPE_LABELS[topicType]}
          onChange={(label) =>
            setTopicType(TOPIC_TYPES.find((type) => TOPIC_TYPE_LABELS[type] === label) ?? 'ETC')
          }
        />
      </FormGroup>

      {topicType === 'CONTEST' ? (
        <>
          <FormGroup label="대회 형식" hint="공모전과 해커톤은 '대회' 하나로 묶여 관리돼요.">
            <RadioChipGroup
              options={CONTEST_FORMATS.map((format) => CONTEST_FORMAT_LABELS[format])}
              value={CONTEST_FORMAT_LABELS[contestFormat]}
              onChange={(label) =>
                setContestFormat(
                  CONTEST_FORMATS.find((format) => CONTEST_FORMAT_LABELS[format] === label) ??
                    'COMPETITION',
                )
              }
            />
          </FormGroup>

        <FormGroup
          label="연동할 대회"
          hint="등록된 대회 목록에서 선택해 연결해요. 검색 결과에 없는 외부 대회면 이름과 원본 링크를 직접 입력해도 모집글은 정상 등록되지만, 대회 허브에는 노출되지 않아요."
        >
          {pickedContest ? (
            <div className="picked-card">
              <span className="picker-poster">{pickedContest.tag}</span>
              <span className="picker-meta">
                <span className="pname">{pickedContest.title}</span>
                <span className="psub">
                  {pickedContest.host} · 접수 {pickedContest.period}
                </span>
              </span>
              <button
                type="button"
                className="picked-clear"
                onClick={() => {
                  setPickedContest(null);
                  setContestKeyword('');
                }}
              >
                연결 해제
              </button>
            </div>
          ) : (
            <div className="picker">
              <div className="contest-link-field">
                <TextField
                  placeholder="대회명으로 검색 (예: 프로그래머스 오락실)"
                  autoComplete="off"
                  value={contestKeyword}
                  onChange={(event) => setContestKeyword(event.target.value)}
                />
              </div>
              {contestResults.length > 0 ? (
                <div className="picker-results" hidden={false}>
                  {contestResults.map((contest) => (
                    <button
                      key={contest.id}
                      type="button"
                      className="picker-item"
                      onClick={() => setPickedContest(contest)}
                    >
                      <span className="picker-poster">{contest.tag}</span>
                      <span className="picker-meta">
                        <span className="pname">{contest.title}</span>
                        <span className="psub">
                          {contest.host} · 접수 {contest.period}
                        </span>
                      </span>
                    </button>
                  ))}
                </div>
              ) : null}
            </div>
          )}
        </FormGroup>

        {pickedContest ? null : (
          <FormGroup
            label="대회 원본 링크"
            hint="등록 여부와 관계없이 원본 페이지 링크는 항상 필요해요."
          >
            <TextField
              placeholder="https://..."
              value={contestLinkUrl}
              onChange={(event) => setContestLinkUrl(event.target.value)}
            />
          </FormGroup>
        )}
        </>
      ) : null}

      <FormGroup label="모집글 제목" required error={errors.title}>
        <TextField
          placeholder="예: 프로그래머스 오락실 공모전 참여하실분"
          value={title}
          onChange={(event) => {
            setTitle(event.target.value);
            setErrors((prev) => ({ ...prev, title: '' }));
          }}
        />
      </FormGroup>

      <FormGroup label="파티 소개" required error={errors.description}>
        <TextAreaField
          placeholder="어떤 팀을 찾고 있는지, 어떻게 진행할 계획인지 자유롭게 적어주세요."
          value={description}
          onChange={(event) => {
            setDescription(event.target.value);
            setErrors((prev) => ({ ...prev, description: '' }));
          }}
        />
      </FormGroup>

      <FormGroup label="대표 사진">
        <CoverUpload
          onChange={setCoverFileName}
          hint={
            <>
              파티 목록 카드와 상세 상단에 노출돼요. <b>1장만</b> 등록할 수 있고, 새로 올리면 기존
              사진은 교체됩니다. 권장 비율 4:3.
            </>
          }
        />
      </FormGroup>

      <FormGroup label="모집 포지션" required={positionRequired} error={positionRequired ? errors.positions : undefined}>
        <div>
          {positions.map((position) => (
            <div key={position.key} className="position-input-row">
              <SelectField
                value={position.type}
                onChange={(event) =>
                  patchPosition(position.key, { type: event.target.value as PositionType })
                }
              >
                {POSITION_TYPES.map((type) => (
                  <option key={type} value={type}>
                    {POSITION_LABELS[type]}
                  </option>
                ))}
              </SelectField>
              <TextField
                type="number"
                min={1}
                aria-label="정원"
                placeholder="정원"
                value={position.capacity}
                onChange={(event) => {
                  patchPosition(position.key, { capacity: event.target.value });
                  setErrors((prev) => ({ ...prev, positions: '' }));
                }}
              />
              <button
                type="button"
                className="remove-row"
                aria-label="포지션 삭제"
                onClick={() => removePosition(position.key)}
              >
                <Icon name="i-x" />
              </button>
            </div>
          ))}
        </div>
        <button type="button" className="add-row-btn" onClick={addPosition}>
          <Icon name="i-plus" />
          포지션 추가
        </button>
      </FormGroup>

      <FormGroup label="GitHub 리포지토리 (선택)">
        <TextField
          placeholder="https://github.com/..."
          value={repositoryUrl}
          onChange={(event) => setRepositoryUrl(event.target.value)}
        />
      </FormGroup>

      <FormActions>
        <button type="button" className="btn btn-primary" onClick={submit} disabled={submitting}>
          {submitting ? '등록 중…' : editId ? '수정 저장' : '모집글 등록'}
        </button>
        <button type="button" className="btn btn-ghost" onClick={() => router.push('/party')}>
          취소
        </button>
      </FormActions>
    </form>
  );
}
