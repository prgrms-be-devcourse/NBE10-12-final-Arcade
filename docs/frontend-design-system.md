# 크루온 프런트엔드 디자인 시스템

> 기준일: 2026-09-11  
> 적용 범위: `front/app`, `front/components`  
> 목적: 새 화면과 컴포넌트가 동일한 시각 언어, 상태 표현, 접근성 규칙을 따르도록 하는 구현 기준

## 1. 디자인 원칙

크루온의 UI는 **네온 아케이드**를 현대적인 협업 플랫폼에 맞게 해석한다.

1. 콘텐츠가 효과보다 먼저 읽혀야 한다.
2. 게임성은 색, 픽셀 레이블, 하드 섀도, 상태 게이지에 사용하고 본문 가독성을 해치지 않는다.
3. 동일한 의미는 어느 화면에서나 같은 색, 형태, 상호작용을 사용한다.
4. 테마는 색만 바꾸는 것이 아니라, 다크의 네온과 라이트의 스티커형 대비를 각각 일관되게 유지한다.
5. 키보드, 축소 모션, 오류·비활성 상태도 기본 상태와 동등하게 설계한다.

## 2. 테마와 디자인 토큰

토큰의 단일 진실 원천은 `front/app/styles/tokens.css`의 `:root`와 `[data-theme="light"]`이다. `front/app/globals.css`는 이 파일을 포함한 스타일의 import 순서만 관리한다. 컴포넌트와 페이지 CSS에서는 가능한 한 아래 의미 토큰을 사용하고, 개별 색상값을 새로 추가하지 않는다.

| 역할 | 토큰 | 다크 | 라이트 |
| --- | --- | --- | --- |
| 앱 배경 | `--bg` | `#0a0a13` | `#ece9fb` |
| 기본 면 | `--surface` | `#14141f` | `#ffffff` |
| 보조 면 | `--surface-2` | `#1b1b2b` | `#f3f0ff` |
| 일반 테두리 | `--border` | `#26263a` | `#c8c1ef` |
| 강조 테두리 | `--border-strong` | `#34344c` | `#241d4a` |
| 본문 텍스트 | `--text` | `#f2f3f8` | `#171238` |
| 보조 텍스트 | `--text-muted` | `#a6a6bd` | `#453e6e` |
| 약한 텍스트 | `--text-dim` | `#6f6f89` | `#7a72a6` |
| 주요 행동/활성 | `--accent` | `#31e9d2` | `#c81368` |
| 주요 행동 hover | `--accent-strong` | `#7bf7e8` | `#8a0d49` |
| 강조 배경 | `--accent-soft` | 민트 12% | 핑크 14% |
| 보상/주의 | `--gold` | `#f5c451` | `#c8791a` |
| 카드 섀도 | `--shadow-card` | 검정 하드 섀도 | 인디고 하드 섀도 |
| 위험 | `--color-danger` | `#ff6b7a` | `#ba244f` |
| 성공 | `--color-success` | 민트 | 핑크/인디고 계열 |
| 경고 | `--color-warning` | 골드 | 브라운 오렌지 |
| 오버레이 | `--overlay` | 짙은 남색 78% | 인디고 45% |
| 프레임 안쪽선 | `--frame-dark` | `#07060f` | `#241d4a` |

### 테마 사용 규칙

- 다크는 기본 테마다. 어두운 남색 배경 위에 민트 네온을 주요 행동·현재 상태에 사용한다.
- 라이트는 보라/화이트 면, 인디고 테두리, 핑크 accent를 사용한다. 라이트에서 다크의 민트값을 직접 쓰지 않는다.
- 실제 테마 값은 `html[data-theme]`만 변경한다. `ThemeToggle`이 이 속성과 localStorage를 관리한다.
- `--accent`는 CTA, 선택, 포커스, 진행 상태에만 사용한다. 장식용으로 과도하게 쓰지 않는다.
- `--gold`는 수상, 성취, 주의에 제한한다. 삭제·실패에는 사용하지 않는다.

## 3. 타이포그래피

| 역할 | 폰트 | 용도 |
| --- | --- | --- |
| Display | PF Stardust | 페이지/섹션 제목, 버튼, 탭, 주요 메뉴 |
| Body | Gothic A1 | 본문, 설명, 폼, 목록 |
| Pixel/Mono | Press Start 2P | D-day, 짧은 수치, 시스템성 레이블 |

### 계층

- 페이지 제목: Display, `clamp(1.5rem, 2.6vw, 2rem)` 이상
- 상세 제목: Body 굵게 또는 Display, `1.4rem`~`1.9rem`
- 카드 제목: Body 800, 대체로 `1.05rem`
- 본문: Body, `0.84rem`~`1rem`, 줄간격 `1.5`~`1.75`
- 보조 설명: `--text-muted` 또는 `--text-dim`, `0.72rem`~`0.86rem`
- 픽셀 폰트는 긴 한글 문단에 사용하지 않는다.

## 4. 레이아웃과 간격

