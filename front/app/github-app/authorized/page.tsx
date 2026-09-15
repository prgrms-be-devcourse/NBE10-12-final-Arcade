'use client';
export default function GithubAppAuthorizedPage() {
  const returnToParty = () => {
    const saved = sessionStorage.getItem('githubAppReturnPath');
    sessionStorage.removeItem('githubAppReturnPath');
    window.location.assign(saved?.startsWith('/party/') ? `${saved}?githubRepoManager=1` : '/mypage');
  };
  return <main><div className="board-wrap container"><section className="github-callback"><h1>GitHub 권한 인증 완료</h1><p>연결할 GitHub 레포를 선택할 수 있어요.</p><button type="button" className="btn btn-primary" onClick={returnToParty}>Party로 돌아가기</button></section></div></main>;
}
