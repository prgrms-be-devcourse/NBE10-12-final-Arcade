/**
 * 표기 정규화 자체 점검.
 *
 *   node --experimental-strip-types lib/techStacks.check.ts
 */
import assert from 'node:assert/strict';
import { TECH_STACKS, isKnownTechStack, normalizeTechStack } from './techStacks.ts';

// 1. 대소문자·공백·구두점 차이를 흡수한다
assert.equal(normalizeTechStack('java'), 'Java');
assert.equal(normalizeTechStack('  SPRING boot '), 'Spring Boot');
assert.equal(normalizeTechStack('next js'), 'Next.js');
assert.equal(normalizeTechStack('Next.js'), 'Next.js');

// 2. 다른 철자도 표준 표기로 모은다
assert.equal(normalizeTechStack('자바'), 'Java');
assert.equal(normalizeTechStack('k8s'), 'Kubernetes');
assert.equal(normalizeTechStack('golang'), 'Go');

// 3. 목록에 없으면 공백만 정리해 그대로 둔다 (2단계 - 막지 않는다)
assert.equal(normalizeTechStack('  사내   도구 '), '사내 도구');
assert.equal(isKnownTechStack('사내 도구'), false);
assert.equal(isKnownTechStack('java'), true);

// 4. 스크린샷에서 문제가 됐던 조각들은 목록 밖이라 경고가 뜬다
['아', '킬', '짜'].forEach((s) => assert.equal(isKnownTechStack(s), false, s));

// 5. 목록에 중복이 없다 (자동완성이 같은 값을 두 번 띄우지 않도록)
assert.equal(new Set(TECH_STACKS).size, TECH_STACKS.length);

console.log(`techStacks: ${TECH_STACKS.length}개, 5 checks passed`);
