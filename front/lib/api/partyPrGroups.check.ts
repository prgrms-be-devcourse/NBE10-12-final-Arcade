/**
 * 순수 로직 자체 점검. 의존성이 없어 그대로 돌아간다.
 *
 *   node --experimental-strip-types lib/api/partyPrGroups.check.ts
 */
import assert from 'node:assert/strict';
import { applyPullRequestToGroups, type PartyPrGroup } from './partyPrGroups.ts';
import type { PartyPullRequest } from './partyGithub';

const pr = (id: string, authorGithubUserId?: number, title = id) =>
  ({ id, authorGithubUserId, authorLogin: 'someone', title } as unknown as PartyPullRequest);

const base = (): PartyPrGroup[] => [
  { memberId: '1', memberName: '파티장', githubUserId: 11, owner: true, pullRequests: [pr('a', 11)] },
  { memberId: '2', memberName: '미연동 파티원', owner: false, pullRequests: [] },
];

// 1. 있던 PR 은 자리를 지키고 내용만 바뀐다
const updated = applyPullRequestToGroups(base(), pr('a', 11, '제목 변경'));
assert.equal(updated[0].pullRequests.length, 1);
assert.equal(updated[0].pullRequests[0].title, '제목 변경');

// 2. 새 PR 은 작성자 묶음 맨 앞에 붙는다
const added = applyPullRequestToGroups(base(), pr('b', 11));
assert.deepEqual(added[0].pullRequests.map((p) => p.id), ['b', 'a']);

// 3. 처음 보는 작성자는 외부 묶음이 새로 생긴다
const external = applyPullRequestToGroups(base(), pr('c', 99));
assert.equal(external.length, 3);
assert.equal(external[2].memberId, undefined);
assert.equal(external[2].githubUserId, 99);

// 4. 작성자 미상 PR 이 GitHub 미연동 파티원 묶음으로 새지 않는다
const unknown = applyPullRequestToGroups(base(), pr('d'));
assert.equal(unknown[1].pullRequests.length, 0);
assert.equal(unknown.length, 3);
assert.equal(unknown[2].githubUserId, undefined);

// 5. 작성자 미상이 두 건이면 같은 묶음에 모인다
const twice = applyPullRequestToGroups(unknown, pr('e'));
assert.equal(twice.length, 3);
assert.deepEqual(twice[2].pullRequests.map((p) => p.id), ['e', 'd']);

console.log('partyPrGroups: 5 checks passed');
