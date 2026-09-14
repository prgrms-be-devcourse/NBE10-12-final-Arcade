'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { CoverUpload } from '@/components/ui/CoverUpload';
import {
  FormActions,
  FormGroup,
  FormRow,
  Options,
  SelectField,
  TextAreaField,
  TextField,
} from '@/components/ui/Field';
import { RadioChipGroup } from '@/components/ui/RadioChipGroup';
import { ApiError, createContest, fetchContest, updateContest, uploadContestImage } from '@/lib/api';
import { CONTEST_FORMATS, CONTEST_FORMAT_LABELS, CONTEST_TAGS } from '@/lib/constants';
import { httpUrlOrNull } from '@/lib/externalUrl';
import { useLeaveTo } from '@/lib/navigation';
import type { ContestFormat, ContestTag } from '@/lib/types';

/**
 * 대회 등록 · 수정 폼.
 * 입력 항목은 상세 페이지가 보여주는 값과 1:1로 맞춰 둔다 —
 * 여기서 채운 값이 그대로 상세에 나타난다.
 */
export function ContestCreateForm({ editId }: { editId?: string }) {
  const router = useRouter();
  // 수정이면 그 대회로, 신규 작성 취소면 목록으로
  const leave = useLeaveTo(editId ? `/contests/${editId}` : '/contests');

  const [title, setTitle] = useState('');
  const [format, setFormat] = useState<ContestFormat>('COMPETITION');
  const [tag, setTag] = useState<ContestTag | ''>('');
  const [linkUrl, setLinkUrl] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [coverFile, setCoverFile] = useState<File | null>(null);
  /**
   * 지금 등록된 대표 사진 URL. CoverUpload 가 기존 파일을 표시하는 방법이 없어(새 파일 선택 전용)
   * 화면에 보여주지는 못하지만, 새 파일을 고르지 않았을 때 저장 시 그대로 돌려보내야
   * updateContest 가 이 값을 지우지 않는다.
   */
  const [existingCoverImageUrl, setExistingCoverImageUrl] = useState<string | undefined>(undefined);
  const [description, setDescription] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');
  /** 수정 진입 시 기존 값을 읽어오는 동안. 다 읽기 전에 저장하면 빈 값으로 덮인다 */
  const [loading, setLoading] = useState(Boolean(editId));
  /** 기존 값을 못 읽어왔을 때. 빈 폼으로 덮어쓰지 못하게 저장 버튼을 계속 막아 둔다 */
  const [loadError, setLoadError] = useState('');

  /**
   * 수정 진입이면 기존 대회를 읽어 폼을 채운다.
   *
   * 대표 이미지는 CoverUpload 가 기존 파일을 표시하는 방법이 없어(새 파일 선택 전용) 채우지 않는다 -
   * 그래서 수정할 때는 새 이미지를 다시 올리지 않아도 되게 validate() 에서 이 필드를 건너뛴다.
   */
  useEffect(() => {
    if (!editId) return;
    let alive = true;

    (async () => {
      // editId 가 언마운트 없이 A → B 로 바뀌는 경우(같은 컴포넌트가 재사용될 때)에도 다시 로딩 상태로
      // 돌아가야 한다 - 안 그러면 B 를 불러오는 동안 화면엔 A 값이 남은 채로 저장 버튼이 눌릴 수 있다
      setLoading(true);
      setLoadError('');
      try {
        const contest = await fetchContest(editId);
        if (!alive) return;

        setTitle(contest.title);
        setFormat(contest.format);
        setTag(contest.tag);
        // 게시글이 없는(archived) 대회는 linkUrl 이 실제 값이 아니라 표시용 대체값('#')이라
        // 그대로 채우면 검증에 걸리는 값으로 폼이 채워진 것처럼 보인다
        setLinkUrl(contest.archived ? '' : contest.linkUrl);
        setStartDate(contest.applicationPeriodStart);
        setEndDate(contest.applicationPeriodEnd);
        setDescription(contest.description);
        setExistingCoverImageUrl(contest.coverImageUrl);
      } catch (cause) {
        if (!alive) return;
        setLoadError(
          cause instanceof ApiError
            ? cause.message
            : '대회 정보를 불러오지 못했어요. 새로고침 후 다시 시도해 주세요.',
        );
      } finally {
        if (alive) setLoading(false);
      }
    })();

    return () => {
      alive = false;
    };
  }, [editId]);

  const validate = () => {
    const next: Record<string, string> = {};
    if (!title.trim()) next.title = '대회명을 입력해 주세요.';
    if (!tag) next.tag = '분야를 선택해 주세요.';
    if (!linkUrl.trim()) next.linkUrl = '원본 페이지 링크를 입력해 주세요.';
    else if (!httpUrlOrNull(linkUrl.trim()))
      next.linkUrl = 'http:// 또는 https://로 시작하는 주소를 입력해 주세요.';
    if (!startDate) next.startDate = '접수 시작일을 선택해 주세요.';
    if (!endDate) next.endDate = '접수 종료일을 선택해 주세요.';
    else if (startDate && endDate < startDate) next.endDate = '접수 종료일이 시작일보다 빨라요.';
    if (!editId && !coverFile) next.cover = '대표 이미지를 1장 등록해 주세요.';
    if (!description.trim()) next.description = '공모전 소개를 입력해 주세요.';
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const submit = async () => {
    if (!validate()) return;
    setSubmitting(true);
    setSubmitError('');
    try {
      // 새 파일을 골랐을 때만 올려서 실어 보낸다. 안 골랐으면(수정 화면) 기존 URL을 그대로 유지한다.
      const coverImageUrl = coverFile ? await uploadContestImage(coverFile) : existingCoverImageUrl;
      const payload = {
        title,
        format,
        tag: tag as ContestTag,
        linkUrl,
        startDate,
        endDate,
        coverImageUrl,
        description,
      };
      const result = editId ? await updateContest(editId, payload) : await createContest(payload);
      if (editId) {
        leave();
        router.refresh();
      } else {
        // 새로 만든 대회로 가는 건 앞으로 가는 이동이라 히스토리에 쌓는 게 맞다
        router.push(`/contests/${result.id}`);
      }
    } catch (cause) {
      setSubmitError(
        cause instanceof ApiError
          ? cause.message
          : '저장하지 못했어요. 잠시 후 다시 시도해 주세요.',
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={(event) => event.preventDefault()}>
      {loadError ? <p className="form-error" role="alert">{loadError}</p> : null}
      {submitError ? <p className="form-error" role="alert">{submitError}</p> : null}
      <FormGroup label="대회명" required error={errors.title}>
        <TextField
          placeholder="예: 2026 공공데이터 활용 챌린지"
          value={title}
          onChange={(event) => {
            setTitle(event.target.value);
            setErrors((prev) => ({ ...prev, title: '' }));
          }}
        />
      </FormGroup>

      <FormGroup label="형식" hint="공모전과 해커톤은 '대회' 하나로 묶여 허브에 함께 노출돼요.">
        <RadioChipGroup
          options={CONTEST_FORMATS.map((value) => CONTEST_FORMAT_LABELS[value])}
          value={CONTEST_FORMAT_LABELS[format]}
          onChange={(label) =>
            setFormat(
              CONTEST_FORMATS.find((value) => CONTEST_FORMAT_LABELS[value] === label) ??
                'COMPETITION',
            )
          }
        />
      </FormGroup>

      <FormRow>
        <FormGroup label="분야" required error={errors.tag}>
          <SelectField
            value={tag}
            onChange={(event) => {
              setTag(event.target.value as ContestTag);
              setErrors((prev) => ({ ...prev, tag: '' }));
            }}
          >
            <option value="">분야를 선택하세요</option>
            <Options values={CONTEST_TAGS} />
          </SelectField>
        </FormGroup>
        <FormGroup label="원본 링크" required error={errors.linkUrl}>
          <TextField
            placeholder="https://..."
            value={linkUrl}
            onChange={(event) => {
              setLinkUrl(event.target.value);
              setErrors((prev) => ({ ...prev, linkUrl: '' }));
            }}
          />
        </FormGroup>
      </FormRow>

      <FormRow>
        <FormGroup label="접수 시작일" required error={errors.startDate}>
          <TextField
            type="date"
            value={startDate}
            onChange={(event) => {
              setStartDate(event.target.value);
              setErrors((prev) => ({ ...prev, startDate: '' }));
            }}
          />
        </FormGroup>
        <FormGroup
          label="접수 종료일"
          required
          error={errors.endDate}
          hint="상세의 D-day 가 이 날짜로 계산돼요."
        >
          <TextField
            type="date"
            value={endDate}
            onChange={(event) => {
              setEndDate(event.target.value);
              setErrors((prev) => ({ ...prev, endDate: '' }));
            }}
          />
        </FormGroup>
      </FormRow>

      <FormGroup label="메인 사진" required error={errors.cover}>
        <CoverUpload
          onFileChange={(file) => {
            setCoverFile(file);
            setErrors((prev) => ({ ...prev, cover: '' }));
          }}
          hint={
            <>
              목록 카드와 상세 페이지 상단에 쓰이는 대표 이미지예요. <b>1장만</b> 등록할 수 있고,
              새로 올리면 기존 사진은 교체됩니다. 권장 비율 4:3.
            </>
          }
        />
      </FormGroup>

      <FormGroup
        label="공모전 소개"
        required
        error={errors.description}
        hint="시상 내역과 심사 · 발표 일정도 여기에 함께 적어주세요."
      >
        <TextAreaField
          placeholder="대회 취지, 심사 기준, 시상 내역, 일정, 지원 내용 등을 적어주세요."
          value={description}
          onChange={(event) => {
            setDescription(event.target.value);
            setErrors((prev) => ({ ...prev, description: '' }));
          }}
        />
      </FormGroup>

      <FormActions>
        <button
          type="button"
          className="btn btn-primary"
          onClick={submit}
          disabled={submitting || loading || Boolean(loadError)}
        >
          {loading ? '불러오는 중…' : submitting ? '신청 중…' : editId ? '수정 저장' : '등록 신청'}
        </button>
        <button type="button" className="btn btn-ghost" onClick={leave}>
          취소
        </button>
      </FormActions>
    </form>
  );
}
