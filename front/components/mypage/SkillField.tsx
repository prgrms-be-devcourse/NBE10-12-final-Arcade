'use client';

import { useRef, useState } from 'react';
import { ChipRow, SkillChip } from '@/components/ui/Tag';
import { isEnterCommit } from '@/lib/ime';
import { TECH_STACKS, isKnownTechStack, normalizeTechStack } from '@/lib/techStacks';

/** 한 번에 띄울 후보 수. 더 늘리면 목록이 화면을 덮는다 */
const MAX_SUGGESTIONS = 8;

/**
 * 스킬 입력 — 기술 목록 자동완성이 붙은 칩 입력칸.
 *
 * 처음에는 `<datalist>`(브라우저 기본 자동완성)를 썼는데, 그 드롭다운은 **브라우저가 직접 그려서**
 * 색·테두리를 맞출 수 없고 위/아래로 제멋대로 열린다. 그래서 공모전 검색이 쓰던 `.picker` 패턴을
 * 그대로 가져와 직접 그린다 - 입력칸 바로 아래에 고정되고 테마 토큰을 그대로 쓴다.
 *
 * 목록에 없는 값도 등록된다. 표기만 목록 기준으로 맞추고("java" → "Java") 안내만 띄운다.
 */
export function SkillField({
  skills,
  onChange,
}: {
  skills: string[];
  onChange: (next: string[]) => void;
}) {
  const [input, setInput] = useState('');
  const [notice, setNotice] = useState('');
  const [open, setOpen] = useState(false);
  /** 키보드로 짚고 있는 후보. -1 이면 입력한 값 그대로 등록한다 */
  const [active, setActive] = useState(-1);
  const inputRef = useRef<HTMLInputElement>(null);

  const keyword = input.trim().toLowerCase();
  const suggestions = TECH_STACKS.filter(
    (tech) => !skills.includes(tech) && tech.toLowerCase().includes(keyword),
  ).slice(0, MAX_SUGGESTIONS);
  const listOpen = open && suggestions.length > 0;

  const add = (raw: string) => {
    const value = normalizeTechStack(raw);
    if (!value) return;

    setNotice(
      isKnownTechStack(value) ? '' : `'${value}' 는 등록된 기술이 아니에요. 입력한 그대로 저장됩니다.`,
    );
    if (!skills.includes(value)) onChange([...skills, value]);
    setInput('');
    setActive(-1);
  };

  const onKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      if (!listOpen) return;
      event.preventDefault();
      const step = event.key === 'ArrowDown' ? 1 : -1;
      // -1(입력값 그대로)과 후보 사이를 순환한다
      setActive((prev) => {
        const next = prev + step;
        if (next < -1) return suggestions.length - 1;
        if (next >= suggestions.length) return -1;
        return next;
      });
      return;
    }

    if (event.key === 'Escape') {
      setOpen(false);
      setActive(-1);
      return;
    }

    if (!isEnterCommit(event)) return;
    event.preventDefault();
    // 짚어 둔 후보가 있으면 그것을, 없으면 입력한 값을 등록한다
    const picked = active >= 0 ? suggestions[active] : input;
    if (picked?.trim()) add(picked);
  };

  return (
    <>
      {skills.length > 0 ? (
        <ChipRow>
          {skills.map((skill) => (
            <button
              key={skill}
              type="button"
              className="skill-chip-remove"
              aria-label={`${skill} 삭제`}
              onClick={() => onChange(skills.filter((value) => value !== skill))}
            >
              <SkillChip>
                {skill}
                <span aria-hidden="true" className="skill-chip-x">
                  ×
                </span>
              </SkillChip>
            </button>
          ))}
        </ChipRow>
      ) : null}

      <div className="picker skill-picker">
        <input
          ref={inputRef}
          className="field-input"
          placeholder="스킬을 입력하고 Enter (예: Spring Boot)"
          autoComplete="off"
          role="combobox"
          aria-expanded={listOpen}
          aria-controls="skill-suggestions"
          aria-autocomplete="list"
          aria-activedescendant={active >= 0 ? `skill-option-${active}` : undefined}
          value={input}
          onFocus={() => setOpen(true)}
          // 후보를 누를 때 blur 가 먼저 나면 목록이 닫혀 클릭이 사라진다
          onBlur={() => setOpen(false)}
          onChange={(event) => {
            setInput(event.target.value);
            setNotice('');
            setActive(-1);
            setOpen(true);
          }}
          onKeyDown={onKeyDown}
        />

        {listOpen ? (
          <div className="picker-results" id="skill-suggestions" role="listbox">
            {suggestions.map((tech, index) => (
              <button
                key={tech}
                type="button"
                id={`skill-option-${index}`}
                role="option"
                aria-selected={index === active}
                className={index === active ? 'skill-option is-active' : 'skill-option'}
                onMouseDown={(event) => {
                  // blur 를 막아야 클릭이 살아난다
                  event.preventDefault();
                  add(tech);
                  inputRef.current?.focus();
                }}
                onMouseEnter={() => setActive(index)}
              >
                {tech}
              </button>
            ))}
          </div>
        ) : null}
      </div>

      {notice ? <p className="form-hint">{notice}</p> : null}
    </>
  );
}
