import { Icon } from '@/components/icons/Icon';
import { Avatar } from '@/components/ui/Avatar';
import type { UserProfile } from '@/lib/types';

interface ProfileCardProps {
  profile: UserProfile;
  /** 마이페이지에서만 노출되는 수정 버튼 */
  onEdit?: () => void;
  /** 비밀번호 변경 — 프로필 수정과 별개로 즉시 반영되는 동작이라 버튼을 따로 둔다 */
  onChangePassword?: () => void;
  /** GitHub OAuth로 계정을 연동한다 */
  onConnectGithub?: () => void;
  months?: number;
}

export function ProfileCard({
  profile,
  onEdit,
  onChangePassword,
  onConnectGithub,
  months = 10,
}: ProfileCardProps) {
  return (
    <section className="profile-card">
      <Avatar initial={profile.initial} avatarUrl={profile.avatarUrl} />
      <div className="profile-info">
        <div className="profile-name-row">
          <h2>{profile.name}</h2>
          <span className="badge lv">LV.14</span>
          <span className="badge verified">
            <Icon name="i-check" />
            자동기록 {profile.stats.exhibitions}건
          </span>
        </div>
        <p className="profile-role">{profile.role} · 대표 포지션</p>
        <p className="profile-bio">{profile.bio}</p>
        <div className="hero-meta-row">
          <span className="hero-meta-item">
            크루온 활동 <b>{months}개월째</b>
          </span>
          <span className="hero-meta-item">
            연속 활동 <b>{profile.streakDays}일째</b>
          </span>
        </div>
      </div>
      {onEdit || onChangePassword || onConnectGithub ? (
        <div className="profile-actions">
          {onConnectGithub ? (
            <button type="button" className="btn btn-ghost profile-edit-btn" onClick={onConnectGithub}>
              <Icon name="i-github" viewBox="0 0 16 16" />
              GitHub 연동
            </button>
          ) : null}
          {onEdit ? (
            <button type="button" className="btn btn-ghost profile-edit-btn" onClick={onEdit}>
              <Icon name="i-pencil" />
              수정하기
            </button>
          ) : null}
          {onChangePassword ? (
            <button
              type="button"
              className="btn btn-ghost profile-edit-btn"
              onClick={onChangePassword}
            >
              비밀번호 변경
            </button>
          ) : null}
        </div>
      ) : null}
    </section>
  );
}