| 항목 | 기준 |
| --- | --- |
| 콘텐츠 최대 폭 | `73.75rem` (1180px) |
| 기본 페이지 좌우 패딩 | `1.5rem` |
| 데스크톱 상단 내비게이션 | 높이 `4.5rem` |
| 홈 랜딩 섹션 여백 | 상하 `3rem` (모바일 `2.5rem`) |
| 일반 섹션 여백 | 상하 `4rem` |
| 보드/상세 페이지 여백 | 상단 약 `3rem`, 하단 `4rem`~`4.5rem` |
| 카드 내부 패딩 | 대체로 `1rem`~`1.5rem` |
| 작은 요소 간격 | `0.375rem`~`0.625rem` |
| 일반 묶음 간격 | `0.75rem`~`1rem` |
| 큰 묶음 간격 | `1.5rem`~`2.75rem` |

네온 아케이드 skin의 프레임 언어를 유지하기 위해 현재 `--radius-card`, `--radius-chip`, `--radius-input`은 모두 `0`이다. 면으로 인지되는 카드·패널·테이블·모달은 `--radius-card`를, 버튼·태그·칩·아이콘 버튼은 `--radius-chip`을, 입력 요소는 `--radius-input`을 사용한다. 반경 값을 바꿀 때에는 세 토큰과 `/design-system` 전시를 함께 검토한다.

### 공통 spacing scale

여백의 단일 기준은 `styles/tokens.css`의 `--space-*`와 `--page-gutter`다. 화면 고유의 시각 보정 외에는 아래 값만 사용한다.

| 토큰 | 값 | 용도 |
| --- | --- | --- |
| `--space-1` ~ `--space-3` | `0.375rem` / `0.5rem` / `0.625rem` | 아이콘·태그·짧은 메타 간격 |
| `--space-4` ~ `--space-6` | `0.75rem` / `1rem` / `1.5rem` | 버튼 묶음, 카드 내부, 폼 필드 |
| `--space-7` ~ `--space-9` | `2rem` / `2.75rem` / `3rem` | 레이아웃 열, 반복 블록, 홈 섹션 |
| `--space-10` ~ `--space-11` | `4rem` / `4.5rem` | 일반 섹션·보드 하단 여백 |
| `--space-section-mobile` | `2.5rem` | 모바일 홈 Hero·섹션의 세로 여백 |
| `--page-gutter` | `1.5rem` (모바일 `1rem`) | container와 헤더의 좌우 여백 |

- `DetailGrid`의 열 간격과 `SideCard`의 내부/반복 간격은 `--layout-gap`과 `--space-6`으로 통일한다.
- 폼 필드 간격은 `--space-6`, 같은 행의 열 간격과 action 버튼 묶음은 `--space-4`를 사용한다.

### 반응형 원칙

- 넓은 화면: 보드 3열, 상세/마이페이지 본문+사이드바 2열을 허용한다.
- 약 `64rem` 이하: 본문+사이드바를 1열로 전환한다.
- 약 `53.75rem` 이하: 헤더의 가로 메뉴를 숨기고 하단 모바일 내비게이션을 사용한다.
- 약 `40rem` 이하: 폼 행과 작은 카드 그리드를 1열로 전환한다.
- `prefers-reduced-motion: reduce`에서는 장식 애니메이션과 reveal 전환을 제거한다.

새 컴포넌트는 기존의 여러 임의 break point를 늘리지 말고, 위 세 단계(데스크톱 / 태블릿 / 모바일)를 우선 사용한다.

## 5. 컴포넌트 규약

### 버튼

| Variant | 클래스/API | 용도 |
| --- | --- | --- |
| Primary | `.btn.btn-primary`, `Button` | 생성, 저장, 신청, 확인 등 주요 행동 |
| Ghost | `.btn.btn-ghost`, `Button variant="ghost"` | 취소, 보조 행동, 되돌아가기 |
| Danger | `.btn.is-danger` | 삭제와 되돌릴 수 없는 행동. 확인 다이얼로그를 동반한다. |
| Icon | `.icon-btn` | 레이블 없이 아이콘만 쓰는 조작. 반드시 `aria-label`을 제공한다. |

- 모든 버튼은 최소한 default, hover, focus-visible, active, disabled 상태를 가진다.
- primary는 한 화면 또는 모달의 핵심 행동 하나에 우선 사용한다.
- 로딩 중에는 중복 제출이 되지 않게 disabled 처리하고, 레이블에 진행 상태를 알린다.

### 카드와 패널

- 카드: `--surface` 배경, `--border` 테두리, `--radius-card`, 하드 섀도를 기본으로 한다.
- hover가 가능한 카드만 가벼운 상승과 강조 테두리/글로우를 제공한다. 단순 정보 패널에는 hover 효과를 강제하지 않는다.
- 대표 이미지는 카드 상단 4:3 비율을 기본으로 하며, 카드 본문은 카테고리 → 제목 → 보조 정보/지표 순서로 배치한다.
- 상세 페이지의 반복 섹션에는 `Block`, 보조 정보에는 `SideCard`, 2열 상세 구조에는 `DetailGrid`를 사용한다.

### 폼

