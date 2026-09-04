import Link from 'next/link';
import { Icon } from '@/components/icons/Icon';
import { GithubConnectionCard } from '@/components/team/GithubConnectionCard';
import { PullRequestList } from '@/components/team/PullRequestList';
import { SendMessageButton } from '@/components/message/SendMessageButton';
import { FinishPartyButton } from '@/components/team/FinishPartyButton';
import { BackLink } from '@/components/ui/BackLink';
import { Block, DetailGrid, SideCard } from '@/components/ui/Block';
import { LeaderRow } from '@/components/ui/Avatar';
import { DDay, Tag, TagRow } from '@/components/ui/Tag';
import {
  fetchParty,
  fetchPartyGithubConnectionOrNull,
  fetchPartyPullRequestsOrEmpty,
} from '@/lib/api';

export default async function TeamSpacePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const [party, githubConnection, pullRequests] = await Promise.all([
    fetchParty(id),
    fetchPartyGithubConnectionOrNull(id),
    fetchPartyPullRequestsOrEmpty(id),
  ]);

  // 파티 상세 API에는 팀원 목록이 아직 없어, 확실히 제공되는 파티장만 표시한다.
  const leader = party.leader;

  return (
    <main>
      <div className="board-wrap container">
        <BackLink href="/mypage" />

        <DetailGrid
          main={
            <>
              <div className="detail-header">
                <TagRow>
                  <Tag>해커톤</Tag>
                  <Tag>{party.subCategory}</Tag>
                  <Tag accent>매칭 완료</Tag>
                </TagRow>
                <DDay>{party.dday}</DDay>
              </div>

              <h1 className="detail-title">{party.title}</h1>
              <div className="pboard-meta" style={{ marginTop: '0.625rem' }}>
                <Icon name="i-users" />
                모집 정원 {party.positions.reduce((total, position) => total + position.capacity, 0)}명
              </div>

              {party.contestName ? (
                <div className="contest-link-card">
                  <div>
                    <p className="clc-label">연동 공모전</p>
                    <h4>{party.contestName}</h4>
                    <p className="clc-sub">파티에 연결된 공모전이에요.</p>
                  </div>
                  {party.contestId ? (
                    <Link className="card-link" href={`/contests/${party.contestId}`}>
                      공모전 보기 →
                    </Link>
                  ) : party.contestLinkUrl ? (
                    <a className="card-link" href={party.contestLinkUrl} target="_blank" rel="noopener noreferrer">
                      공모전 보기 ↗
                    </a>
                  ) : null}
                </div>
              ) : null}

              <Block title="팀원">
                <div className="member-list">
                  <div className="member-row">
                    <LeaderRow user={leader} href={`/profile/${leader.id}`} />
                    <span className="member-row-right">
                      <Tag accent>파티장</Tag>
                      <SendMessageButton recipient={leader} variant="icon" />
                    </span>
                  </div>
                </div>
                <p className="checklist-note">팀원 전체 목록 API가 추가되면 승인된 팀원을 함께 표시합니다.</p>
              </Block>

              <Block
                title="진행 기록 · Pull Request"
                description="연결된 GitHub 저장소의 PR을 웹훅으로 받아 쌓아둔 목록이에요. 파티가 끝나면 이 기록이 참여자 성취의 근거가 됩니다."
              >
                <PullRequestList pullRequests={pullRequests} />
              </Block>

              <Block title="진행 기록 · 커밋">
                <p className="checklist-note">
                  커밋 단위 수집·승인·댓글 API는 아직 제공되지 않습니다. 현재 서버에서 동기화하는
                  진행 기록은 위 PR 목록입니다.
                </p>
              </Block>
            </>
          }
          side={
            <>
              <SideCard title="파티장">
                <LeaderRow user={leader} href={`/profile/${leader.id}`} card />
                <div className="side-card-action">
                  <SendMessageButton recipient={leader} />
                </div>
              </SideCard>

              <FinishPartyButton partyId={party.id} />

              <SideCard title="저장소">
                <GithubConnectionCard
                  partyId={id}
                  connection={githubConnection}
                  fallbackRepository={party.githubRepoUrl}
                />
              </SideCard>
            </>
          }
        />
      </div>
    </main>
  );
}
