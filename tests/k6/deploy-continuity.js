import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const expectedVersionResponses = new Counter('expected_version_responses');
const unexpectedVersionResponses = new Counter('unexpected_version_responses');
const applicationVersionResponses = new Counter('application_version_responses');

const baseUrl = (__ENV.BASE_URL || '').replace(/\/$/, '');
const expectedVersion = __ENV.EXPECTED_VERSION || '';
const oldVersion = __ENV.OLD_VERSION || '';

export const options = {
  summaryTrendStats: ['avg', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    deploy_continuity: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.RATE || 5),
      timeUnit: '1s',
      // 실제 종료 시점은 배포 완료 후 관찰 시간이 지난 뒤 REST API로 결정한다.
      // 이 값은 배포가 비정상적으로 오래 걸릴 때를 위한 상한이다.
      duration: __ENV.MAX_DURATION || '15m',
      // 실행 중 VU 추가가 늦어 iteration을 누락하지 않도록 처음부터 준비한다.
      preAllocatedVUs: 20,
      maxVUs: 20,
      gracefulStop: '5s',
    },
  },
  thresholds: {
    http_req_failed: ['rate==0'],
    checks: ['rate==1'],
    dropped_iterations: ['count==0'],
    expected_version_responses: ['count>0'],
    unexpected_version_responses: ['count==0'],
  },
};

export function setup() {
  if (!baseUrl.startsWith('https://')) {
    throw new Error('BASE_URL must be an HTTPS origin');
  }
  if (!expectedVersion) {
    throw new Error('EXPECTED_VERSION is required');
  }
  if (!oldVersion) {
    throw new Error('OLD_VERSION is required');
  }
}

export default function () {
  const response = http.get(`${baseUrl}/api/v1/parties?size=1`, {
    tags: { test_type: 'deploy-continuity' },
    timeout: '10s',
  });
  const version = response.headers['X-App-Version'] || 'missing';
  const allowedVersion = version === oldVersion || version === expectedVersion;

  applicationVersionResponses.add(1, { version });
  expectedVersionResponses.add(version === expectedVersion ? 1 : 0);
  unexpectedVersionResponses.add(allowedVersion ? 0 : 1);

  check(response, {
    'public API returns 200': (res) => res.status === 200,
    'response version is old or new': () => allowedVersion,
  });
}
