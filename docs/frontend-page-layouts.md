# 프런트엔드 페이지 레이아웃 맵

> 기준일: 2026-09-11  
> 관련 기준: [frontend-design-system.md](./frontend-design-system.md)  
> 대상: `front/app`의 모든 `page.tsx` 라우트

이 문서는 화면이 어떤 공통 shell과 레이아웃 패턴을 사용해야 하는지 기록한다. 화면 구조를 변경할 때는 이 문서의 패턴을 먼저 따르고, 예외가 생기면 이유를 남긴다.

## 1. 전역 Shell

모든 페이지는 `RootLayout → AppShell → page` 구조로 렌더링된다.

```text
RootLayout
└─ AppShell
   ├─ Header                    (인증 화면에서는 숨김)
   ├─ <main> 페이지 콘텐츠
   ├─ Footer                    (인증 화면에서는 숨김)
   ├─ MobileNav                 (인증 화면에서는 숨김, ≤ 53.75rem)
   └─ AgreementGate             (약관 미동의 계정만)
```

- 일반 페이지의 기본 콘텐츠 폭은 `.container`의 최대 `73.75rem`이다. 좌우 gutter는 `--page-gutter`(`1.5rem`, 모바일 `1rem`)를 사용한다. 좁은 생성·수정 폼은 `.container--form`(`47.5rem`), 읽기/시스템 피드백은 `.container--reading`(`40rem`), 제한된 상세 읽기 영역은 `.container--detail`을 사용한다.
- 게시판·상세 화면의 기본 세로 여백은 `.board-wrap`의 상단 `3rem`·하단 `4.5rem`이다. 상세 열 간격은 `--layout-gap`(`1.5rem`), 반복 `Block`과 `SideCard` 간격은 각각 `2.75rem`·`1.5rem`을 사용한다.
- 인증 화면(`/login`, `/signup`, `/forgot-password`)은 Header/Footer/MobileNav 없이 중앙 정렬 auth shell을 사용한다.
- 모바일에서는 데스크톱 상단 메뉴 대신 하단 `MobileNav`가 제공된다.

## 2. 공통 레이아웃 패턴

| 패턴 | 구조 | 사용 화면 |
| --- | --- | --- |
| 홈 랜딩 | Hero → 복수의 전폭 섹션 | `/` |
| 보드 | 제목/설명/행동 → 필터·목록·페이지네이션 | `/party`, `/contests`, `/exhibition` |
| 단일 폼 | 좁은 container → 뒤로가기 → 안내 배너 → 폼 | 생성·수정 화면 |
| 2열 상세 | 헤더 → 본문 블록 + sticky 사이드 카드 | 파티·공모전·전시 상세 |
| 프로필/대시보드 | 프로필 hero → 지표/탭 → 본문+사이드바 | 마이페이지, 주최자, 공개 프로필 |
| 인증 | viewport 중앙 auth card | 로그인·가입·비밀번호 재설정 |
| 시스템 피드백 | 중앙 정렬 메시지/행동 | GitHub 콜백, 찾을 수 없음 등 |

## 3. 라우트별 레이아웃

### 홈과 보드

| 경로 | 데스크톱 구조 | 모바일 구조 | 주요 구성 요소 |
| --- | --- | --- | --- |
| `/` | Hero Slider → TOP 3 파티 podium 3열 → 인기 공모전 가로 카드열 → 인기 전시 3열 + 전체 보기 | Hero 아트와 슬라이더 화살표 일부 숨김, podium·전시 카드 1열 | `HeroSlider`, `RankCard`, `ContestCard`, `ProjectCard`, `SectionHead` |
| `/party` | 제목/설명 + 우측 “파티 만들기” → 추천 행 → 검색/필터 → 파티 카드 보드 → 페이지네이션 | 헤더 action 및 필터가 줄바꿈, 카드 1열 | `PartyBoard`, `SectionHead` |
| `/contests` | 제목/설명 + 권한 보유 시 우측 “대회 등록” → 필터 → 공모전 카드 보드 → 페이지네이션 | action·필터 줄바꿈, 카드 1열 | `ContestBoard`, `SectionHead` |
| `/exhibition` | 제목/설명 + 우측 “전시 등록” → 전시 카드 보드 → 페이지네이션 | 카드 1열, 지표는 카드 이미지 우하단에 유지 | `ExhibitionBoard`, `ProjectCard`, `SectionHead` |

