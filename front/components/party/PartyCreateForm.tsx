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
import {
  createParty,
  fetchContest,
  fetchContests,
  fetchParty,
  searchContests,
  updateParty,
} from '@/lib/api';
import { httpUrlOrNull } from '@/lib/externalUrl';
import { useLeaveTo } from '@/lib/navigation';
import {
  CONTEST_FORMATS,
  CONTEST_FORMAT_LABELS,
  POSITION_LABELS,
  POSITION_TYPES,
  TOPIC_TYPES,
  TOPIC_TYPE_LABELS,
} from '@/lib/constants';
import type { Contest, ContestFormat, PositionType, TopicType } from '@/lib/types';

/** 서버 PartyCreateReqBody 의 @Size 와 같은 값 */
const PARTY_NAME_MAX = 10;
const TITLE_MAX = 20;
/** 파티 총원 상한 — 서버는 아직 이 값을 강제하지 않아 프론트에서만 막는다 */
const TOTAL_CAPACITY_MAX = 10;

interface PositionRow {
  key: string;
  type: PositionType;
  /** 빈 칸으로 시작할 수 있도록 문자열로 다룬다 */
  capacity: string;
}

export function PartyCreateForm({ editId }: { editId?: string }) {
  const router = useRouter();
  const { confirm, dialog } = useConfirm();
  // 수정으로 들어왔으면 되돌아간다 - push 하면 상세에서 뒤로가기를 눌렀을 때 수정 폼이 다시 나온다
  const leaveEdit = useLeaveTo(editId ? `/party/${editId}` : '/party');
  const [topicType, setTopicType] = useState<TopicType>('CONTEST');
  const [contestFormat, setContestFormat] = useState<ContestFormat>('COMPETITION');
  const [contestLinkUrl, setContestLinkUrl] = useState('');
  const [partyName, setPartyName] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [repositoryUrl, setRepositoryUrl] = useState('');
  const [coverFileName, setCoverFileName] = useState<string | null>(null);
  const [positions, setPositions] = useState<PositionRow[]>([
    { key: 'p1', type: 'BACK', capacity: '' },
  ]);
  const [errors, setErrors] = useState<Record<string, string>>({});
  /**
   * 폼에 입력칸이 없는 값. 수정할 때 그대로 돌려보내지 않으면 분야가 ETC 로,
   * 모집 기한이 30일 뒤로 덮인다 (toPartyRequestBody 의 기본값).
   */
  const [carried, setCarried] = useState<{ subCategory?: string; deadline?: string }>({});
  /** 수정 진입 시 기존 값을 읽어오는 동안. 다 읽기 전에 저장하면 빈 값으로 덮인다 */
  const [loading, setLoading] = useState(Boolean(editId));

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
   * 수정 진입이면 기존 파티를 읽어 폼을 채운다.
   *
   * 서버 PartyDto 에는 대회 형식(contestFormat)이 없어 공모전으로 두고 시작한다 -
   * 목록 DTO 에만 있는 값이라 상세에서는 알 수 없다.
   */
  useEffect(() => {
    if (!editId) return;
    let alive = true;

    (async () => {
      const party = await fetchParty(editId);
      // 연결된 대회 카드(포스터·주최·접수기간)를 그리려면 대회 상세가 필요하다
      const contest = party.contestId ? await fetchContest(party.contestId) : null;
      if (!alive) return;

      setTopicType(party.topicType);
      setPartyName(party.partyName ?? '');
      setTitle(party.title);
      setDescription(party.description);
      setRepositoryUrl(party.githubRepoUrl ?? '');
      setContestLinkUrl(party.contestLinkUrl ?? '');
      setPickedContest(contest);
      setContestKeyword(contest ? '' : (party.contestName ?? ''));
      setPositions(
        party.positions.map((position, index) => ({
          key: `p${index}`,
          type: position.type,
          capacity: String(position.capacity),
        })),
      );
      setCarried({ subCategory: party.subCategory, deadline: party.deadline });
      setLoading(false);
    })();

    return () => {
      alive = false;
    };
  }, [editId]);

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

  /** 포지션 종류는 4개뿐이라, 이미 쓰인 타입을 빼면 새 행에 기본으로 넣을 타입이 남았는지 알 수 있다 */
  const unusedType = (rows: PositionRow[]) =>
    POSITION_TYPES.find((type) => !rows.some((row) => row.type === type));

  const addPosition = () => {
    const type = unusedType(positions);
    if (!type) return; // 포지션 종류를 모두 썼으면 더 추가할 게 없다
    setPositions((prev) => [...prev, { key: `p${Date.now()}`, type, capacity: '' }]);
  };

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
  const totalCapacity = positions.reduce((sum, row) => sum + (Number(row.capacity) || 0), 0);

  const validate = () => {
    const next: Record<string, string> = {};
    // 서버 PartyCreateReqBody 가 파티 이름 10자·모집글 제목 20자로 막는다. 넘기면 400 이라 여기서 먼저 잡는다
    if (!partyName.trim()) next.partyName = '파티 이름을 입력해 주세요.';
    else if (partyName.trim().length > PARTY_NAME_MAX)
      next.partyName = `파티 이름은 ${PARTY_NAME_MAX}자까지 입력할 수 있어요.`;
    if (!title.trim()) next.title = '모집글 제목을 입력해 주세요.';
    else if (title.trim().length > TITLE_MAX)
      next.title = `모집글 제목은 ${TITLE_MAX}자까지 입력할 수 있어요.`;
    if (!description.trim()) next.description = '파티 소개를 입력해 주세요.';
    // pickedContest 가 있으면 linkUrl 은 대회 쪽 값을 그대로 쓰므로 이 입력칸 자체가 안 보인다
    if (topicType === 'CONTEST' && !pickedContest) {
      if (!contestLinkUrl.trim()) next.contestLinkUrl = '대회 원본 링크를 입력해 주세요.';
      else if (!httpUrlOrNull(contestLinkUrl.trim()))
        next.contestLinkUrl = 'http:// 또는 https://로 시작하는 주소를 입력해 주세요.';
    }
    if (positionRequired) {
      const filled = positions.filter((row) => Number(row.capacity) > 0);
      if (filled.length === 0) next.positions = '모집 포지션과 정원을 한 개 이상 입력해 주세요.';
    }
    if (new Set(positions.map((row) => row.type)).size !== positions.length)
      next.positions = '같은 포지션을 중복해서 추가할 수 없어요.';
    if (totalCapacity > TOTAL_CAPACITY_MAX)
      next.positions = `파티 총원은 ${TOTAL_CAPACITY_MAX}명을 넘을 수 없어요.`;
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const submit = async () => {
    if (!validate()) return;
    setSubmitting(true);
    const payload = {
      ...carried,
      partyName,
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
      if (editId) {
        leaveEdit();
        router.refresh();
      } else {
        // 새로 만든 파티로 가는 건 앞으로 가는 이동이라 히스토리에 쌓는 게 맞다
        router.push(`/party/${result.id}`);
      }
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
            required
            hint="대회 링크를 입력해주세요."
            error={errors.contestLinkUrl}
          >
            <TextField
              placeholder="https://..."
              value={contestLinkUrl}
              onChange={(event) => {
                setContestLinkUrl(event.target.value);
                setErrors((prev) => ({ ...prev, contestLinkUrl: '' }));
              }}
            />
          </FormGroup>
        )}
        </>
      ) : null}

      <FormGroup
        label="파티 이름"
        required
        hint={`팀 이름처럼 짧게 — ${PARTY_NAME_MAX}자 이내`}
        error={errors.partyName}
      >
        <TextField
          placeholder="예: 오락실크루"
          maxLength={PARTY_NAME_MAX}
          value={partyName}
          onChange={(event) => {
            setPartyName(event.target.value);
            setErrors((prev) => ({ ...prev, partyName: '' }));
          }}
        />
      </FormGroup>

      <FormGroup label="모집글 제목" required hint={`${TITLE_MAX}자 이내`} error={errors.title}>
        <TextField
          placeholder="예: 오락실 공모전 같이 나가요"
          maxLength={TITLE_MAX}
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

      <FormGroup
        label="모집 포지션"
        required={positionRequired}
        hint={`총원 ${totalCapacity} / ${TOTAL_CAPACITY_MAX}명`}
        error={positionRequired ? errors.positions : undefined}
      >
        <div>
          {positions.map((position) => {
            const rowMax = Math.max(
              0,
              TOTAL_CAPACITY_MAX -
                (totalCapacity - (Number(position.capacity) || 0)),
            );
            return (
              <div key={position.key} className="position-input-row">
                <SelectField
                  value={position.type}
                  onChange={(event) =>
                    patchPosition(position.key, { type: event.target.value as PositionType })
                  }
                >
                  {POSITION_TYPES.filter(
                    (type) => type === position.type || !positions.some((row) => row.type === type),
                  ).map((type) => (
                    <option key={type} value={type}>
                      {POSITION_LABELS[type]}
                    </option>
                  ))}
                </SelectField>
                <TextField
                  type="number"
                  min={1}
                  max={rowMax}
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
            );
          })}
        </div>
        <button
          type="button"
          className="add-row-btn"
          onClick={addPosition}
          disabled={!unusedType(positions)}
        >
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
        <button
          type="button"
          className="btn btn-primary"
          onClick={submit}
          disabled={submitting || loading}
        >
          {loading ? '불러오는 중…' : submitting ? '등록 중…' : editId ? '수정 저장' : '모집글 등록'}
        </button>
        <button type="button" className="btn btn-ghost" onClick={leaveEdit}>
          취소
        </button>
      </FormActions>
    </form>
  );
}
