'use client';

import { useEffect, useRef, useState } from 'react';
import { Icon } from '@/components/icons/Icon';
import {
  connectChatSocket,
  fetchChatMessages,
  sendChatMessage,
  type ChatSocketStatus,
} from '@/lib/api';
import type { ChatMessage, ChatRoom } from '@/lib/types';

const STATUS_LABEL: Record<ChatSocketStatus, string> = {
  connecting: '연결 중…',
  open: '실시간 연결됨',
  closed: '연결 끊김 · 새로고침하면 다시 연결돼요',
  mock: '데모 모드 · 실시간 수신은 서버 연결 후 동작해요',
};

/**
 * 채팅방 하나의 메시지 뷰.
 * 팀 채팅(PARTY)과 1:1(DIRECT) 모두 같은 화면을 쓰고, 헤더 표기만 달라진다.
 */
export function ChatRoomView({ room }: { room: ChatRoom }) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [status, setStatus] = useState<ChatSocketStatus>('connecting');
  const [draft, setDraft] = useState('');
  const logRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let alive = true;
    fetchChatMessages(room.id).then((loaded) => {
      if (alive) setMessages(loaded);
    });

    const disconnect = connectChatSocket(room.id, {
      onMessage: (incoming) =>
        setMessages((prev) =>
          prev.some((item) => item.id === incoming.id) ? prev : [...prev, incoming],
        ),
      onStatus: setStatus,
    });

    return () => {
      alive = false;
      disconnect();
    };
  }, [room.id]);

  // 새 메시지가 붙으면 항상 마지막이 보이게 한다
  useEffect(() => {
    const log = logRef.current;
    if (log) log.scrollTop = log.scrollHeight;
  }, [messages]);

  const send = async () => {
    const content = draft.trim();
    if (!content) return;

    const tempId = `temp-${Date.now()}`;
    const now = new Date();
    const pad = (value: number) => String(value).padStart(2, '0');
    const optimistic: ChatMessage = {
      id: tempId,
      roomId: room.id,
      senderId: 'me',
      senderName: '나',
      senderInitial: '나',
      content,
      date: `${now.getFullYear()}.${pad(now.getMonth() + 1)}.${pad(now.getDate())}`,
      sentAt: `${pad(now.getHours())}:${pad(now.getMinutes())}`,
      mine: true,
      pending: true,
    };
    setMessages((prev) => [...prev, optimistic]);
    setDraft('');

    try {
      const saved = await sendChatMessage(room.id, content);
      setMessages((prev) => prev.map((item) => (item.id === tempId ? saved : item)));
    } catch {
      setMessages((prev) =>
        prev.map((item) =>
          item.id === tempId ? { ...item, pending: false, failed: true } : item,
        ),
      );
    }
  };

  // 날짜가 바뀌는 지점에만 구분선을 넣는다
  let lastDate = '';

  return (
    <div className="chat-panel">
      <div className="chat-head">
        <span className="chat-participants">
          {room.type === 'DIRECT' && room.counterpart ? (
            <>
              <span className="mini-avatar">{room.counterpart.initial}</span>
              <span className="chat-participant-count">
                {room.counterpart.name}님과의 1:1 대화
              </span>
            </>
          ) : (
            <>
              {room.participants.map((participant) => (
                <span key={participant.id} className="mini-avatar" title={participant.name}>
                  {participant.initial}
                </span>
              ))}
              <span className="chat-participant-count">파티원 {room.participants.length}명</span>
            </>
          )}
        </span>
        <span className="chat-status" data-status={status}>
          {STATUS_LABEL[status]}
        </span>
      </div>

      <div className="chat-log" ref={logRef}>
        {messages.length === 0 ? (
          <p className="chat-empty">아직 대화가 없어요. 첫 메시지를 남겨보세요.</p>
        ) : (
          messages.map((message) => {
            const showDate = message.date !== lastDate;
            lastDate = message.date;
            return (
              <div key={message.id}>
                {showDate ? <p className="chat-day">{message.date}</p> : null}
                <div className={message.mine ? 'chat-msg is-mine' : 'chat-msg'}>
                  {message.mine ? null : (
                    <span className="mini-avatar">{message.senderInitial}</span>
                  )}
                  <div className="chat-msg-body">
                    {message.mine ? null : (
                      <span className="chat-sender">{message.senderName}</span>
                    )}
                    <p className="chat-bubble">{message.content}</p>
                    <span className="chat-time">
                      {message.failed
                        ? '전송 실패'
                        : message.pending
                          ? '보내는 중…'
                          : message.sentAt}
                    </span>
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>

      <div className="chat-form">
        <input
          type="text"
          placeholder="메시지를 입력하세요"
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              event.preventDefault();
              send();
            }
          }}
        />
        <button type="button" className="btn btn-primary" onClick={send}>
          <Icon name="i-mail" />
          보내기
        </button>
      </div>
    </div>
  );
}