### 생성·수정 폼

아래 화면은 넓은 보드가 아니라 `max-width: 47.5rem`의 좁은 컨테이너를 기본으로 한다. 편집 실패/권한 없음 같은 시스템 상태는 `max-width: 40rem`에서 안내와 복귀 행동만 보여 준다.

| 경로 | 데스크톱 구조 | 모바일 구조 | 주요 구성 요소 |
| --- | --- | --- | --- |
| `/party/create` | 뒤로가기 → 편집 모드 안내(해당 시) → 파티 생성 폼 | 모든 `FormRow` 1열 | `PartyCreateForm` |
| `/contests/create` | 뒤로가기 → 편집 모드 안내(해당 시) → 공모전 등록 폼 | 모든 `FormRow` 1열 | `ContestCreateForm` |
| `/exhibition/create` | 뒤로가기 → 전시 등록 폼 | 커버 업로드와 필드가 세로 배치 | `ExhibitionCreateForm` |
| `/goals/create` | 뒤로가기 → 안내 배너 → 성취 생성 폼 | 모든 입력 묶음 1열 | `GoalCreateForm` |
| `/goals/[id]/edit` | 뒤로가기 → 성취 수정 폼, 또는 오류/권한 안내 | 폼 1열, 오류 CTA는 전폭에 가깝게 배치 | `GoalEditForm` |

### 상세와 작업 공간

| 경로 | 데스크톱 구조 | 모바일 구조 | 주요 구성 요소 |
| --- | --- | --- | --- |
| `/party/[id]` | 상세 헤더(상태·좋아요·북마크·소유자 도구) → 2열: 소개·모집 포지션·팀원·댓글 / sticky 신청 패널·파티장 카드 | 본문 뒤에 신청 패널을 일반 흐름으로 배치, 1열 | `Block`, `DetailGrid`, `ApplyPanel`, `CommentThread` |
| `/party/[id]/team` | 상세 헤더 → 2열: 연동 공모전·팀원·체크리스트·PR/연동 정보 / 팀 작업 사이드 카드 | 본문과 사이드바 1열, 작업 도구는 본문 뒤 | `Checklist`, `PullRequestList`, `GithubConnectionCard` |
| `/contests/[id]` | 공모전 hero/상세 헤더 → 2열: 소개·연결 파티 보드 / 공모전 정보·파티 생성·외부 사이트·주최자 도구 사이드 카드 | 1열로 전환, 보드 카드도 1열 | `ContestDetailView`, `DetailGrid`, `PartyCard` |
| `/exhibition/[id]` | 상세 헤더(조회·좋아요·북마크) → 2열: 대표 이미지·소개·커밋 스냅샷·댓글 / 팀·프로젝트 메타 사이드 카드 | 대표 이미지와 본문 뒤에 사이드 정보, 1열 | `DetailGrid`, `CommentSection` |
| `/goals/[id]` | 단일 상세 container: 뒤로가기 → 성취 상세·출처·연결 TODO/활동·소유자 도구 | 단일 열 유지 | `GoalDetailView` |
| `/mypage/todo/[id]` | 뒤로가기 → 개인 TODO 상세/작업 공간 | 단일 열 유지 | `SoloSpaceLoader` |

### 사용자·주최자·관리

