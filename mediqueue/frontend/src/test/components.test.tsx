import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { FormField } from '@/components/ui/FormField';
import { Dialog } from '@/components/ui/Dialog';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { ToastProvider, useToast } from '@/lib/feedback/ToastContext';
import { Mail } from 'lucide-react';

const ToastTestConsumer = () => {
  const toast = useToast();
  return (
    <div>
      <button onClick={() => toast.success('Patient profile saved', 'Saved')}>Trigger Success</button>
      <button onClick={() => toast.error('Unable to reach server', 'Error')}>Trigger Error</button>
    </div>
  );
};

describe('Reusable UI Components', () => {
  describe('Button', () => {
    it('renders with variants, sizes, and handles clicks', () => {
      const handleClick = vi.fn();
      render(
        <Button variant="primary" size="md" onClick={handleClick}>
          Click Me
        </Button>
      );

      const button = screen.getByRole('button', { name: 'Click Me' });
      expect(button).toBeInTheDocument();
      fireEvent.click(button);
      expect(handleClick).toHaveBeenCalledTimes(1);
    });

    it('displays loading spinner and disables interaction when isLoading is true', () => {
      const handleClick = vi.fn();
      render(
        <Button isLoading onClick={handleClick}>
          Submit
        </Button>
      );

      const button = screen.getByRole('button');
      expect(button).toBeDisabled();
      fireEvent.click(button);
      expect(handleClick).not.toHaveBeenCalled();
    });
  });

  describe('Input and FormField', () => {
    it('renders with label, error association, and left icon slot', () => {
      render(
        <FormField
          id="email-input"
          label="Email Address"
          required
          error="Invalid email format"
          hint="We will not share your email"
        >
          <Input id="email-input" leftIcon={<Mail data-testid="mail-icon" />} defaultValue="invalid" />
        </FormField>
      );

      expect(screen.getByLabelText(/Email Address/i)).toBeInTheDocument();
      expect(screen.getByTestId('mail-icon')).toBeInTheDocument();
      expect(screen.getByText('Invalid email format')).toBeInTheDocument();

      const input = screen.getByLabelText(/Email Address/i);
      expect(input).toHaveAttribute('aria-invalid', 'true');
    });
  });

  describe('Dialog and ConfirmDialog', () => {
    it('renders dialog when isOpen is true and dismisses on Escape or close button', () => {
      const handleClose = vi.fn();
      const { rerender } = render(
        <Dialog isOpen={true} onClose={handleClose} title="Test Modal">
          <p>Dialog body text</p>
        </Dialog>
      );

      expect(screen.getByText('Test Modal')).toBeInTheDocument();
      expect(screen.getByText('Dialog body text')).toBeInTheDocument();

      // Dismiss on close button
      const closeBtn = screen.getByRole('button', { name: /close/i });
      fireEvent.click(closeBtn);
      expect(handleClose).toHaveBeenCalledTimes(1);

      // Does not render when isOpen is false
      rerender(
        <Dialog isOpen={false} onClose={handleClose} title="Test Modal">
          <p>Dialog body text</p>
        </Dialog>
      );
      expect(screen.queryByText('Test Modal')).not.toBeInTheDocument();
    });

    it('ConfirmDialog executes onConfirm action and handles cancel', () => {
      const handleConfirm = vi.fn();
      const handleClose = vi.fn();

      render(
        <ConfirmDialog
          isOpen={true}
          onClose={handleClose}
          onConfirm={handleConfirm}
          title="Cancel Appointment"
          description="Are you sure you want to cancel this visit?"
          confirmText="Yes, Cancel"
        />
      );

      expect(screen.getByText('Cancel Appointment')).toBeInTheDocument();
      const confirmBtn = screen.getByRole('button', { name: 'Yes, Cancel' });
      fireEvent.click(confirmBtn);
      expect(handleConfirm).toHaveBeenCalledTimes(1);

      const cancelBtn = screen.getByRole('button', { name: 'Cancel' });
      fireEvent.click(cancelBtn);
      expect(handleClose).toHaveBeenCalledTimes(1);
    });
  });

  describe('Toast Notifications', () => {
    it('renders toast notification and allows manual dismissal', () => {
      render(
        <ToastProvider>
          <ToastTestConsumer />
        </ToastProvider>
      );

      fireEvent.click(screen.getByText('Trigger Success'));
      expect(screen.getByText('Patient profile saved')).toBeInTheDocument();
      expect(screen.getByText('Saved')).toBeInTheDocument();

      // Dismiss toast
      const dismissBtn = screen.getByRole('button', { name: /close notification/i });
      act(() => {
        fireEvent.click(dismissBtn);
      });
      expect(screen.queryByText('Patient profile saved')).not.toBeInTheDocument();
    });
  });
});
