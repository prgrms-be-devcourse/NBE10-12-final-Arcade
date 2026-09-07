'use client';

import { useRef, useState } from 'react';
import { Icon } from '@/components/icons/Icon';

interface CoverUploadProps {
  /** 파일명이 바뀔 때 상위 폼에 알린다 (아직 업로드를 붙이지 않은 폼이 쓴다) */
  onChange?: (fileName: string | null) => void;
  /** 파일 자체가 필요할 때 쓴다. 실제 업로드를 붙인 폼은 이쪽을 받는다 */
  onFileChange?: (file: File | null) => void;
  compact?: boolean;
  title?: string;
  sub?: string;
  hint?: React.ReactNode;
}

/** 대표 이미지 1장 업로드 — 공모전 등록 / 전시 등록 / 프로필 사진에서 재사용 */
export function CoverUpload({
  onChange,
  onFileChange,
  compact,
  title = '대표 이미지 1장을 올려주세요',
  sub = '클릭하거나 파일을 끌어다 놓기 · JPG · PNG · 5MB 이하',
  hint,
}: CoverUploadProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [fileName, setFileName] = useState<string | null>(null);

  const update = (file: File | null) => {
    setFileName(file?.name ?? null);
    onChange?.(file?.name ?? null);
    onFileChange?.(file);
  };

  return (
    <>
      <div className={compact ? 'cover-upload is-compact' : 'cover-upload'}>
        {fileName ? (
          <>
            <div className="cover-preview">
              <span className="cover-badge">MAIN</span>
              <span className="cover-name">{fileName}</span>
            </div>
            <div className="cover-actions">
              <button type="button" className="btn btn-ghost" onClick={() => inputRef.current?.click()}>
                사진 교체
              </button>
              <button type="button" className="btn btn-ghost" onClick={() => update(null)}>
                삭제
              </button>
            </div>
          </>
        ) : (
          <button type="button" className="cover-drop" onClick={() => inputRef.current?.click()}>
            <Icon name="i-plus" />
            {title}
            <span className="sub">{sub}</span>
          </button>
        )}
        <input
          ref={inputRef}
          type="file"
          accept="image/png,image/jpeg"
          hidden
          onChange={(event) => update(event.target.files?.[0] ?? null)}
        />
      </div>
      {hint ? <p className="cover-hint">{hint}</p> : null}
    </>
  );
}
