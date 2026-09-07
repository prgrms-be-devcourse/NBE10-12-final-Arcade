# GitHub OAuth App 설정

GitHub 로그인과 GitHub 계정 연동을 사용하려면 아래 순서로 OAuth App을 설정합니다. 저장소와 PR을 연동하는 [GitHub App](github-app.md)과는 별도의 앱입니다.

## 1. OAuth App 생성

GitHub의 `Settings → Developer settings → OAuth Apps → New OAuth App`에서 앱을 생성합니다.

| 항목 | 설정값 |
| --- | --- |
| `Application name` | 사용자가 GitHub 인증 화면에서 알아볼 수 있는 서비스 이름 |
| `Homepage URL` | 프론트엔드 공개 주소. 예: `http://crewon.localhost` 또는 `https://app.example.com` |
| `Application description` | 선택 사항 |
| `Authorization callback URL` | `{서비스 공개 주소}/login/oauth2/code/github` |

로컬 Docker Compose 환경의 기본 콜백 URL은 다음과 같습니다.

```text
http://crewon.localhost/login/oauth2/code/github
```

프론트엔드와 백엔드를 각각 실행해 백엔드에 직접 접속하는 경우에는 다음 주소를 사용합니다.

```text
http://localhost:8080/login/oauth2/code/github
```

운영 환경에서는 사용자가 접속하는 공개 HTTPS 주소를 기준으로 등록합니다. 예를 들어 서비스 주소가 `https://app.example.com`이면 콜백 URL은 `https://app.example.com/login/oauth2/code/github`입니다. 프록시가 `/oauth2/*`와 `/login/oauth2/*` 요청을 백엔드로 전달해야 합니다.

GitHub OAuth App에는 콜백 URL을 하나만 등록할 수 있으므로 로컬과 운영 환경은 OAuth App을 따로 만드는 것을 권장합니다.

## 2. Client ID와 Client Secret 발급

생성된 OAuth App 설정 화면에서 `Client ID`를 확인하고 `Generate a new client secret`을 눌러 Client secret을 발급합니다. Client secret은 생성 직후 안전한 곳에 보관하고 저장소에 커밋하지 않습니다.

## 3. 백엔드 환경변수 입력

[back/.env.example](../back/.env.example)을 `back/.env`로 복사하고 발급받은 값을 입력합니다.

```dotenv
SPRING__SECURITY__OAUTH2__CLIENT__REGISTRATION__GITHUB__CLIENT_ID=발급받은_Client_ID
SPRING__SECURITY__OAUTH2__CLIENT__REGISTRATION__GITHUB__CLIENT_SECRET=발급받은_Client_Secret
```

Docker Compose로 실행한다면 루트의 [.env.local.example](../.env.local.example)을 `.env.local`로 복사한 뒤 같은 두 환경변수에 값을 입력합니다. 운영 배포에서는 [.env.prod.example](../.env.prod.example)의 같은 항목을 기준으로 배포 환경변수를 설정합니다.

값을 변경한 뒤에는 백엔드를 재시작합니다.

## 4. 동작 확인

브라우저에서 다음 로그인 시작 주소로 접속합니다.

```text
{서비스 공개 주소}/oauth2/authorization/github
```

GitHub 인증을 마치면 GitHub가 1단계에서 등록한 `/login/oauth2/code/github`로 사용자를 돌려보냅니다. 로그인 요청에 `redirectUrl` 쿼리 파라미터가 있으면 인증 완료 후 해당 프론트엔드 경로로 이동합니다.

콜백 URL 오류가 발생하면 다음 항목을 확인합니다.

- GitHub OAuth App의 `Authorization callback URL`과 실제 요청의 공개 스킴(`http`/`https`), 호스트, 포트가 같은지 확인합니다.
- 리버스 프록시가 `Host`, `X-Forwarded-Host`, `X-Forwarded-Proto` 헤더를 백엔드에 전달하는지 확인합니다.
- Client ID와 Client secret이 같은 OAuth App에서 발급된 값인지 확인합니다.
- 환경변수를 변경한 뒤 백엔드를 재시작했는지 확인합니다.

Client secret과 로그인 과정에서 발급되는 토큰은 저장소, 로그, 브라우저 코드에 노출하지 않습니다.
