# GitHub App 설정

GitHub 저장소와 PR을 연동하려면 아래 순서로 GitHub App을 설정합니다.

## 1. GitHub App 생성

GitHub의 `Settings → Developer settings → GitHub Apps → New GitHub App`에서 App을 생성합니다. App을 만든 뒤 설정 화면에서 `App ID`, `Client ID`, App URL의 slug를 확인합니다.

## 2. 콜백 URL 설정

외부에서 접근 가능한 백엔드 주소를 준비합니다. 운영에서는 API의 HTTPS 도메인을 사용하고, 로컬 테스트에서는 ngrok 주소를 사용합니다.

`Request user authorization (OAuth) during installation`을 활성화하는 경우:

- `Callback URL`: `{백엔드 주소}/api/v1/github-app/user/callback`
- `CUSTOM__GITHUB__APP__USER_AUTHORIZATION_CALLBACK_URL`에도 같은 주소를 입력합니다.

이 옵션을 사용하지 않는 경우:

- `Post installation → Setup URL`: `{백엔드 주소}/api/v1/github-app/setup`
- 사용자 인증을 사용한다면 `Callback URL`과 `CUSTOM__GITHUB__APP__USER_AUTHORIZATION_CALLBACK_URL`은 위와 같이 설정합니다.

## 3. 권한과 웹훅 설정

`Permissions & events`에서 다음 Repository permissions를 모두 `Read-only`로 설정합니다.

- `Metadata`
- `Contents`
- `Pull requests`

Webhook을 활성화하고 URL에 `{백엔드 주소}/api/v1/github/webhook`을 입력합니다. 이벤트는 `Pull request`를 구독합니다. 설치할 때 `Only select repositories`를 선택했다면 연동할 파티 저장소를 포함합니다.

## 4. 인증 정보 발급

GitHub App 설정에서 다음 정보를 발급합니다.

- `Client secrets → Generate a new client secret`에서 Client secret을 생성합니다.
- `Private keys → Generate a private key`에서 PEM 개인키를 생성합니다.
- Webhook secret에는 임의의 문자열을 입력합니다. 예: `openssl rand -hex 32`
- 사용자 토큰 암호화 키를 생성합니다: `openssl rand -base64 32`

## 5. 백엔드 환경변수 입력

[back/.env.example](../back/.env.example)을 `back/.env`로 복사하고 값을 채웁니다.

| 환경변수 | 채울 값 |
| --- | --- |
| `CUSTOM__GITHUB__APP__ID` | GitHub App 설정의 숫자 `App ID` |
| `CUSTOM__GITHUB__APP__SLUG` | App URL `https://github.com/apps/{slug}`의 `{slug}` 부분 |
| `CUSTOM__GITHUB__APP__CLIENT_ID` | GitHub App 설정 화면의 `Client ID` |
| `CUSTOM__GITHUB__APP__CLIENT_SECRET` | 생성한 Client secret |
| `CUSTOM__GITHUB__APP__USER_AUTHORIZATION_CALLBACK_URL` | 2단계의 Callback URL과 같은 주소 |
| `CUSTOM__GITHUB__APP__TOKEN_ENCRYPTION_KEY` | `openssl rand -base64 32`의 출력값 |
| `CUSTOM__GITHUB__APP__PRIVATE_KEY` | 생성한 PEM 개인키 전체 |
| `CUSTOM__GITHUB__WEBHOOK__SECRET` | GitHub App Webhook secret과 같은 값 |

PEM 파일은 다음 명령으로 한 줄 환경변수 값으로 변환합니다.

```bash
awk 'NF {sub(/\r/, ""); printf "%s\\n", $0;}' downloaded-private-key.pem
```

출력 결과 전체를 `CUSTOM__GITHUB__APP__PRIVATE_KEY`에 넣습니다. `CUSTOM__GITHUB__APP__TOKEN_ENCRYPTION_KEY`는 기존 사용자 토큰을 복호화하는 데 쓰이므로 배포 후에도 변경하지 않습니다.

## 6. 로컬 테스트

백엔드를 8080 포트로 실행한 뒤 `ngrok http 8080`을 실행합니다. ngrok 주소가 `https://abc.ngrok-free.app`이면 2·3단계의 `{백엔드 주소}`와 환경변수 값을 다음 주소로 바꿉니다.

- Callback URL: `https://abc.ngrok-free.app/api/v1/github-app/user/callback`
- Setup URL: `https://abc.ngrok-free.app/api/v1/github-app/setup`
- Webhook URL: `https://abc.ngrok-free.app/api/v1/github/webhook`

ngrok 주소가 바뀌면 GitHub App에 등록한 URL과 `CUSTOM__GITHUB__APP__USER_AUTHORIZATION_CALLBACK_URL`을 함께 갱신한 뒤 백엔드를 재시작합니다. `.env`, PEM 개인키, Client secret, token encryption key, Webhook secret은 저장소에 커밋하지 않습니다.
