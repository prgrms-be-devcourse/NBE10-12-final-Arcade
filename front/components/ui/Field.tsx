import type {
  InputHTMLAttributes,
  ReactNode,
  SelectHTMLAttributes,
  TextareaHTMLAttributes,
} from 'react';
import { createContext, useContext, useId } from 'react';

interface FieldA11yContextValue {
  id?: string;
  describedBy?: string;
  invalid?: boolean;
}

const FieldA11yContext = createContext<FieldA11yContextValue>({});

function joinIds(...ids: Array<string | undefined>) {
  return ids.filter(Boolean).join(' ') || undefined;
}

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
  const generatedId = useId();
  const fieldId = htmlFor ?? `field-${generatedId}`;
  const hintId = hint && !error ? `${fieldId}-hint` : undefined;
  const errorId = error ? `${fieldId}-error` : undefined;
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
      <FieldA11yContext.Provider value={{ id: fieldId, describedBy: joinIds(hintId, errorId), invalid: Boolean(error) }}>
        {children}
      </FieldA11yContext.Provider>
      {/* 안내 문구는 평소에 보여주고, 검증에 걸리면 그 자리에 이유를 대신 띄운다 */}
      {error ? <p id={errorId} className="form-field-error" role="alert">{error}</p> : hint ? <p id={hintId} className="form-hint">{hint}</p> : null}
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
  const { className, id, 'aria-invalid': ariaInvalid, 'aria-describedby': ariaDescribedBy, ...rest } = props;
  const field = useContext(FieldA11yContext);
  return <input className={['field-input', className].filter(Boolean).join(' ')} id={id ?? field.id} aria-invalid={ariaInvalid ?? (field.invalid || undefined)} aria-describedby={joinIds(ariaDescribedBy, field.describedBy)} {...rest} />;
}

export function SelectField(props: SelectHTMLAttributes<HTMLSelectElement>) {
  const { className, id, 'aria-invalid': ariaInvalid, 'aria-describedby': ariaDescribedBy, ...rest } = props;
  const field = useContext(FieldA11yContext);
  return <select className={['field-select', className].filter(Boolean).join(' ')} id={id ?? field.id} aria-invalid={ariaInvalid ?? (field.invalid || undefined)} aria-describedby={joinIds(ariaDescribedBy, field.describedBy)} {...rest} />;
}

export function TextAreaField(props: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  const { className, id, 'aria-invalid': ariaInvalid, 'aria-describedby': ariaDescribedBy, ...rest } = props;
  const field = useContext(FieldA11yContext);
  return <textarea className={['field-textarea', className].filter(Boolean).join(' ')} id={id ?? field.id} aria-invalid={ariaInvalid ?? (field.invalid || undefined)} aria-describedby={joinIds(ariaDescribedBy, field.describedBy)} {...rest} />;
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
