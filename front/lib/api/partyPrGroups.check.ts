/**
 * 순수 로직 자체 점검. 의존성이 없어 그대로 돌아간다.
 *
 *   node --experimental-strip-types lib/api/partyPrGroups.check.ts
 */
import assert from 'node:assert/strict';
import { replacePrGroup, type PartyPrGroup } from './partyPrGroups.ts';
import type { PartyPullRequest } from './partyGithub';

const pr = (id: string) => ({ id }) as unknown as PartyPullRequest;

const base = (): PartyPrGroup[] => [
  { memberId: '1', memberName: '파티장', githubUserId: 11, owner: true, pullRequests: [pr('a')] },
  { memberId: '2', memberName: '미연동 파티원', owner: false, pullRequests: [] },
];

// 1. 있던 작성자는 자리를 지킨 채 PR 목록만 갈린다
const updated = replacePrGroup(base(), {
  memberId: '1', memberName: '파티장', githubUserId: 11, owner: true, pullRequests: [pr('b'), pr('a')],
});
assert.equal(updated.length, 2);
assert.equal(updated[0].memberName, '파티장');
assert.deepEqual(updated[0].pullRequests.map((p) => p.id), ['b', 'a']);

// 2. 처음 보는 외부 작성자는 뒤에 붙는다
const external = replacePrGroup(base(), {
  githubUserId: 99, githubLogin: 'someone', owner: false, pullRequests: [pr('c')],
});
assert.equal(external.length, 3);
assert.equal(external[2].memberId, undefined);
assert.equal(external[2].githubLogin, 'someone');

// 3. 작성자 미상 묶음이 GitHub 미연동 파티원 자리로 새지 않는다
const unknown = replacePrGroup(base(), { owner: false, pullRequests: [pr('d')] });
assert.equal(unknown[1].pullRequests.length, 0);
assert.equal(unknown.length, 3);

// 4. 작성자 미상이 다시 오면 새 묶음이 아니라 그 자리를 갱신한다
const twice = replacePrGroup(unknown, { owner: false, pullRequests: [pr('e'), pr('d')] });
assert.equal(twice.length, 3);
assert.deepEqual(twice[2].pullRequests.map((p) => p.id), ['e', 'd']);

console.log('partyPrGroups: 4 checks passed');