- `FormGroup` + `TextField`/`SelectField`/`TextAreaField` 조합을 기본으로 한다.
- 라벨, 필수 표시, 힌트 또는 오류 메시지를 항상 같은 그룹에 둔다.
- 오류는 `has-error`와 `form-field-error`로 표시한다. 오류 색만으로 전달하지 말고 이유를 텍스트로 쓴다.
- 포커스는 accent 테두리와 명확한 outline/glow를 사용한다.

### 태그·칩·상태

| 요소 | 용도 |
| --- | --- |
| `Tag` | 분야, 기술, 분류 |
| `SkillChip` | 보유 기술 및 편집 가능한 기술 목록 |
| `StatusPill` | 진행·검토·대기 같은 상태 |
| `SlotChip` | 포지션별 정원/마감 |
| `SourceBadge` | 성취 출처 |
| `DDay` | 마감까지의 짧은 시간 정보 |

- 칩은 짧은 값만 담고, 문장형 설명이나 핵심 CTA에 사용하지 않는다.
- 상태는 색뿐 아니라 레이블로도 판별 가능해야 한다.
- 새 상태가 필요하면 임의 색상 대신 semantic variant를 먼저 정의한다.

### 데이터 표시와 오버레이

- 반복 데이터는 `DataTable`, 페이지 이동은 `Pagination`, 진행률은 `ProgressBar`를 우선 사용한다.
- 확인/입력 오버레이는 `Modal`과 `ConfirmDialog`를 사용한다.
- 삭제는 `DeleteButton`처럼 확인 → 처리 중 → 성공 이동/실패 메시지 흐름을 따른다.

## 6. 상태와 피드백

| 상태 | 표현 원칙 |
| --- | --- |
| 선택/활성 | accent 배경 또는 테두리 + 명시적 활성 클래스 |
| hover | 색/테두리/섀도의 작은 변화. 레이아웃 점프 금지 |
| focus | accent outline. 마우스 hover와 구별 가능해야 함 |
| disabled | 불투명도만 낮추지 말고 클릭 불가 커서와 행동 불가 이유를 필요 시 제공 |
| loading | 행동을 잠그고 버튼 또는 해당 영역에 진행 문구 표시 |
| success | 짧은 성공 문구와 긍정적 surface/accent 조합 |
| error | 오류 문구, 연결된 필드 강조, 재시도 가능한 다음 행동 |
| destructive | danger 스타일 + 확인 다이얼로그 |

## 7. 접근성 기준

- 키보드로 도달 가능한 모든 인터랙티브 요소에 `:focus-visible`을 제공한다.
- 아이콘만 있는 버튼은 의미 있는 `aria-label`을 제공한다.
- 선택 토글에는 `aria-pressed`, 현재 메뉴에는 `aria-current="page"`를 사용한다.
- 필드 오류는 `aria-invalid`와 `aria-describedby`로 입력 요소에 연결한다.
- 모달은 열릴 때 제목 또는 첫 조작 요소로 포커스를 옮기고, 열려 있는 동안 포커스가 모달 밖으로 나가지 않게 한다.
- 자동 재생·장식 애니메이션은 축소 모션 환경에서 멈춰야 한다.

## 8. 구현 위치와 사용 우선순위

| 영역 | 기준 파일 |
| --- | --- |
| 전역 토큰 | `front/app/styles/tokens.css` |
| 전역 스타일 import 순서 | `front/app/globals.css` |
| 폰트·테마 초기화 | `front/app/layout.tsx` |
| 테마 전환 | `front/components/layout/ThemeToggle.tsx` |
| 공통 UI API | `front/components/ui/` |
| 헤더·푸터·모바일 내비게이션 | `front/components/layout/` |

새 화면을 만들 때에는 `globals.css`에 페이지 전용 전역 클래스를 추가하기 전에 공통 UI 컴포넌트를 확장할 수 있는지 검토한다. 공통화할 수 없는 화면 고유 스타일만 페이지 또는 기능 단위 스타일로 둔다.

### 스타일 소유권

`front/app/globals.css`는 import 순서만 관리한다. 스타일은 아래 소유자 파일에 둔다.

| 파일 | 소유 영역 |
| --- | --- |
| `styles/tokens.css`, `base.css` | 테마 토큰, reset, 전역 shell과 container |
| `styles/layout.css` | 헤더·푸터·모바일 navigation 같은 shell layout |
| `styles/primitives.css` | Button, Card, Notice, 입력·상태 primitive |
| `styles/pages.css` | 홈·보드·상세 등 페이지 패턴 |
| `styles/features.css` | 인증, 관리자, 팀 작업공간 등 기능 UI |
| `styles/skin.css`, `overrides.css` | 아케이드 skin과 호환 보정. 새 selector는 다른 소유자 파일에 우선 추가 |

내부 `/design-system`에서 지원하는 공통 UI의 variant와 테마 표현을 확인한다. 토큰·공통 API 변경 시에는 이 페이지, 본 문서, TODO 상태를 함께 갱신한다.

페이지별 공통 shell, 화면 구성 순서, 데스크톱·모바일 전환은 [frontend-page-layouts.md](./frontend-page-layouts.md)를 기준으로 한다.
