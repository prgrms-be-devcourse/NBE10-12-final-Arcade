'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { AchievementTimeline } from './AchievementTimeline';
import { BookmarkList } from './BookmarkList';
import { ApplicantManager } from './ApplicantManager';
import { HeroStats } from './HeroStats';
import { MessageBox } from './MessageBox';
import { MypageTabs } from './MypageTabs';
import { ProfileCard } from './ProfileCard';
import { PasswordChangeModal } from './PasswordChangeModal';
import { ProfileEditPanel } from './ProfileEditPanel';
import { TodoTable } from './TodoTable';
import { Footer } from '@/components/layout/Footer';
import { Block, DetailGrid, SideCard } from '@/components/ui/Block';
import { StatusPill, Tag } from '@/components/ui/Tag';
import type { MypageTabKey } from '@/lib/mypageTabs';
import { POSITION_LABELS } from '@/lib/constants';
import type {
  Applicant,
  BookmarkItem,
  DirectMessage,
  TodoItem,
  UserProfile,
} from '@/lib/types';

interface MypageViewProps {
  /** 알림·쪽지에서 ?tab= 으로 진입했을 때의 초기 탭 */
  initialTab: MypageTabKey;
  profile: UserProfile;
  todos: TodoItem[];
  applicants: Applicant[];
  myApplications: Applicant[];
  messages: DirectMessage[];
  bookmarks: BookmarkItem[];
  myParties: { id: string; title: string }[];
}

export function MypageView({
  initialTab,
  profile: initialProfile,
  todos,
  applicants,
  myApplications,
  messages,
  bookmarks,
  myParties,
}: MypageViewProps) {
  const router = useRouter();
  const [profile, setProfile] = useState(initialProfile);
  const [editing, setEditing] = useState(false);
  const [passwordOpen, setPasswordOpen] = useState(false);
  const [activeTab, setActiveTab] = useState<MypageTabKey>(initialTab);

  /** 탭 전환은 즉시 반영하고, 공유·새로고침을 위해 URL 만 뒤따라 갱신한다 */
  const changeTab = (key: MypageTabKey) => {
    setActiveTab(key);
    router.replace(`/mypage?tab=${key}`, { scroll: false });
  };

  return (
    <main>
      <div className="mypage-wrap container">
        {editing ? (
          <ProfileEditPanel
            profile={profile}
            onCancel={() => setEditing(false)}
            onSaved={(updated) => {
              setProfile(updated);
              setEditing(false);
            }}
          />
        ) : (
          <>
            <ProfileCard
              profile={profile}
              onEdit={() => setEditing(true)}
              onChangePassword={() => setPasswordOpen(true)}
            />

            <HeroStats streakDays={profile.streakDays} badges={profile.badges} />

            <MypageTabs active={activeTab} onChange={changeTab} />

            {activeTab === 'identity' ? (
              <IdentityTab profile={profile} />
            ) : null}

            {activeTab === 'todo' ? (
              <div className="mypage-tab-panel">
                <DetailGrid main={<TodoTable todos={todos} />} />
              </div>
            ) : null}

            {activeTab === 'manage' ? (
              <div className="mypage-tab-panel">
                <DetailGrid
                  main={
                    <>
                      <Block title="내 지원" reveal>
                        {myApplications.map((application) => (
                          <div key={application.id} className="applicant-row">
                            <div>
                              <p style={{ fontWeight: 800, fontSize: '.92rem' }}>
                                {application.partyName}
                              </p>
                              <p className="applicant-sub">
                                {POSITION_LABELS[application.position]} 지원 · 성취 프로필 전체
                                첨부됨
                              </p>
                            </div>
                            <StatusPill tone="pending">승인 대기</StatusPill>
                          </div>
                        ))}
                        {myApplications.length === 0 ? (
                          <p className="notif-empty">아직 지원한 파티가 없어요.</p>
                        ) : null}
                      </Block>

                      <Block
                        title="파티 관리"
                        description="내가 파티장인 파티의 지원자를 확인하고 승인·거절하세요."
                        reveal
                      >
                        <ApplicantManager applicants={applicants} parties={myParties} />
                      </Block>
                    </>
                  }
                />
              </div>
            ) : null}

            {activeTab === 'messages' ? (
              <div className="mypage-tab-panel">
                <DetailGrid
                  main={
                    <Block title="쪽지함" reveal>
                      <MessageBox messages={messages} />
                    </Block>
                  }
                />
              </div>
            ) : null}

            {activeTab === 'bookmarks' ? (
              <div className="mypage-tab-panel">
                <DetailGrid
                  main={
                    <Block
                      title="북마크"
                      description="북마크한 파티 · 대회 · 전시를 한 목록에서 볼 수 있어요."
                      reveal
                    >
                      <BookmarkList bookmarks={bookmarks} />
                    </Block>
                  }
                />
              </div>
            ) : null}

          </>
        )}

        {passwordOpen ? (
          <PasswordChangeModal
            email={`${profile.id}@crewon.dev`}
            onClose={() => setPasswordOpen(false)}
          />
        ) : null}

        <Footer contained={false} />
      </div>
    </main>
  );
}

