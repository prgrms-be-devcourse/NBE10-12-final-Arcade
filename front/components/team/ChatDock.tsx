'use client';

import { useEffect, useState } from 'react';
import { Icon } from '@/components/icons/Icon';
import { ChatRoomView } from './ChatRoomView';
import { fetchMyChatRooms, openDirectChatRoom } from '@/lib/api';
import type { ChatRoom, TeamChatGroup } from '@/lib/types';

/**
 * 전역 채팅 도크 — 어느 화면에서든 우측 하단 버튼으로 연다.
 *
 * 방은 매칭이 끝나 '형성된 팀'에만 생긴다.
 * 목록 → 팀 채팅 / 참여자 → 1:1 대화 순으로 좁은 폭에서도 넘어갈 수 있게 단계를 나눴다.
 */
export function ChatDock() {
  const [open, setOpen] = useState(false);
  const [groups, setGroups] = useState<TeamChatGroup[] | null>(null);
  const [room, setRoom] = useState<ChatRoom | null>(null);

  useEffect(() => {
    if (!open || groups) return;
    let alive = true;
    fetchMyChatRooms().then((loaded) => {
      if (alive) setGroups(loaded);
    });
    return () => {
      alive = false;
    };
  }, [open, groups]);

  const totalUnread = (groups ?? []).reduce(
    (sum, group) =>
      sum +
      (group.teamRoom.unreadCount ?? 0) +
      group.directRooms.reduce((inner, dm) => inner + (dm.unreadCount ?? 0), 0),
    0,
  );

  const openDirect = async (partyId: string, memberId: string) => {
    const direct = await openDirectChatRoom(partyId, memberId);
    setRoom(direct);
  };

  return (
    <>
      <button
        type="button"
        className={open ? 'chat-dock-fab is-open' : 'chat-dock-fab'}
        aria-label={open ? '채팅 닫기' : '채팅 열기'}
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
      >
        <Icon name={open ? 'i-x' : 'i-mail'} />
        {!open && totalUnread > 0 ? <span className="chat-dock-badge">{totalUnread}</span> : null}
      </button>

      {open ? (
        <div className="chat-dock" role="dialog" aria-label="채팅">
          <div className="chat-dock-head">
            {room ? (
              <button type="button" className="chat-dock-back" onClick={() => setRoom(null)}>
                <Icon name="i-chevron-left" />
                목록
              </button>
            ) : null}
            <h4>{room ? room.title : '채팅'}</h4>
            {room ? null : <p>매칭이 끝난 팀에만 채팅방이 열려요.</p>}
          </div>

          {room ? (
            <ChatRoomView key={room.id} room={room} />
          ) : groups === null ? (
            <p className="chat-dock-empty">불러오는 중이에요.</p>
          ) : groups.length === 0 ? (
            <p className="chat-dock-empty">
              아직 매칭된 팀이 없어요. 파티에 합류하면 채팅방이 열립니다.
            </p>
          ) : (
            <div className="chat-dock-list">
              {groups.map((group) => (
                <div key={group.partyId} className="chat-dock-group">
                  <p className="chat-dock-group-name">{group.partyTitle}</p>

                  <button
                    type="button"
                    className="chat-room-item"
                    onClick={() => setRoom(group.teamRoom)}
                  >
                    <span className="chat-room-icon">
                      <Icon name="i-users" />
                    </span>
                    <span className="chat-room-body">
                      <span className="chat-room-title">
                        팀 채팅
                        {group.teamRoom.unreadCount ? (
                          <span className="chat-room-unread">{group.teamRoom.unreadCount}</span>
                        ) : null}
                      </span>
                      <span className="chat-room-last">
                        {group.teamRoom.lastMessage ?? '대화를 시작해 보세요'}
                      </span>
                    </span>
                  </button>

                  {/* 참여자를 누르면 그 사람과의 1:1 방이 열린다 */}
                  <p className="chat-dock-sub">참여자 {group.members.length}명</p>
                  {group.members.map((member) => {
                    const existing = group.directRooms.find(
                      (dm) => dm.counterpart?.id === member.id,
                    );
                    return (
                      <button
                        key={member.id}
                        type="button"
                        className="chat-room-item"
                        onClick={() => openDirect(group.partyId, member.id)}
                      >
                        <span className="chat-room-icon">
                          <span className="mini-avatar">{member.initial}</span>
                        </span>
                        <span className="chat-room-body">
                          <span className="chat-room-title">
                            {member.name}
                            {existing?.unreadCount ? (
                              <span className="chat-room-unread">{existing.unreadCount}</span>
                            ) : null}
                          </span>
                          <span className="chat-room-last">
                            {existing?.lastMessage ?? `${member.role} · 1:1 대화 시작`}
                          </span>
                        </span>
                      </button>
                    );
                  })}
                </div>
              ))}
            </div>
          )}
        </div>
      ) : null}
    </>
  );
}
