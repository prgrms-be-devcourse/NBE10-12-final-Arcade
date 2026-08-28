import type {
  InputHTMLAttributes,
  ReactNode,
  SelectHTMLAttributes,
  TextareaHTMLAttributes,
} from 'react';

interface FormGroupProps {
  label?: ReactNode;
  htmlFor?: string;
  hint?: ReactNode;
  children: ReactNode;
  className?: string;
  style?: React.CSSProperties;
  /** 필수 항목이면 라벨 옆에 * 를 붙인다 */
  required?: boolean;
  /** 검증에 걸린 경우의 안내. 값이 있으면 hint 대신 이 문구를 보여준다 */
  error?: string;
}

export function FormGroup({
  label,
  htmlFor,
  hint,
  children,
  className,
  style,
  required,
  error,
}: FormGroupProps) {
  return (
    <div
      className={['form-group', error ? 'has-error' : null, className].filter(Boolean).join(' ')}
      style={style}
    >
      {label ? (
        <label className="form-label" htmlFor={htmlFor}>
          {label}
          {required ? <span className="form-required">*</span> : null}
        </label>
      ) : null}
      {children}
      {/* 안내 문구는 평소에 보여주고, 검증에 걸리면 그 자리에 이유를 대신 띄운다 */}
      {error ? <p className="form-field-error">{error}</p> : hint ? <p className="form-hint">{hint}</p> : null}
    </div>
  );
}

export function FormRow({ children }: { children: ReactNode }) {
  return <div className="form-row">{children}</div>;
}

export function FormActions({ children }: { children: ReactNode }) {
  return <div className="form-actions">{children}</div>;
}

export function TextField(props: InputHTMLAttributes<HTMLInputElement>) {
  const { className, ...rest } = props;
  return <input className={['field-input', className].filter(Boolean).join(' ')} {...rest} />;
}

export function SelectField(props: SelectHTMLAttributes<HTMLSelectElement>) {
  const { className, ...rest } = props;
  return <select className={['field-select', className].filter(Boolean).join(' ')} {...rest} />;
}

export function TextAreaField(props: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  const { className, ...rest } = props;
  return <textarea className={['field-textarea', className].filter(Boolean).join(' ')} {...rest} />;
}

/** 셀렉트 옵션을 문자열 배열로 간단히 그리기 위한 헬퍼 */
export function Options({ values }: { values: readonly string[] }) {
  return (
    <>
      {values.map((value) => (
        <option key={value} value={value}>
          {value}
        </option>
      ))}
    </>
  );
}
