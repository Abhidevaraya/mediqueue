import React from 'react';
import { Dialog } from '@/components/ui/Dialog';
import { Button } from '@/components/ui/Button';
import { AlertTriangle } from 'lucide-react';

export interface ConfirmDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  message?: string;
  description?: string;
  confirmLabel?: string;
  confirmText?: string;
  cancelLabel?: string;
  cancelText?: string;
  variant?: 'danger' | 'primary';
  isLoading?: boolean;
}

export const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  description,
  confirmLabel,
  confirmText,
  cancelLabel = 'Cancel',
  cancelText,
  variant = 'primary',
  isLoading = false,
}) => {
  const finalMessage = message ?? description ?? '';
  const finalConfirmLabel = confirmText ?? confirmLabel ?? 'Confirm';
  const finalCancelLabel = cancelText ?? cancelLabel ?? 'Cancel';
  return (
    <Dialog isOpen={isOpen} onClose={onClose} title={title}>
      <div className="flex items-start gap-3.5 my-2">
        {variant === 'danger' && (
          <div className="p-2.5 rounded-full bg-status-error-surface text-status-error shrink-0 mt-0.5">
            <AlertTriangle className="w-5 h-5" />
          </div>
        )}
        <p className="text-sm text-text-secondary leading-relaxed">{finalMessage}</p>
      </div>

      <div className="flex items-center justify-end gap-3 mt-6 pt-4 border-t border-surface-border">
        <Button variant="ghost" onClick={onClose} disabled={isLoading}>
          {finalCancelLabel}
        </Button>
        <Button
          variant={variant === 'danger' ? 'danger' : 'primary'}
          onClick={onConfirm}
          isLoading={isLoading}
        >
          {finalConfirmLabel}
        </Button>
      </div>
    </Dialog>
  );
};
