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
import { ChipRow, SkillChip } from '@/components/ui/Tag';
import { createExhibition, updateExhibition } from '@/lib/api';
import type { ExhibitionFormPayload } from '@/lib/api/exhibitions';

const SOURCES = ['파티 연동', '자기신고'] as const;
const CATEGORIES = ['웹 개발', '게임 개발', '앱 개발', '데이터', '기타'] as const;

/** 완료한 파티 목록 — 실제 연동 시 GET /me/parties?status=done 로 대체 */
const COMPLETED_PARTIES = [
  '페이브릿지 해커톤 도전팀 (2026.08 완료)',
  '그린테크 챌린지 참가팀 (2025.11 완료)',
  '결제 API 안정화 해커톤 (2025.06 완료)',
] as const;

export function ExhibitionCreateForm({ editId }: { editId?: string }) {
  const router = useRouter();
  const [source, setSource] = useState<string>(SOURCES[0]);
  const [partyId, setPartyId] = useState('');
  const [title, setTitle] = useState('');
  const [coverFileName, setCoverFileName] = useState<string | null>(null);
  const [summary, setSummary] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('');
  const [link, setLink] = useState('');
  const [skills, setSkills] = useState<string[]>([]);
  const [skillInput, setSkillInput] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  const addSkill = (value: string) => {
    const trimmed = value.trim();
    if (!trimmed || skills.includes(trimmed)) return;
    setSkills((prev) => [...prev, trimmed]);
    setErrors((prev) => ({ ...prev, skills: '' }));
  };

  const validate = () => {
    const next: Record<string, string> = {};
    // 자기신고는 연동할 파티가 없다
    if (source === '파티 연동' && !partyId) next.partyId = '연동할 파티를 선택해 주세요.';
    if (!title.trim()) next.title = '전시 제목을 입력해 주세요.';
    if (!coverFileName) next.cover = '대표 이미지를 1장 등록해 주세요.';
    if (!summary.trim()) next.summary = '한 줄 소개를 입력해 주세요.';
    if (!description.trim()) next.description = '프로젝트 설명을 입력해 주세요.';
    if (!category) next.category = '분야를 선택해 주세요.';
    if (!link.trim()) next.link = 'GitHub · 데모 링크를 입력해 주세요.';
    if (skills.length === 0) next.skills = '사용 기술을 한 개 이상 추가해 주세요.';
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const submit = async () => {
    if (!validate()) return;
    setSubmitting(true);
    const payload: ExhibitionFormPayload = {
      source: source as ExhibitionFormPayload['source'],
      partyId: partyId || undefined,
      title,
      coverFileName: coverFileName ?? undefined,
      summary,
      description,
      category,
      link: link || undefined,
      skills,
    };
    try {
      const result = editId
        ? await updateExhibition(editId, payload)
        : await createExhibition(payload);
      router.push(`/exhibition/${result.id}`);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={(event) => event.preventDefault()}>
      <FormGroup
        label="출처"
        hint="파티 연동을 고르면 완료한 파티를 선택해 체크리스트 스냅샷을 함께 전시할 수 있어요."
      >
        <RadioChipGroup options={SOURCES} value={source} onChange={setSource} />
      </FormGroup>

      {source === '파티 연동' ? (
        <FormGroup label="연동할 파티" required error={errors.partyId}>
          <SelectField
            value={partyId}
            onChange={(event) => {
              setPartyId(event.target.value);
              setErrors((prev) => ({ ...prev, partyId: '' }));
            }}
          >
            <option value="">파티를 선택하세요</option>
            <Options values={COMPLETED_PARTIES} />
          </SelectField>
        </FormGroup>
      ) : null}

      <FormGroup label="전시 제목" required error={errors.title}>
        <TextField
          placeholder="예: 정산 자동화 API"
          value={title}
          onChange={(event) => {
            setTitle(event.target.value);
            setErrors((prev) => ({ ...prev, title: '' }));
          }}
        />
      </FormGroup>

      <FormGroup label="메인 사진" required error={errors.cover}>
        <CoverUpload
          onChange={(name) => {
            setCoverFileName(name);
            setErrors((prev) => ({ ...prev, cover: '' }));
          }}
          hint={
            <>
              전시관 목록 카드와 상세 상단에 노출돼요. <b>1장만</b> 등록할 수 있고, 새로 올리면 기존
              사진은 교체됩니다.
            </>
          }
        />
      </FormGroup>

      <FormGroup label="한 줄 소개" required error={errors.summary}>
        <TextField
          placeholder="예: 페이브릿지 해커톤 도전팀 · 백엔드"
          value={summary}
          onChange={(event) => {
            setSummary(event.target.value);
            setErrors((prev) => ({ ...prev, summary: '' }));
          }}
        />
      </FormGroup>

      <FormGroup label="프로젝트 설명" required error={errors.description}>
        <TextAreaField
          placeholder="어떤 문제를 어떻게 풀었는지, 맡은 역할은 무엇이었는지 적어주세요."
          value={description}
          onChange={(event) => {
            setDescription(event.target.value);
            setErrors((prev) => ({ ...prev, description: '' }));
          }}
        />
      </FormGroup>

      <FormRow>
        <FormGroup label="분야" required error={errors.category}>
          <SelectField
            value={category}
            onChange={(event) => {
              setCategory(event.target.value);
              setErrors((prev) => ({ ...prev, category: '' }));
            }}
          >
            <option value="">분야를 선택하세요</option>
            <Options values={CATEGORIES} />
          </SelectField>
        </FormGroup>
        <FormGroup label="GitHub · 데모 링크" required error={errors.link}>
          <TextField
            placeholder="https://github.com/..."
            value={link}
            onChange={(event) => {
              setLink(event.target.value);
              setErrors((prev) => ({ ...prev, link: '' }));
            }}
          />
        </FormGroup>
      </FormRow>

      <FormGroup label="사용 기술" required error={errors.skills}>
        <ChipRow>
          {skills.map((skill) => (
            <button
              key={skill}
              type="button"
              onClick={() => setSkills((prev) => prev.filter((value) => value !== skill))}
              style={{ background: 'none', border: 'none', padding: 0 }}
              aria-label={`${skill} 삭제`}
            >
              <SkillChip>{skill} ×</SkillChip>
            </button>
          ))}
        </ChipRow>
        <TextField
          placeholder="기술을 입력하고 Enter"
          value={skillInput}
          onChange={(event) => setSkillInput(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              event.preventDefault();
              addSkill(skillInput);
              setSkillInput('');
            }
          }}
        />
      </FormGroup>

      <FormActions>
        <button type="button" className="btn btn-primary" onClick={submit} disabled={submitting}>
          {submitting ? '등록 중…' : editId ? '수정 저장' : '전시 등록'}
        </button>
        <button type="button" className="btn btn-ghost" onClick={() => router.push('/exhibition')}>
          취소
        </button>
      </FormActions>
    </form>
  );
}
