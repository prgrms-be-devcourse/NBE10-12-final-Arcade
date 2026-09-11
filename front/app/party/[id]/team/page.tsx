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
  fetchPartyPrGroupsOrEmpty,
  membersOf,
  fetchMyProfileOrNull,
} from '@/lib/api';

export default async function TeamSpacePage({ params, searchParams }: { params: Promise<{ id: string }>; searchParams: Promise<{ githubRepoManager?: string }> }) {
  const { id } = await params;
  const { githubRepoManager } = await searchParams;
  const [party, githubConnection, prGroups, currentUser] = await Promise.all([
    fetchParty(id),
    fetchPartyGithubConnectionOrNull(id),
    fetchPartyPrGroupsOrEmpty(id),
    fetchMyProfileOrNull(),
  ]);

  const leader = party.leader;
  /**
   * 파티원 목록 API 는 아직 없다. 담당자별 PR 응답이 파티장과 승인 파티원을
   * PR 0건이어도 모두 담아 주므로(ARC-108) 그 목록을 팀원 블록에 그대로 쓴다.
   */
  const members = membersOf(prGroups);

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
                  {members.length > 0 ? (
                    members.map((member) => {
                      // 이름만 있고 포지션은 이 응답에 없어 role 을 비운다
                      const user = {
                        id: member.memberId ?? '',
                        name: member.memberName ?? '',
                        initial: (member.memberName ?? '?').charAt(0),
                        role: '',
                      };
                      return (
                        <div key={member.memberId} className="member-row">
                          <LeaderRow user={user} href={`/profile/${user.id}`} />
                          <span className="member-row-right">
                            {member.owner ? <Tag accent>파티장</Tag> : null}
                            <SendMessageButton recipient={user} variant="icon" />
                          </span>
                        </div>
                      );
                    })
                  ) : (
                    // 목록을 못 읽었을 때(비로그인·권한 없음)라도 확실히 아는 파티장은 보여준다
                    <div className="member-row">
                      <LeaderRow user={leader} href={`/profile/${leader.id}`} />
                      <span className="member-row-right">
                        <Tag accent>파티장</Tag>
                        <SendMessageButton recipient={leader} variant="icon" />
                      </span>
                    </div>
                  )}
                </div>
              </Block>

              <Block
                title="진행 기록 · Pull Request"
                description="연결된 GitHub 저장소의 PR을 웹훅으로 받아 쌓아둔 목록이에요. 파티가 끝나면 이 기록이 참여자 성취의 근거가 됩니다."
              >
                <PullRequestList
                  key={`${githubConnection?.status ?? 'NOT_CONNECTED'}:${prGroups.flatMap((group) => group.pullRequests.map((pr) => `${pr.id}-${pr.updatedAt}`)).join(',')}`}
                  partyId={id}
                  groups={prGroups}
                  liveSyncEnabled={githubConnection?.status === 'ACTIVE' || githubConnection?.status === 'SYNCING'}
                />
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

              <FinishPartyButton
                partyId={party.id}
                isOwner={currentUser?.id === party.leader.id}
              />

              <SideCard title="저장소">
                <GithubConnectionCard
                  partyId={id}
                  connection={githubConnection}
                  isOwner={currentUser?.id === party.leader.id}
                  ownerId={party.leader.id}
                  initialRepositoryUrl={party.githubRepoUrl}
                  canSelectRepository={party.status === 'IN_PROGRESS'}
                  autoOpenManager={githubRepoManager === '1'}
                />
              </SideCard>
            </>
          }
        />
      </div>
    </main>
  );
}
