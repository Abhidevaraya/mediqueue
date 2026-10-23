import React, { forwardRef } from 'react';
import { cn } from '@/lib/utils/cn';
import { Loader2 } from 'lucide-react';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  isLoading?: boolean;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      className,
      variant = 'primary',
      size = 'md',
      isLoading = false,
      disabled,
      leftIcon,
      rightIcon,
      children,
      ...props
    },
    ref
  ) => {
    return (
      <button
        ref={ref}
        disabled={disabled || isLoading}
        className={cn(
          'inline-flex items-center justify-center font-medium rounded-xl transition-all duration-200 focus-visible:outline-none focus-visible-ring disabled:opacity-50 disabled:cursor-not-allowed select-none active:scale-[0.98]',
          // Variant styling
          variant === 'primary' &&
            'bg-brand-lavender text-white hover:bg-brand-lavender-hover shadow-soft-sm hover:shadow-soft-md shadow-brand-lavender/20',
          variant === 'secondary' &&
            'bg-brand-lavender-light text-brand-indigo hover:bg-brand-lavender-light/80 dark:bg-surface-subtle dark:text-brand-lavender dark:hover:bg-surface-subtle/80',
          variant === 'outline' &&
            'border border-surface-border bg-transparent text-text-primary hover:bg-surface-subtle',
          variant === 'ghost' &&
            'bg-transparent text-text-secondary hover:text-text-primary hover:bg-surface-subtle',
          variant === 'danger' &&
            'bg-status-error text-white hover:bg-status-error/90 shadow-soft-sm shadow-status-error/20',
          // Size styling
          size === 'sm' && 'h-9 px-3.5 text-xs gap-1.5',
          size === 'md' && 'h-11 px-5 text-sm gap-2',
          size === 'lg' && 'h-13 px-6 text-base gap-2.5',
          className
        )}
        {...props}
      >
        {isLoading && <Loader2 className="w-4 h-4 animate-spin shrink-0" aria-hidden="true" />}
        {!isLoading && leftIcon && <span className="shrink-0">{leftIcon}</span>}
        <span>{children}</span>
        {!isLoading && rightIcon && <span className="shrink-0">{rightIcon}</span>}
      </button>
    );
  }
);

Button.displayName = 'Button';
