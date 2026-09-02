'use client';

import { useState } from 'react';
import { Icon } from '@/components/icons/Icon';
import { ApiError } from '@/lib/api';
import { startPartyGithubInstall, type PartyGithubConnection, type PartyGithubStatus } from '@/lib/api';

const STATUS_TEXT: Record<PartyGithubStatus, string> = {
  PENDING: '아직 저장소를 연결하지 않았어요.',
  SYNCING: '저장소에 있던 PR을 받아오는 중이에요.',
  ACTIVE: '연결됐어요. 새 PR이 자동으로 쌓입니다.',
  INSTALLATION_REQUIRED: 'GitHub App 설치가 해제됐어요. 다시 연결해 주세요.',
  APPROVAL_PENDING: '조직 관리자의 설치 승인을 기다리고 있어요.',
  ERROR: '연결에 문제가 생겼어요.',
};

/**
 * 서버가 코드값을 그대로 msg 로 내려주는 경우가 있어(400-22) 사람이 읽을 문구로 바꾼다.
 * 서버가 문구를 고치면 이 표는 지워도 된다 (docs/프론트-API연동_백엔드_수정요청.md ⑪).
 */
const ERROR_TEXT: Record<string, string> = {
  GITHUB_REPOSITORY_URL_INVALID:
    '파티에 등록된 GitHub 저장소 주소가 없어요. 파티 수정에서 저장소 주소를 먼저 넣어주세요.',
  GITHUB_APP_INSTALL_STATE_INVALID: '설치 요청이 만료됐어요. 다시 시도해 주세요.',
  GITHUB_APP_INSTALLATION_UNAVAILABLE: 'GitHub App 설치가 해제된 것 같아요. 다시 연결해 주세요.',
  GITHUB_APP_REPOSITORY_REMOVED: '설치 대상에서 저장소가 빠졌어요. 저장소를 다시 포함해 주세요.',
};

/** 이 상태들에서만 설치를 다시 시작할 수 있게 버튼을 보여준다 */
const NEEDS_INSTALL: PartyGithubStatus[] = ['PENDING', 'INSTALLATION_REQUIRED', 'APPROVAL_PENDING', 'ERROR'];

/**
 * 팀 스페이스의 저장소 카드.
 *
 * 연결 상태를 보여주고, 필요하면 GitHub App 설치를 시작한다.
 * 설치는 파티장만 할 수 있는데(서버가 403 으로 막는다) 화면은 권한을 미리 판단하지 않고
 * 서버 문구를 그대로 띄운다 — 화면이 권한 규칙을 따로 들고 있으면 서버와 어긋난다.
 */
export function GithubConnectionCard({
  partyId,
  connection,
  /** 연결 전에 보여줄 파티 등록 저장소 주소 (파티 상세의 githubRepoUrl) */
  fallbackRepository,
}: {
  partyId: string;
  connection: PartyGithubConnection | null;
  fallbackRepository?: string;
}) {
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState('');

  const repository = connection?.repositoryFullName ?? fallbackRepository ?? '';
  const repositoryUrl = repository.startsWith('http')
    ? repository
    : `https://github.com/${repository}`;

  const install = async () => {
    setStarting(true);
    setError('');
    try {
      // 설치가 끝나면 서버 setup 콜백이 이 경로로 되돌려 보낸다
      const { installationUrl } = await startPartyGithubInstall(partyId, `/party/${partyId}/team`);
      window.location.href = installationUrl;
    } catch (caught) {
      // 파티장이 아니면 403, 파티에 저장소 주소가 없으면 400-22 로 온다.
      // 5xx 는 GitHub App 설정값이 서버에 없을 때도 나므로 사용자에게 원문을 보여주지 않는다
      const fallback = '지금은 GitHub 연결을 시작할 수 없어요. 잠시 후 다시 시도해 주세요.';
      setError(
        caught instanceof ApiError && caught.status < 500
          ? (ERROR_TEXT[caught.message] ?? caught.message)
          : fallback,
      );
      setStarting(false);
    }
  };

  return (
    <>
      {repository ? (
        <a className="link-row" href={repositoryUrl} target="_blank" rel="noopener noreferrer">
          <span className="icon">
            <Icon name="i-external" />
          </span>
          <span className="txt">
            <span className="k">GitHub</span>
            <br />
            <span className="v">{repository}</span>
          </span>
        </a>
      ) : null}

      {connection ? (
        <>
          <p className="gh-status" data-status={connection.status}>
            {STATUS_TEXT[connection.status] ?? '연결 상태를 알 수 없어요.'}
          </p>
          {connection.lastError ? (
            <p className="gh-status-detail">
              {ERROR_TEXT[connection.lastErrorCode ?? ''] ?? connection.lastError}
            </p>
          ) : null}

          {NEEDS_INSTALL.includes(connection.status) ? (
            <div className="side-card-action">
              <button type="button" className="btn btn-ghost" disabled={starting} onClick={install}>
                <Icon name="i-external" />
                {starting ? '설치 화면으로 이동 중…' : 'GitHub App 연결하기'}
              </button>
            </div>
          ) : null}

          {error ? <p className="form-error">{error}</p> : null}
        </>
      ) : (
        <p className="gh-status">
          연결 상태를 불러오지 못했어요. 로그인했는지 확인해 주세요.
        </p>
      )}
    </>
  );
}
