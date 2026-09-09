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
}

/**
 * 가입일로부터 '크루온 활동 N개월째'(기획서 2.11 총 활동 기간).
 *
 * 가입 당월이 1개월째다 - 가입 첫날 '0개월째' 라고 적히지 않게 한다.
 * 첫 활동일이 아니라 가입일을 쓴다: 활동 로그는 활동한 날만 남아, 오래된 로그를 정리하면
 * 이미 보여준 기간이 줄어든다.
 */
function activeMonths(joinedAt: string): number {
  const joined = new Date(joinedAt);
  const now = new Date();
  const months =
    (now.getFullYear() - joined.getFullYear()) * 12 + (now.getMonth() - joined.getMonth());

  return Math.max(0, months) + 1;
}

export function ProfileCard({
  profile,
  onEdit,
  onChangePassword,
  onConnectGithub,
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
          {/* 가입일을 모르면(비로그인 폴백) 지어내지 않고 문구를 빼 둔다 */}
          {profile.joinedAt ? (
            <span className="hero-meta-item">
              크루온 활동 <b>{activeMonths(profile.joinedAt)}개월째</b>
            </span>
          ) : null}
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
