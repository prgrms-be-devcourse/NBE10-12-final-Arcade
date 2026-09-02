'use client';

import { useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { FormActions, FormGroup, FormRow, TextAreaField, TextField } from '@/components/ui/Field';
import { RadioChipGroup } from '@/components/ui/RadioChipGroup';
import { Button } from '@/components/ui/Button';
import { ApiError } from '@/lib/api';
import { createGoal, fetchTodos, type CreateGoalPayload } from '@/lib/api';
import { GOAL_STATUSES, GOAL_STATUS_LABELS, GOAL_TYPE_LABELS } from '@/lib/constants';
import type { GoalStatus, TodoItem } from '@/lib/types';

/** 자기신고로 등록할 수 있는 유형. PROJECT 는 파티 확정 시 시스템이 만든다(400-4) */
const SELF_REPORTED_TYPES = ['CONTEST', 'CHECKLIST'] as const;
type SelfReportedType = (typeof SELF_REPORTED_TYPES)[number];

const TEAM_OPTIONS = ['팀 참가', '개인 참가'] as const;

/** 빈 문자열은 보내지 않는다 — 서버에 빈 값이 그대로 저장되지 않게 */
function trimmed(value: string): string | undefined {
  const next = value.trim();
  return next === '' ? undefined : next;
}

/**
 * 성취 자기신고 등록 폼.
 *
 * 서버가 받는 값(GoalDetailReqBody)만 담는다 — 대회 링크·증빙 파일은 아직 요청 DTO 에 없어서
 * 여기서 받지 않는다. 등록은 최소 항목으로 끝내고 나머지는 수정 화면에서 채우는 흐름이다.
 */
export function GoalCreateForm() {
  const router = useRouter();

  const [type, setType] = useState<SelfReportedType>('CONTEST');
  const [status, setStatus] = useState<GoalStatus>('WANT');

  // CONTEST
  const [contestName, setContestName] = useState('');
  const [teamLabel, setTeamLabel] = useState<string>(TEAM_OPTIONS[1]);
  const [result, setResult] = useState('');
  const [awardDate, setAwardDate] = useState('');

  // CHECKLIST
  const [title, setTitle] = useState('');
  const [memo, setMemo] = useState('');
  const [targetDate, setTargetDate] = useState('');

  // 연결할 개인 TODO. 파티 등록의 공모전 선택과 같은 구성이다
  const [todos, setTodos] = useState<TodoItem[]>([]);
  const [todoKeyword, setTodoKeyword] = useState('');
  const [pickedTodo, setPickedTodo] = useState<TodoItem | null>(null);

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitError, setSubmitError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const isContest = type === 'CONTEST';

  // 내 TODO 는 많아야 수십 건이라 한 번 읽어두고 검색은 화면에서 거른다
  useEffect(() => {
    if (isContest) return;
    let alive = true;
    fetchTodos()
      .then((rows) => {
        if (alive) setTodos(rows);
      })
      .catch(() => {
        if (alive) setTodos([]);
      });
    return () => {
      alive = false;
    };
  }, [isContest]);

  const todoResults = useMemo(() => {
    const keyword = todoKeyword.trim().toLowerCase();
    const rows = keyword
      ? todos.filter((todo) => todo.title.toLowerCase().includes(keyword))
      : todos;
    return rows.slice(0, 8);
  }, [todos, todoKeyword]);

  /** 고른 TODO 의 제목을 비어 있을 때만 채운다. 이미 적은 제목을 덮지 않는다 */
  const pickTodo = (todo: TodoItem) => {
    setPickedTodo(todo);
    if (!title.trim()) {
      setTitle(todo.title);
      setErrors((prev) => ({ ...prev, title: '' }));
    }
  };

  // 서버가 타입별로 요구하는 필수 항목과 같은 규칙 (400-4)
  const validate = () => {
    const next: Record<string, string> = {};
    if (isContest && !contestName.trim()) next.contestName = '대회명을 입력해 주세요.';
    if (!isContest && !title.trim()) next.title = '목표 제목을 입력해 주세요.';
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const submit = async () => {
    if (!validate()) return;

    // 목 데이터의 id 는 'todo-1' 같은 문자열이라 숫자로 떨어질 때만 보낸다
    const todoId = pickedTodo ? Number(pickedTodo.id) : undefined;

    const payload: CreateGoalPayload = {
      type,
      status,
      detail: isContest
        ? {
            contestName: contestName.trim(),
            isTeam: teamLabel === TEAM_OPTIONS[0],
            result: trimmed(result),
            awardDate: trimmed(awardDate),
          }
        : {
            title: title.trim(),
            memo: trimmed(memo),
            targetDate: trimmed(targetDate),
            todoId: Number.isFinite(todoId) ? todoId : undefined,
          },
    };

    setSubmitting(true);
    setSubmitError('');
    try {
      await createGoal(payload);
      // 상세 화면이 아직 없어 목록으로 돌려보낸다. 새로 만든 성취가 바로 보이도록 다시 읽는다
      router.push('/mypage');
      router.refresh();
    } catch (error) {
      // 서버 예외는 msg 를 그대로 화면 문구로 쓸 수 있는 봉투로 온다
      setSubmitError(
        error instanceof ApiError ? error.message : '등록하지 못했어요. 잠시 후 다시 시도해 주세요.',
      );
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={(event) => event.preventDefault()}>
      <FormGroup
        label="유형"
        hint="파티 활동으로 남는 프로젝트 성취는 파티가 확정되면 자동으로 기록돼요. 개인 사이드 프로젝트는 체크리스트로 남깁니다."
      >
        <RadioChipGroup
          options={SELF_REPORTED_TYPES.map((value) => GOAL_TYPE_LABELS[value])}
          value={GOAL_TYPE_LABELS[type]}
          onChange={(label) =>
            setType(
              SELF_REPORTED_TYPES.find((value) => GOAL_TYPE_LABELS[value] === label) ?? type,
            )
          }
        />
      </FormGroup>

      <FormGroup label="진행 상태" hint="지금 어디까지 왔는지 골라주세요. 나중에 수정 화면에서 바꿀 수 있어요.">
        <RadioChipGroup
          options={GOAL_STATUSES.map((value) => GOAL_STATUS_LABELS[value])}
          value={GOAL_STATUS_LABELS[status]}
          onChange={(label) =>
            setStatus(GOAL_STATUSES.find((value) => GOAL_STATUS_LABELS[value] === label) ?? status)
          }
        />
      </FormGroup>

      {isContest ? (
        <>
          <FormGroup
            label="대회명"
            htmlFor="goalContestName"
            required
            hint="크루온 밖에서 참가한 대회를 직접 적는 자리예요."
            error={errors.contestName}
          >
            <TextField
              id="goalContestName"
              value={contestName}
              placeholder="예: 2025 공공데이터 활용 공모전"
              onChange={(event) => {
                setContestName(event.target.value);
                setErrors((prev) => ({ ...prev, contestName: '' }));
              }}
            />
          </FormGroup>

          <FormGroup label="참가 형태">
            <RadioChipGroup options={TEAM_OPTIONS} value={teamLabel} onChange={setTeamLabel} />
          </FormGroup>

          <FormRow>
            <FormGroup label="수상 결과" htmlFor="goalResult" hint="수상하지 않았다면 비워두세요.">
              <TextField
                id="goalResult"
                value={result}
                placeholder="예: 우수상"
                onChange={(event) => setResult(event.target.value)}
              />
            </FormGroup>

            <FormGroup label="수상일" htmlFor="goalAwardDate" hint="성취 리스트의 연도 기준이에요.">
              <TextField
                id="goalAwardDate"
                type="date"
                value={awardDate}
                onChange={(event) => setAwardDate(event.target.value)}
              />
            </FormGroup>
          </FormRow>
        </>
      ) : (
        <>
          <FormGroup
            label="연결할 개인 TODO"
            hint="마이페이지에 만든 개인 TODO를 연결하면 그 할 일 목록이 성취의 진행 과정으로 남아요. 연결하지 않아도 등록됩니다."
          >
            {pickedTodo ? (
              <div className="picked-card">
                <span className="picker-poster">{pickedTodo.category}</span>
                <span className="picker-meta">
                  <span className="pname">{pickedTodo.title}</span>
                  <span className="psub">
                    {pickedTodo.createdAt} 생성 · 할 일 {pickedTodo.doneCount}/{pickedTodo.totalCount}
                  </span>
                </span>
                <button
                  type="button"
                  className="picked-clear"
                  onClick={() => {
                    setPickedTodo(null);
                    setTodoKeyword('');
                  }}
                >
                  연결 해제
                </button>
              </div>
            ) : (
              <div className="picker">
                <div className="contest-link-field">
                  <TextField
                    placeholder="TODO 제목으로 검색 (예: 정보처리기사)"
                    autoComplete="off"
                    value={todoKeyword}
                    onChange={(event) => setTodoKeyword(event.target.value)}
                  />
                </div>
                {todoResults.length > 0 ? (
                  <div className="picker-results">
                    {todoResults.map((todo) => (
                      <button
                        key={todo.id}
                        type="button"
                        className="picker-item"
                        onClick={() => pickTodo(todo)}
                      >
                        <span className="picker-poster">{todo.category}</span>
                        <span className="picker-meta">
                          <span className="pname">{todo.title}</span>
                          <span className="psub">
                            {todo.createdAt} 생성 · 할 일 {todo.doneCount}/{todo.totalCount}
                          </span>
                        </span>
                      </button>
                    ))}
                  </div>
                ) : (
                  <p className="form-hint">
                    {todoKeyword.trim()
                      ? '검색 결과가 없어요.'
                      : '연결할 수 있는 개인 TODO가 없어요. 마이페이지에서 먼저 만들어 주세요.'}
                  </p>
                )}
              </div>
            )}
          </FormGroup>

          <FormGroup label="목표 제목" htmlFor="goalTitle" required error={errors.title}>
            <TextField
              id="goalTitle"
              value={title}
              placeholder="예: 정보처리기사 취득"
              onChange={(event) => {
                setTitle(event.target.value);
                setErrors((prev) => ({ ...prev, title: '' }));
              }}
            />
          </FormGroup>

          <FormGroup label="목표일" htmlFor="goalTargetDate" hint="언제까지 해낼 계획인지 적어두세요.">
            <TextField
              id="goalTargetDate"
              type="date"
              value={targetDate}
              onChange={(event) => setTargetDate(event.target.value)}
            />
          </FormGroup>

          <FormGroup label="메모" htmlFor="goalMemo" hint="준비 방법이나 남길 말을 자유롭게 적어요.">
            <TextAreaField
              id="goalMemo"
              rows={5}
              value={memo}
              onChange={(event) => setMemo(event.target.value)}
            />
          </FormGroup>
        </>
      )}

      {submitError ? <p className="form-error">{submitError}</p> : null}

      <FormActions>
        <Button onClick={submit} disabled={submitting}>
          {submitting ? '등록 중…' : '등록'}
        </Button>
        <Button variant="ghost" onClick={() => router.push('/mypage')}>
          취소
        </Button>
      </FormActions>
    </form>
  );
}
