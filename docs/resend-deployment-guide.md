# Resend 비밀번호 재설정 메일 배포 가이드

## 목적

비밀번호 재설정 메일은 Resend HTTP API로 발송한다. 로컬에서는 `onboarding@resend.dev`를 이용해 제한된 발송 테스트를 할 수 있고, 운영에서는 Resend에서 검증한 팀 도메인 또는 하위 도메인을 발신자로 사용한다.

## 운영 전 준비

### 1. 발송 도메인 결정

- 팀이 소유하고 DNS를 관리할 수 있는 도메인을 선택한다.
- 기존 수신 메일(MX) 설정과 충돌을 피하기 위해 `mail.example.com` 같은 하위 도메인을 권장한다.
- Resend 대시보드의 Domains에서 해당 도메인 또는 하위 도메인을 추가한다.

### 2. Cloudflare DNS 검증

Resend 대시보드에서 Cloudflare 자동 연결을 승인하거나, 표시되는 DNS 레코드를 Cloudflare에 수동으로 추가한다.

- Resend가 제시한 MX, SPF(TXT), DKIM 레코드를 값까지 정확히 복사한다.
- DKIM CNAME/TXT 레코드는 Cloudflare에서 `DNS Only`로 설정한다.
- DNS 반영 뒤 Resend 도메인 상태가 `verified`인지 확인한다.
- Resend 가이드: [Cloudflare 설정](https://resend.com/docs/knowledge-base/cloudflare), [도메인 관리](https://resend.com/docs/dashboard/domains/introduction)

### 3. API 키 생성

- Resend API Keys에서 새 키를 생성한다.
- 권한은 `Sending access`를 선택한다.
- 가능하면 검증한 발송 도메인으로 키 범위를 제한한다.
- 키는 생성 화면에서 한 번만 표시되므로 배포 환경의 secret에 저장한다. 저장소, 배포 로그, 채팅에 공유하지 않는다.

## 배포 환경 변수

아래 값은 배포 플랫폼의 secret/environment 설정에 등록한다.

```dotenv
RESEND_API_KEY=re_xxxxxxxxx
CUSTOM__MAIL__ENABLED=true
CUSTOM__MAIL__FROM=Arcade <no-reply@mail.example.com>
CUSTOM__FRONTEND__BASE_URL=https://app.example.com
```

주의 사항:

- `CUSTOM__MAIL__FROM`의 도메인은 Resend에서 검증된 도메인 또는 동일한 하위 도메인이어야 한다.
- `CUSTOM__MAIL__ENABLED=true`인데 `RESEND_API_KEY`가 비어 있으면 애플리케이션은 시작하지 않는다.
- `CUSTOM__FRONTEND__BASE_URL`은 HTTPS 프론트엔드 주소여야 하며, 메일의 `/password/reset?token=...` 링크 생성에 사용된다.
- `CUSTOM__MAIL__RESEND__API_BASE_URL`은 기본값 `https://api.resend.com`을 사용한다. 테스트 대역을 연결할 때만 변경한다.

## 로컬 발송 테스트

```dotenv
RESEND_API_KEY=re_xxxxxxxxx
CUSTOM__MAIL__ENABLED=true
CUSTOM__MAIL__FROM=Arcade <onboarding@resend.dev>
CUSTOM__FRONTEND__BASE_URL=http://localhost:3000
```

`resend.dev` 발신은 Resend가 허용한 테스트 수신자에게만 발송할 수 있다. 일반 회원 이메일 발송은 검증 도메인 설정 후에 가능하다.

## 배포 후 확인

1. 활성 테스트 회원으로 `POST /api/v1/members/password/reset-requests`를 호출한다.
2. 지정한 발신 주소와 제목 `[Arcade] 비밀번호 재설정 안내`로 메일이 수신되는지 확인한다.
3. 링크가 운영 프론트엔드의 HTTPS 주소로 이동하는지 확인한다.
4. 새 비밀번호 설정 후 기존 비밀번호 로그인이 실패하고, 새 비밀번호 로그인이 성공하는지 확인한다.
5. 기존 refresh token이 더 이상 사용할 수 없는지 확인한다.
6. Resend 대시보드의 이메일 이벤트에서 delivered/bounced 상태를 확인한다.

## 장애 대응 및 롤백

- API 키 또는 도메인 검증 오류로 발송이 실패해도 외부 API는 회원 열거 방지를 위해 동일한 성공 응답을 반환한다.
- 서버 로그에는 회원 ID와 실패 이벤트만 남으며 이메일, 토큰, 재설정 URL은 기록하지 않는다.
- 긴급히 발송을 중단해야 하면 `CUSTOM__MAIL__ENABLED=false`로 배포한다. 이 경우 API는 유지되지만 실제 메일은 보내지지 않는다.
- 도메인 불일치 오류가 나면 `CUSTOM__MAIL__FROM`이 Resend에서 검증한 도메인과 정확히 일치하는지 먼저 확인한다.
