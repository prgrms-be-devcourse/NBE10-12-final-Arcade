'use client';

import { useState } from 'react';
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
import { createContest, updateContest } from '@/lib/api';
import { CONTEST_FORMATS, CONTEST_FORMAT_LABELS, CONTEST_TAGS } from '@/lib/constants';
import type { ContestFormat, ContestTag } from '@/lib/types';

/**
 * 대회 등록 · 수정 폼.
 * 입력 항목은 상세 페이지가 보여주는 값과 1:1로 맞춰 둔다 —
 * 여기서 채운 값이 그대로 상세에 나타난다.
 */
export function ContestCreateForm({ editId }: { editId?: string }) {
  const router = useRouter();

  const [title, setTitle] = useState('');
  const [format, setFormat] = useState<ContestFormat>('COMPETITION');
  const [tag, setTag] = useState<ContestTag | ''>('');
  const [linkUrl, setLinkUrl] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [coverFileName, setCoverFileName] = useState<string | null>(null);
  const [prize, setPrize] = useState('');
  const [target, setTarget] = useState('');
  const [description, setDescription] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  const validate = () => {
    const next: Record<string, string> = {};
    if (!title.trim()) next.title = '대회명을 입력해 주세요.';
    if (!tag) next.tag = '분야를 선택해 주세요.';
    if (!linkUrl.trim()) next.linkUrl = '원본 페이지 링크를 입력해 주세요.';
    if (!startDate) next.startDate = '접수 시작일을 선택해 주세요.';
    if (!endDate) next.endDate = '접수 종료일을 선택해 주세요.';
    else if (startDate && endDate < startDate) next.endDate = '접수 종료일이 시작일보다 빨라요.';
    if (!coverFileName) next.cover = '대표 이미지를 1장 등록해 주세요.';
    if (!description.trim()) next.description = '공모전 소개를 입력해 주세요.';
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const submit = async () => {
    if (!validate()) return;
    setSubmitting(true);
    const payload = {
      title,
      format,
      tag: tag as ContestTag,
      linkUrl,
      startDate,
      endDate,
      coverFileName: coverFileName ?? undefined,
      prize,
      target,
      description,
    };
    try {
      const result = editId ? await updateContest(editId, payload) : await createContest(payload);
      router.push(`/contests/${result.id}`);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={(event) => event.preventDefault()}>
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
          onChange={(name) => {
            setCoverFileName(name);
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

      <FormRow>
        <FormGroup label="상금" hint="상세 사이드의 '공모전 정보'에 표시돼요.">
          <TextField
            placeholder="예: 500만원"
            value={prize}
            onChange={(event) => setPrize(event.target.value)}
          />
        </FormGroup>
        <FormGroup label="참가 대상">
          <TextField
            placeholder="예: 대학생 · 일반인 누구나 (2~5인 팀)"
            value={target}
            onChange={(event) => setTarget(event.target.value)}
          />
        </FormGroup>
      </FormRow>

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
        <button type="button" className="btn btn-primary" onClick={submit} disabled={submitting}>
          {submitting ? '신청 중…' : editId ? '수정 저장' : '등록 신청'}
        </button>
        <button type="button" className="btn btn-ghost" onClick={() => router.push('/contests')}>
          취소
        </button>
      </FormActions>
    </form>
  );
}
