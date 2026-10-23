import React from 'react';
import { useTheme } from '@/lib/theme/ThemeContext';
import { Sun, Moon, Laptop } from 'lucide-react';
import { cn } from '@/lib/utils/cn';

export const ThemeToggle: React.FC<{ className?: string }> = ({ className }) => {
  const { theme, setTheme } = useTheme();

  return (
    <div
      className={cn(
        'inline-flex items-center p-1 rounded-xl bg-surface-subtle border border-surface-border text-text-secondary select-none',
        className
      )}
      role="group"
      aria-label="Theme switcher"
    >
      <button
        type="button"
        onClick={() => setTheme('light')}
        className={cn(
          'p-1.5 rounded-lg text-xs font-medium transition-all duration-200 flex items-center justify-center focus-visible:outline-none focus-visible-ring',
          theme === 'light'
            ? 'bg-surface-elevated text-brand-lavender shadow-soft-sm font-semibold'
            : 'hover:text-text-primary'
        )}
        aria-pressed={theme === 'light'}
        aria-label="Light mode"
      >
        <Sun className="w-4 h-4" />
      </button>

      <button
        type="button"
        onClick={() => setTheme('dark')}
        className={cn(
          'p-1.5 rounded-lg text-xs font-medium transition-all duration-200 flex items-center justify-center focus-visible:outline-none focus-visible-ring',
          theme === 'dark'
            ? 'bg-surface-elevated text-brand-lavender shadow-soft-sm font-semibold'
            : 'hover:text-text-primary'
        )}
        aria-pressed={theme === 'dark'}
        aria-label="Dark mode"
      >
        <Moon className="w-4 h-4" />
      </button>

      <button
        type="button"
        onClick={() => setTheme('system')}
        className={cn(
          'p-1.5 rounded-lg text-xs font-medium transition-all duration-200 flex items-center justify-center focus-visible:outline-none focus-visible-ring',
          theme === 'system'
            ? 'bg-surface-elevated text-brand-lavender shadow-soft-sm font-semibold'
            : 'hover:text-text-primary'
        )}
        aria-pressed={theme === 'system'}
        aria-label="System theme"
      >
        <Laptop className="w-4 h-4" />
      </button>
    </div>
  );
};
