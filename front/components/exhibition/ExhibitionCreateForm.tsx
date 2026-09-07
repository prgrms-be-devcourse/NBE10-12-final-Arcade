'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { FormActions, FormGroup, TextAreaField, TextField } from '@/components/ui/Field';
import { ApiError, fetchExhibition, publishPartyShowcase } from '@/lib/api';

/**
 * 전시 게시 폼.
 *
 * 전시는 파티에 종속이라(POST /api/v1/parties/{partyId}/showcase) 파티를 고르는 화면이 아니라
 * **파티에서 들어오는** 화면이다. 팀 스페이스의 '전시 게시' 버튼이 partyId 를 달고 보낸다.
 *
 * 서버가 받는 값은 title · description 뿐이다. 대표 이미지 · 분야 · 기술스택 · 링크는
 * 서버에 담을 자리가 없어 뺐다 (docs/마이페이지-요약API_백엔드_요청.md ⑥).
 *
 * 이미 게시된 파티에 다시 보내면 제목·설명이 갱신된다 — 등록과 수정이 같은 호출이다.
 */
export function ExhibitionCreateForm({ partyId }: { partyId?: string }) {
  const router = useRouter();
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [partyName, setPartyName] = useState('');
  const [published, setPublished] = useState(false);
  const [loading, setLoading] = useState(Boolean(partyId));
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  // 초안을 읽어 이미 적어둔 값을 채운다. 게시 전 초안은 파티원만 볼 수 있다(403).
  useEffect(() => {
    if (!partyId) return;
    let alive = true;

    fetchExhibition(partyId)
      .then((draft) => {
        if (!alive) return;
        setTitle(draft.title);
        setDescription(draft.description);
        setPartyName(draft.partyName);
        setPublished(draft.period !== '');
      })
      .catch((error) => {
        if (!alive) return;
        setErrors({
          load: error instanceof ApiError ? error.message : '전시 정보를 불러오지 못했어요.',
        });
      })
      .finally(() => {
        if (alive) setLoading(false);
      });

    return () => {
      alive = false;
    };
  }, [partyId]);

  if (!partyId) {
    return (
      <p className="notif-empty">
        전시는 파티 단위로 게시해요. 팀 스페이스에서 <b>전시 게시</b>를 눌러 들어와 주세요.{' '}
        <Link href="/exhibition">전시관으로 돌아가기</Link>
      </p>
    );
  }

  if (loading) return <p className="notif-empty">전시 정보를 불러오는 중이에요.</p>;
  if (errors.load) return <p className="notif-empty">{errors.load}</p>;

  const submit = async () => {
    const next: Record<string, string> = {};
    if (!title.trim()) next.title = '전시 제목을 입력해 주세요.';
    if (!description.trim()) next.description = '프로젝트 설명을 입력해 주세요.';
    setErrors(next);
    if (Object.keys(next).length > 0) return;

    setSubmitting(true);
    try {
      const result = await publishPartyShowcase(partyId, {
        title: title.trim(),
        description: description.trim(),
      });
      router.push(`/exhibition/${result.id}`);
    } catch (error) {
      // 파티장이 아니면 403, 없는 파티면 404 로 온다. 서버 문구를 그대로 보여준다.
      setErrors({
        submit: error instanceof ApiError ? error.message : '게시하지 못했어요. 잠시 후 다시 시도해 주세요.',
      });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={(event) => event.preventDefault()}>
      {partyName ? (
        <p className="form-hint" style={{ marginTop: 0 }}>
          <b>{partyName}</b> 파티를 전시관에 공개해요. 참여 팀원과 GitHub 저장소는 파티 정보에서
          자동으로 따라옵니다.
        </p>
      ) : null}

      <FormGroup label="전시 제목" required error={errors.title}>
        <TextField
          placeholder="예: 결제 API 안정화 프로젝트"
          value={title}
          onChange={(event) => {
            setTitle(event.target.value);
            setErrors((prev) => ({ ...prev, title: '' }));
          }}
          autoFocus
        />
      </FormGroup>

      <FormGroup label="프로젝트 설명" required error={errors.description}>
        <TextAreaField
          placeholder="어떤 문제를 어떻게 풀었는지 적어주세요."
          value={description}
          onChange={(event) => {
            setDescription(event.target.value);
            setErrors((prev) => ({ ...prev, description: '' }));
          }}
        />
      </FormGroup>

      {errors.submit ? <p className="form-hint">{errors.submit}</p> : null}

      <FormActions>
        <button type="button" className="btn btn-primary" onClick={submit} disabled={submitting}>
          {submitting ? '게시 중…' : published ? '전시 수정' : '전시 게시'}
        </button>
        <button type="button" className="btn btn-ghost" onClick={() => router.push('/exhibition')}>
          취소
        </button>
      </FormActions>
    </form>
  );
}
