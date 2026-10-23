import React from 'react';
import { cn } from '@/lib/utils/cn';

export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  variant?: 'primary' | 'success' | 'warning' | 'error' | 'neutral' | 'outline';
  size?: 'sm' | 'md';
}

export const Badge: React.FC<BadgeProps> = ({
  className,
  variant = 'primary',
  size = 'md',
  children,
  ...props
}) => {
  return (
    <span
      className={cn(
        'inline-flex items-center font-semibold rounded-full select-none transition-colors',
        // Variants
        variant === 'primary' && 'bg-brand-lavender-light text-brand-indigo dark:bg-brand-lavender/20 dark:text-brand-lavender',
        variant === 'success' && 'bg-status-success-surface text-status-success',
        variant === 'warning' && 'bg-status-warning-surface text-status-warning',
        variant === 'error' && 'bg-status-error-surface text-status-error',
        variant === 'neutral' && 'bg-surface-subtle text-text-secondary',
        variant === 'outline' && 'border border-surface-border text-text-secondary',
        // Sizes
        size === 'sm' && 'text-[11px] px-2.5 py-0.5',
        size === 'md' && 'text-xs px-3 py-1',
        className
      )}
      {...props}
    >
      {children}
    </span>
  );
};
