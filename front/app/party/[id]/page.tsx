import { Icon } from '@/components/icons/Icon';
import { ApplyPanel } from '@/components/party/ApplyPanel';
import { DetailActions } from '@/components/ui/DetailActions';
import { LeaderTools } from '@/components/party/LeaderTools';
import { SendMessageButton } from '@/components/message/SendMessageButton';
import { BackLink } from '@/components/ui/BackLink';
import { Block, DetailGrid, SideCard } from '@/components/ui/Block';
import { LeaderRow } from '@/components/ui/Avatar';
import { Tag, TagRow } from '@/components/ui/Tag';
import { fetchContest, fetchParty } from '@/lib/api';
import { MOCK_CURRENT_USER_ID } from '@/lib/mock';
import { CONTEST_FORMAT_LABELS, POSITION_LABELS, TOPIC_TYPE_LABELS } from '@/lib/constants';

export default async function PartyDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const party = await fetchParty(id);

  // 연동 대회의 원본 페이지 주소.
  // 크루온에 등록된 대회는 링크가 CONTEST 쪽에 있어 한 번 더 읽고,
  // 미등록 외부 대회는 파티에 자유 입력된 링크를 그대로 쓴다 (기획서 3.5).
  const contestLinkUrl = party.contestId
    ? await fetchContest(party.contestId)
        .then((contest) => contest.linkUrl)
        .catch(() => undefined)
    : party.contestLinkUrl;

  // 매칭 전 문의는 팀원 목록의 쪽지 아이콘으로, 매칭 후 협업 대화는 팀 스페이스의 채팅이 담당한다

  return (
    <main>
      <div className="board-wrap container">
        <BackLink href="/party" />

        <DetailGrid
          main={
            <>
              <div className="detail-header">
                <TagRow>
                  <Tag>{TOPIC_TYPE_LABELS[party.topicType]}</Tag>
                  {party.contestFormat ? (
                    <Tag>{CONTEST_FORMAT_LABELS[party.contestFormat]}</Tag>
                  ) : null}
                  <Tag>{party.subCategory}</Tag>
                </TagRow>
                <div className="detail-header-right">
                  <DetailActions
                    target="party"
                    id={party.id}
                    likeCount={party.likeCount}
                    likedByMe={party.likedByMe}
                    bookmarkedByMe={party.bookmarkedByMe}
                  />
                </div>
              </div>

              <h1 className="detail-title">{party.title}</h1>
              <div className="pboard-meta" style={{ marginTop: '0.625rem' }}>
                <Icon name="i-users" />
                지원자 {party.applicants}명 · 조회 {party.viewCount.toLocaleString()}
              </div>

              {party.contestName ? (
                <div className="contest-link-card">
                  <div>
                    <p className="clc-label">연동 대회</p>
                    <h4>{party.contestName}</h4>
                    <p className="clc-sub">접수 마감 {party.deadline}</p>
                  </div>
                  {contestLinkUrl ? (
                    // 크루온 상세가 아니라 주최측이 운영하는 원본 공고로 보낸다
                    <a
                      className="card-link"
                      href={contestLinkUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      대회 공고 보기 ↗
                    </a>
                  ) : null}
                </div>
              ) : null}

              <Block title="파티 소개" className="block-spaced">
                <p className="detail-desc">{party.description}</p>
              </Block>

              <Block title="모집 포지션">
                <div className="position-list">
                  {party.positions.map((position, index) => {
                    const state =
                      position.capacity === 0
                        ? 'none'
                        : position.filledCount >= position.capacity
                          ? 'full'
                          : '';
                    return (
                      <div
                        key={position.type}
                        className={['position-row', state].filter(Boolean).join(' ')}
                      >
                        <div>
                          <p className="name">{POSITION_LABELS[position.type]}</p>
                          <p className="req">
                            {party.requirements[index] ??
                              '자세한 내용은 파티 소개를 확인해 주세요.'}
                          </p>
                        </div>
                        <span className="frac">
                          {position.filledCount}/{position.capacity}
                          {position.capacity === 0
                            ? ''
                            : position.filledCount >= position.capacity
                              ? ' · 마감'
                              : ' · 모집중'}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </Block>

              <Block title="현재 팀원">
                <div className="member-list">
                  {party.members.map((member) => (
                    <div key={member.id} className="member-row">
                      <LeaderRow user={member} href={`/profile/${member.id}`} />
                      <span className="member-row-right">
                        {member.id === party.leader.id ? <Tag accent>파티장</Tag> : <Tag>팀원</Tag>}
                        {/* 본인에게는 쪽지를 보낼 수 없으므로 아이콘을 숨긴다 */}
                        {member.id === MOCK_CURRENT_USER_ID ? null : (
                          <SendMessageButton recipient={member} variant="icon" />
                        )}
                      </span>
                    </div>
                  ))}
                </div>
              </Block>
            </>
          }
          side={
            <>
              <SideCard title="파티장">
                <LeaderRow
                  user={party.leader}
                  href={`/profile/${party.leader.id}`}
                  role="백엔드 개발자 · 자동기록 5건"
                  card
                />
                <p className="leader-stat-line">완료 파티 3 · 수상 2 · 자동기록 성취 5</p>
              </SideCard>

              <ApplyPanel party={party} />
              <LeaderTools party={party} />
            </>
          }
        />
      </div>
    </main>
  );
}
