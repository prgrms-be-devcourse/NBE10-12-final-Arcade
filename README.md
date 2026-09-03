# 소개

프로그래머스 벡엔드 최종 프로젝트 5팀 Team Arcade입니다.

# GitHub App 설정

GitHub 저장소 연동 기능을 사용하려면 GitHub App을 생성한 뒤 다음 항목을 설정해야 합니다.

- `Permissions & events`의 Repository permissions
  - `Metadata`: Read-only
  - `Contents`: Read-only
  - `Pull requests`: Read-only
- 필요한 경우 `Pull request` webhook 이벤트를 활성화합니다.
- GitHub App 설정의 `Post installation → Setup URL`에는 설치 완료 콜백을 받을 **외부에서 접근 가능한 백엔드 주소**를 직접 등록합니다. 프론트 환경변수나 백엔드 설정이 이 값을 자동으로 등록하지는 않습니다.
- 설치 화면에서 `Only select repositories`를 선택하고 연동할 파티 저장소를 포함합니다.
- 외부 환경에서 설치 후 복귀하려면 Setup URL과 웹훅 URL 모두 GitHub에서 접근 가능한 HTTPS 주소여야 합니다.

설치 흐름에서 각 URL의 역할은 다음과 같습니다.

- `Setup URL`: GitHub App 설정에 등록하는 백엔드 콜백 주소입니다. GitHub가 설치 완료 후 `state`와 `installation_id`를 붙여 호출합니다.
- `Webhook URL`: GitHub App 설정에 등록하는 백엔드 이벤트 수신 주소입니다. PR 이벤트를 `/api/v1/github/webhook`으로 받습니다.
- 프론트의 `redirectUrl`: 프론트가 설치 시작 API에 전달하는 내부 경로(`/party/{id}/team`)입니다. 백엔드는 설치 state와 함께 저장하고, Setup URL 처리가 끝난 뒤 이 경로로 프론트를 리다이렉트합니다.
- `CUSTOM__FRONTEND__BASE_URL`: 백엔드가 위 내부 경로를 완성할 때 사용하는 프론트 기본 주소입니다.

로컬에서 GitHub 웹훅과 설치 콜백을 테스트할 때는 백엔드를 먼저 실행한 뒤 `ngrok http 8080`을 실행합니다. 예를 들어 ngrok 주소가 `https://abc.ngrok-free.app`이면 GitHub App 설정에 다음을 입력합니다.

- Webhook URL: `https://abc.ngrok-free.app/api/v1/github/webhook`
- Setup URL: `https://abc.ngrok-free.app/api/v1/github-app/setup`

ngrok의 무료 주소는 재실행할 때 바뀔 수 있으므로 GitHub App 설정도 함께 갱신해야 합니다. 운영 환경에서는 ngrok 대신 배포된 API의 HTTPS 도메인을 사용합니다.

백엔드 `back/.env`에는 GitHub App의 `Private keys → Generate a private key`로 내려받은 PEM 개인키 전체를 설정합니다.

```properties
CUSTOM__GITHUB__APP__ID=GitHub_App_ID
CUSTOM__GITHUB__APP__SLUG=GitHub_App_slug
CUSTOM__GITHUB__APP__PRIVATE_KEY=-----BEGIN RSA PRIVATE KEY-----\n...\n-----END RSA PRIVATE KEY-----
CUSTOM__GITHUB__WEBHOOK__SECRET=webhook_secret
```

PEM 파일을 한 줄 환경변수로 변환할 때는 다음 명령을 사용할 수 있습니다.

```bash
awk 'NF {sub(/\r/, ""); printf "%s\\n", $0;}' {다운로드된_파일}.pem
```

출력 결과를 `CUSTOM__GITHUB__APP__PRIVATE_KEY` 값으로 사용합니다. `CUSTOM__GITHUB__WEBHOOK__SECRET`은 임의의 문자열로 정하고, GitHub App 설정의 Webhook secret에도 동일한 값을 입력합니다.

개인키 파일과 실제 secret 값은 저장소에 커밋하지 않습니다.
