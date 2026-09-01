'use client';

import { useState } from 'react';
import { Icon } from '@/components/icons/Icon';
import { useConfirm } from '@/components/ui/ConfirmDialog';
import { CoverUpload } from '@/components/ui/CoverUpload';
import {
  FormGroup,
  FormRow,
  SelectField,
  TextAreaField,
  TextField,
} from '@/components/ui/Field';
import { ChipRow, SkillChip } from '@/components/ui/Tag';
import { POSITION_LABELS, POSITION_TYPES } from '@/lib/constants';
import { updateMyProfile } from '@/lib/api';
import type {
  Achievement,
  CareerItem,
  PositionType,
  ProfileLink,
  UserProfile,
} from '@/lib/types';

interface ProfileEditPanelProps {
  profile: UserProfile;
  onCancel: () => void;
  onSaved: (profile: UserProfile) => void;
}

/** 마이페이지 프로필 수정 패널 (성취 · 경력 · 링크 인라인 에디터 포함) */
export function ProfileEditPanel({ profile, onCancel, onSaved }: ProfileEditPanelProps) {
  const { confirm, dialog } = useConfirm();
  const [nickname, setNickname] = useState(profile.name);
  const [position, setPosition] = useState(profile.position);
  const [bio, setBio] = useState(profile.bio);
  const [githubUsername, setGithubUsername] = useState(profile.githubUsername ?? '');
  const [avatarFileName, setAvatarFileName] = useState<string | null>(null);
  const [skills, setSkills] = useState<string[]>(profile.skills);
  const [skillInput, setSkillInput] = useState('');
  const [achievements, setAchievements] = useState<Achievement[]>(profile.achievements);
  const [careers, setCareers] = useState<CareerItem[]>(profile.careers);
  const [links, setLinks] = useState<ProfileLink[]>(profile.links);
  const [saving, setSaving] = useState(false);

  const save = async () => {
    setSaving(true);
    try {
      const updated = await updateMyProfile({
        nickname,
        position,
        bio,
        githubUsername: githubUsername.trim() || undefined,
        avatarFileName: avatarFileName ?? undefined,
        skills,
        achievements,
        careers,
        links,
      });
      onSaved(updated);
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="block profile-edit-panel">
      {dialog}
      <h3 className="block-title">프로필 수정</h3>
      <p className="form-hint" style={{ marginTop: 0, marginBottom: '1.125rem' }}>
        닉네임·대표 포지션·소개는 파티 지원 시 그대로 노출됩니다. 자동기록 성취는 수정할 수 없어요.
      </p>

      <FormGroup label="프로필 사진">
        <CoverUpload
          compact
          title="사진 업로드"
          sub="JPG · PNG · 5MB 이하"
          onChange={setAvatarFileName}
          hint={
            <>
              프로필·지원 카드에 함께 노출돼요. <b>1장만</b> 등록할 수 있고, 없으면 기존처럼 이니셜
              아바타가 표시됩니다.
            </>
          }
        />
      </FormGroup>

      <FormRow>
        <FormGroup label="닉네임" htmlFor="editNickname">
          <TextField
            id="editNickname"
            value={nickname}
            onChange={(event) => setNickname(event.target.value)}
          />
        </FormGroup>
        <FormGroup label="대표 포지션" htmlFor="editPosition">
          <SelectField
            id="editPosition"
            value={position}
            onChange={(event) => setPosition(event.target.value as PositionType)}
          >
            {POSITION_TYPES.map((type) => (
              <option key={type} value={type}>
                {POSITION_LABELS[type]}
              </option>
            ))}
          </SelectField>
        </FormGroup>
      </FormRow>

      <FormGroup label="한 줄 소개" htmlFor="editBio">
        <TextAreaField
          id="editBio"
          style={{ minHeight: '5rem' }}
          value={bio}
          onChange={(event) => setBio(event.target.value)}
        />
      </FormGroup>

      <FormGroup
        label="GitHub 사용자명"
        htmlFor="editGithub"
        hint="팀 스페이스의 커밋 작성자를 내 계정과 연결하는 데 쓰여요. @ 없이 사용자명만 적어주세요."
      >
        <TextField
          id="editGithub"
          placeholder="예: skyjeong"
          value={githubUsername}
          onChange={(event) => setGithubUsername(event.target.value)}
        />
      </FormGroup>

      <FormGroup label="스킬">
        <ChipRow>
          {skills.map((skill) => (
            <button
              key={skill}
              type="button"
              style={{ background: 'none', border: 'none', padding: 0 }}
              aria-label={`${skill} 삭제`}
              onClick={() => setSkills((prev) => prev.filter((value) => value !== skill))}
            >
              <SkillChip>{skill} ×</SkillChip>
            </button>
          ))}
        </ChipRow>
        <TextField
          placeholder="스킬을 입력하고 Enter"
          value={skillInput}
          onChange={(event) => setSkillInput(event.target.value)}
          onKeyDown={(event) => {
            if (event.key !== 'Enter') return;
            event.preventDefault();
            const value = skillInput.trim();
            if (value && !skills.includes(value)) setSkills((prev) => [...prev, value]);
            setSkillInput('');
          }}
        />
      </FormGroup>

      <EditorBlock
        title="성취 리스트"
        hint="플랫폼 자동기록 성취는 삭제할 수 없어요. 직접 추가한 항목만 편집됩니다."
        addLabel="성취 추가"
        onAdd={() =>
          setAchievements((prev) => [
            ...prev,
            {
              id: `achv-${Date.now()}`,
              type: 'PROJECT',
              status: 'WANT',
              source: 'SELF_REPORTED',
              year: String(new Date().getFullYear()),
              period: '',
              title: '',
              description: '',
              tags: [],
              links: [],
              viewCount: 0,
            },
          ])
        }
      >
        {achievements.map((achievement) => (
          <div key={achievement.id} className="editor-row">
            <TextField
              placeholder="제목"
              value={achievement.title}
              disabled={achievement.source === 'PLATFORM_VERIFIED'}
              onChange={(event) =>
                setAchievements((prev) =>
                  prev.map((item) =>
                    item.id === achievement.id ? { ...item, title: event.target.value } : item,
                  ),
                )
              }
            />
            <TextField
              placeholder="기간 (예: 2026.08)"
              value={achievement.period}
              disabled={achievement.source === 'PLATFORM_VERIFIED'}
              onChange={(event) =>
                setAchievements((prev) =>
                  prev.map((item) =>
                    item.id === achievement.id ? { ...item, period: event.target.value } : item,
                  ),
                )
              }
            />
            <button
              type="button"
              className="editor-del"
              aria-label="성취 삭제"
              disabled={achievement.source === 'PLATFORM_VERIFIED'}
              onClick={async () => {
                const ok = await confirm({
                  title: '성취를 삭제할까요?',
                  description: `'${achievement.title || '제목 없음'}' 항목이 프로필에서 사라져요.`,
                });
                if (ok) {
                  setAchievements((prev) => prev.filter((item) => item.id !== achievement.id));
                }
              }}
            >
              <Icon name="i-x" />
            </button>
          </div>
        ))}
      </EditorBlock>

      <EditorBlock
        title="경력"
        hint="회사 · 역할 / 기간 / 한 줄 설명"
        addLabel="경력 추가"
        onAdd={() =>
          setCareers((prev) => [
            ...prev,
            { id: `career-${Date.now()}`, period: '', title: '', org: '', description: '' },
          ])
        }
      >
        {careers.map((career) => (
          <div key={career.id} className="editor-row">
            <TextField
              placeholder="회사 · 역할"
              value={career.org}
              onChange={(event) =>
                setCareers((prev) =>
                  prev.map((item) =>
                    item.id === career.id ? { ...item, org: event.target.value } : item,
                  ),
                )
              }
            />
            <TextField
              placeholder="기간"
              value={career.period}
              onChange={(event) =>
                setCareers((prev) =>
                  prev.map((item) =>
                    item.id === career.id ? { ...item, period: event.target.value } : item,
                  ),
                )
              }
            />
            <button
              type="button"
              className="editor-del"
              aria-label="경력 삭제"
              onClick={async () => {
                const ok = await confirm({ title: '경력을 삭제할까요?' });
                if (ok) setCareers((prev) => prev.filter((item) => item.id !== career.id));
              }}
            >
              <Icon name="i-x" />
            </button>
          </div>
        ))}
      </EditorBlock>

      <EditorBlock
        title="기타 주소"
        hint="어디 주소인지(GitHub · Blog · 포트폴리오 등)와 링크를 함께 적어주세요."
        addLabel="링크 추가"
        onAdd={() =>
          setLinks((prev) => [...prev, { id: `link-${Date.now()}`, label: '', url: '' }])
        }
      >
        {links.map((link) => (
          <div key={link.id} className="editor-row">
            <TextField
              placeholder="이름 (예: GitHub)"
              value={link.label}
              onChange={(event) =>
                setLinks((prev) =>
                  prev.map((item) =>
                    item.id === link.id ? { ...item, label: event.target.value } : item,
                  ),
                )
              }
            />
            <TextField
              placeholder="주소"
              value={link.url}
              onChange={(event) =>
                setLinks((prev) =>
                  prev.map((item) =>
                    item.id === link.id ? { ...item, url: event.target.value } : item,
                  ),
                )
              }
            />
            <button
              type="button"
              className="editor-del"
              aria-label="링크 삭제"
              onClick={async () => {
                const ok = await confirm({ title: '링크를 삭제할까요?' });
                if (ok) setLinks((prev) => prev.filter((item) => item.id !== link.id));
              }}
            >
              <Icon name="i-x" />
            </button>
          </div>
        ))}
      </EditorBlock>

      <div className="profile-edit-foot">
        <button type="button" className="btn btn-ghost" onClick={onCancel}>
          취소
        </button>
        <button type="button" className="btn btn-primary" onClick={save} disabled={saving}>
          {saving ? '저장 중…' : '저장하기'}
        </button>
      </div>
    </section>
  );
}

function EditorBlock({
  title,
  hint,
  addLabel,
  onAdd,
  children,
}: {
  title: string;
  hint: string;
  addLabel: string;
  onAdd: () => void;
  children: React.ReactNode;
}) {
  return (
    <div className="editor-block">
      <div className="editor-head">
        <h4>{title}</h4>
        <p className="form-hint">{hint}</p>
      </div>
      <div>{children}</div>
      <button type="button" className="editor-add" onClick={onAdd}>
        <Icon name="i-plus" />
        {addLabel}
      </button>
    </div>
  );
}