| 경로 | 데스크톱 구조 | 모바일 구조 | 주요 구성 요소 |
| --- | --- | --- | --- |
| `/mypage` | 프로필 hero → 활동 지표 → 탭 메뉴 → 탭별 본문. 탭 내부는 필요 시 본문+사이드바 또는 테이블/카드 | 프로필 요소와 지표를 세로로, 탭은 가로 스크롤 또는 줄바꿈 없이 유지해야 함, 내부 1열 | `MypageView`, `ProfileCard`, `HeroStats`, `MypageTabs` |
| `/host` | 기업 프로필 hero → 통계 행 → 2열: 등록 공모전 관리 / 기업 링크·자료·담당자 사이드바 | 1열, 공모전 행의 조작 버튼은 내용 아래로 이동 가능 | host 카드·통계·공모전 행·`SideCard` |
| `/profile/[id]` | 좁은 공개 프로필: 뒤로가기 → 프로필 카드·연락처 → 경력/수상 2열 → 공개 전시 그리드 → 외부 링크 | 경력/수상과 전시 그리드 1열 | 프로필 카드, `history-panel`, `ProjectCard` |
| `/admin` | 관리자 헤더 → 관리자 내비게이션 → KPI/차트/테이블·관리 패널 | 사이드/복수 열을 1열로, KPI는 2열 이하 | `AdminConsole` |

### 인증·법적 문서·시스템 콜백

| 경로 | 데스크톱 구조 | 모바일 구조 | 주요 구성 요소 |
| --- | --- | --- | --- |
| `/login` | 화면 중앙 auth card: 로고 → 제목/설명 → 소셜 로그인 → 구분선 → 이메일 로그인 → 가입 링크 | viewport padding만 축소, 카드 폭 100% | `LoginForm` |
| `/signup` | 화면 중앙 auth card: 로고 → 가입 폼 → 약관 동의 → 소셜/보조 링크 | 필드와 약관 블록을 세로 배치 | `SignupForm`, `ConsentGate` |
| `/forgot-password` | 화면 중앙 auth card: 단계형 비밀번호 재설정 폼 | 카드 폭 100%, 버튼 및 필드 세로 배치 | `ForgotPasswordForm` |
| `/legal/[key]` | 좁은 읽기 전용 문서: 뒤로가기 → 제목 → 전문 | 단일 열 유지 | `legal-wrap`, `legal-body` |
| `/github-app/authorized` | 일반 shell 안 중앙 시스템 피드백: 완료 제목 → 설명 → Party 복귀 CTA | 단일 열 유지 | `github-callback` |
| `/github-app/installed` | 일반 shell 안 중앙 시스템 피드백: 설치 완료 → 다음 행동 안내 → Party 복귀 CTA | 단일 열 유지 | `github-callback` |

## 4. 상세 페이지 표준 순서

상세 화면의 정보는 가능한 한 아래 순서로 배치한다.

```text
Back link (필요 시)
→ 상세 헤더: 상태 / 제목 / 핵심 메타 / 행동
→ 본문: 소개 → 핵심 데이터 → 관련 활동 → 댓글
→ 사이드바: 작성자·팀 → 신청/관리 CTA → 보조 메타
```

- 본문은 읽기와 탐색 중심, 사이드바는 행동과 요약 중심이다.
- 신청·관리처럼 문맥을 유지해야 하는 행동 패널만 데스크톱에서 sticky를 허용한다.
- 모바일에서는 사이드바를 숨기지 않고 본문 뒤 일반 흐름으로 내린다.

## 5. 페이지별 정비 원칙

- 새 목록 화면은 보드 패턴을 사용한다. 제목/설명과 primary action을 상단에 두고, 필터는 목록 바로 앞에 둔다.
- 새 생성·수정 화면은 단일 폼 패턴을 사용한다. 보드 폭 전체를 사용하지 않는다.
- 새 상세 화면은 2열 상세 패턴을 사용하되, 실제 보조 정보가 없으면 빈 사이드바를 만들지 않는다.
- 인증 및 시스템 결과 화면은 정보량을 제한하고, 하나의 명확한 다음 행동만 제공한다.
- 모바일에서 UI를 단순히 축소하지 않는다. 2열을 1열로 바꾸고, 하단 내비게이션과 버튼의 터치 영역을 보장한다.
- 홈 Hero와 홈 섹션은 각각 상하 `3rem`을 사용하며, 모바일에서는 `2.5rem`으로 축소한다. 폼은 필드 간 `1.5rem`, 같은 행 및 action 버튼 간 `0.75rem`을 기본으로 한다.
