'use client';
export default function GithubAppInstalledPage() {
  const returnToParty = () => {
    const saved = sessionStorage.getItem('githubAppReturnPath');
    sessionStorage.removeItem('githubAppReturnPath');
    window.location.assign(saved?.startsWith('/party/') ? saved : '/mypage');
  };
  return <main><div className="board-wrap container"><section className="github-callback"><h1>GitHub App 설치 완료</h1><p>Party로 돌아가 GitHub 레포 관리에서 연동할 레포를 선택해 주세요.</p><button type="button" className="btn btn-primary" onClick={returnToParty}>Party로 돌아가기</button></section></div></main>;
}
