import React from 'react';
import { cn } from '@/lib/utils/cn';
import { AlertCircle } from 'lucide-react';

export interface FormFieldProps {
  id: string;
  label?: string;
  required?: boolean;
  error?: string;
  hint?: string;
  children: React.ReactElement<{ id?: string; error?: boolean; 'aria-describedby'?: string }>;
  className?: string;
}

export const FormField: React.FC<FormFieldProps> = ({
  id,
  label,
  required,
  error,
  hint,
  children,
  className,
}) => {
  const errorId = `${id}-error`;
  const hintId = `${id}-hint`;

  const describedBy = [error ? errorId : null, hint ? hintId : null]
    .filter(Boolean)
    .join(' ');

  const childWithProps = React.cloneElement(children, {
    id,
    error: Boolean(error),
    'aria-describedby': describedBy || undefined,
  });

  return (
    <div className={cn('flex flex-col gap-1.5 w-full', className)}>
      {label && (
        <label htmlFor={id} className="text-sm font-medium text-text-primary flex items-center gap-1 select-none">
          {label}
          {required && <span className="text-status-error text-xs" aria-hidden="true">*</span>}
        </label>
      )}

      {childWithProps}

      {hint && !error && (
        <p id={hintId} className="text-xs text-text-secondary leading-normal">
          {hint}
        </p>
      )}

      {error && (
        <p id={errorId} className="text-xs text-status-error flex items-center gap-1 mt-0.5 leading-normal" role="alert">
          <AlertCircle className="w-3.5 h-3.5 shrink-0" />
          <span>{error}</span>
        </p>
      )}
    </div>
  );
};
