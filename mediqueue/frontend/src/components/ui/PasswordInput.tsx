import { forwardRef, useState } from 'react';
import { Input, InputProps } from '@/components/ui/Input';
import { Eye, EyeOff } from 'lucide-react';

export type PasswordInputProps = Omit<InputProps, 'type' | 'rightIcon'>;

export const PasswordInput = forwardRef<HTMLInputElement, PasswordInputProps>((props, ref) => {
  const [showPassword, setShowPassword] = useState(false);

  return (
    <div className="relative w-full">
      <Input
        ref={ref}
        type={showPassword ? 'text' : 'password'}
        {...props}
        className={props.className}
      />
      <button
        type="button"
        onClick={() => setShowPassword((prev) => !prev)}
        className="absolute right-3 top-1/2 -translate-y-1/2 p-1.5 text-text-muted hover:text-text-primary rounded-lg transition-colors focus-visible:outline-none focus-visible-ring"
        aria-label={showPassword ? 'Hide password' : 'Show password'}
        tabIndex={0}
      >
        {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
      </button>
    </div>
  );
});

PasswordInput.displayName = 'PasswordInput';
