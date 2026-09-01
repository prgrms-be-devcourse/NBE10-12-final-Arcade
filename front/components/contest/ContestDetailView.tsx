'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { Icon } from '@/components/icons/Icon';
import { ContestOwnerTools } from './ContestOwnerTools';
import { DetailActions } from '@/components/ui/DetailActions';
import { PartyCard } from '@/components/party/PartyCard';
import { BackLink } from '@/components/ui/BackLink';
import { Block, DetailGrid, SideCard } from '@/components/ui/Block';
import { DDay, Tag, TagRow } from '@/components/ui/Tag';
import { fetchContest } from '@/lib/api';
import { CONTEST_FORMAT_LABELS } from '@/lib/constants';
import type { ContestDetail } from '@/lib/types';

/**
 * 대회 상세.
 *
 * 조회를 클라이언트에서 한다 — 목 모드에서는 방금 등록한 대회가 브라우저 쪽 저장소에만
 * 있어서, 서버에서 조회하면 등록한 값이 보이지 않기 때문이다.
 * 실제 API 로 바뀌면 fetchContest 호출부는 그대로 두고 서버 컴포넌트로 되돌릴 수 있다.
 */
export function ContestDetailView({ id }: { id: string }) {
  const [contest, setContest] = useState<ContestDetail | null>(null);

  useEffect(() => {
    let alive = true;
    fetchContest(id).then((data) => {
      if (alive) setContest(data);
    });
    return () => {
      alive = false;
    };
  }, [id]);

  if (!contest) {
    return (
      <main>
        <div className="board-wrap container">
          <BackLink href="/contests" />
          <p className="notif-empty">대회 정보를 불러오는 중이에요.</p>
        </div>
      </main>
    );
  }

  return (
    <main>
      <div className="board-wrap container">
        <BackLink href="/contests" />

        <DetailGrid
          main={
            <>
              <div className="detail-header">
                <TagRow>
                  <Tag accent>{CONTEST_FORMAT_LABELS[contest.format]}</Tag>
                  <Tag>{contest.tag}</Tag>
                </TagRow>
                <div className="detail-header-right">
                  <span className="detail-views">
                    <Icon name="i-eye" />
                    조회 {contest.viewCount.toLocaleString()}
                  </span>
                  <DetailActions
                    target="contest"
                    id={contest.id}
                    likeCount={contest.likeCount}
                    likedByMe={contest.likedByMe}
                    bookmarkedByMe={contest.bookmarkedByMe}
                  />
                </div>
              </div>

              <h1 className="detail-title">{contest.title}</h1>
              <p className="pboard-meta" style={{ marginTop: '0.625rem' }}>
                {contest.host} · 접수 {contest.period}
              </p>

              <div
                className={
                  contest.coverImageUrl ? 'contest-hero-poster has-cover' : 'contest-hero-poster'
                }
                style={
                  contest.coverImageUrl
                    ? { backgroundImage: `url(${contest.coverImageUrl})` }
                    : undefined
                }
              >
                <Tag accent>{contest.tag}</Tag>
              </div>

              <Block title="공모전 소개">
                <p className="detail-desc">{contest.description}</p>
              </Block>

              <Block
                title="참가팀 · 크루온에서 연결된 파티"
                description="파티 생성 시 이 공모전을 연동하면 여기에 참가팀으로 노출돼요."
              >
                {contest.relatedParties.length > 0 ? (
                  <div className="board-grid">
                    {contest.relatedParties.map((party) => (
                      <PartyCard key={party.id} party={party} />
                    ))}
                  </div>
                ) : (
                  <p className="notif-empty">아직 연결된 파티가 없어요.</p>
                )}
              </Block>
            </>
          }
          side={
            <>
              <SideCard title="공모전 정보">
                <div className="stat-list">
                  <div className="stat-row">
                    <span className="label">접수 마감</span>
                    <span className="value">
                      <DDay>{contest.dday}</DDay>
                    </span>
                  </div>
                  <div className="stat-row">
                    <span className="label">참가팀</span>
                    <span className="value">{contest.teams}</span>
                  </div>
                </div>
              </SideCard>

              <SideCard>
                <Link className="btn btn-primary" style={{ width: '100%' }} href="/party/create">
                  이 대회로 파티 만들기
                </Link>
                <a
                  className="btn btn-ghost"
                  style={{ width: '100%', marginTop: '0.625rem' }}
                  href={contest.linkUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  외부 사이트에서 보기 ↗
                </a>
              </SideCard>

              <ContestOwnerTools contest={contest} />
            </>
          }
        />
      </div>
    </main>
  );
}