/** 프로필(정체성) 탭 */
function IdentityTab({ profile }: { profile: UserProfile }) {
  return (
    <div className="mypage-tab-panel">
      <DetailGrid
        main={
          <>
            <Block title="참여 파티 히스토리" reveal>
              <div className="history-panel">
                <div className="timeline-item">
                  <p className="role-line">페이브릿지 해커톤 도전팀 · 백엔드</p>
                  <p className="period">
                    <Tag>해커톤</Tag> 2026.08.03 매칭 ~ 진행중
                  </p>
                  <p className="desc">
                    체크리스트 8/12 완료 ·{' '}
                    <Link className="card-link" href="/party/paybridge/team">
                      팀 페이지 보기 →
                    </Link>
                  </p>
                </div>
                <div className="timeline-item">
                  <p className="role-line">그린테크 챌린지 참가팀 · 백엔드</p>
                  <p className="period">
                    <Tag>공모전</Tag> 2025 · 완료
                  </p>
                  <p className="desc">
                    동료인증 완료 ·{' '}
                    <Link className="card-link" href="/exhibition/green-report">
                      전시 페이지 보기 →
                    </Link>
                  </p>
                </div>
              </div>
            </Block>

            <Block title="성취 리스트" reveal>
              <AchievementTimeline achievements={profile.achievements} />
            </Block>

            <Block title="스킬 · 경력" reveal>
              <div className="history-cols">
                <div className="history-panel">
                  <h4>경력</h4>
                  {profile.careers.map((career) => (
                    <div key={career.id} className="timeline-item">
                      <p className="role-line">
                        {career.org} · {career.title}
                      </p>
                      <p className="period">{career.period}</p>
                      <p className="desc">{career.description}</p>
                    </div>
                  ))}
                </div>
                <div className="history-panel">
                  <h4>스킬</h4>
                  <div className="skill-groups" style={{ marginTop: 0 }}>
                    <div>
                      <p className="skill-group-label">보유 스킬</p>
                      <div className="chip-row">
                        {profile.skills.map((skill) => (
                          <span key={skill} className="skill-chip">
                            {skill}
                          </span>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </Block>
          </>
        }
        side={
          <>
            <SideCard title="활동 스코어">
              <div className="stat-list">
                <div className="stat-row">
                  <span className="label">완료한 파티</span>
                  <span className="value">{profile.stats.completedParties}</span>
                </div>
                <div className="stat-row">
                  <span className="label">수상 이력</span>
                  <span className="value">{profile.stats.awards}</span>
                </div>
                <div className="stat-row">
                  <span className="label">자동기록 성취</span>
                  <span className="value">{profile.stats.exhibitions}</span>
                </div>
              </div>
            </SideCard>

            <SideCard title="기타 주소">
              {profile.links.map((link) => (
                <a
                  key={link.id}
                  className="link-row"
                  href={link.url.startsWith('http') ? link.url : `https://${link.url}`}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <span className="txt">
                    <span className="k">{link.label}</span>
                    <br />
                    <span className="v">{link.url}</span>
                  </span>
                </a>
              ))}
              <p className="form-hint" style={{ marginTop: '0.625rem' }}>
                주소 추가·수정은 상단 <b>수정하기</b>에서 관리해요.
              </p>
            </SideCard>
          </>
        }
      />
    </div>
  );
}
