import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { LoginPage } from '@/features/auth/LoginPage';
import { RegisterPage } from '@/features/auth/RegisterPage';
import { AuthProvider } from '@/lib/auth/AuthContext';
import { ToastProvider } from '@/lib/feedback/ToastContext';
import { authService } from '@/lib/auth/authService';
import { tokenStorage } from '@/lib/auth/tokenStorage';
import { AppApiError } from '@/lib/api/client';

import { ThemeProvider } from '@/lib/theme/ThemeContext';

// Mock authService
vi.mock('@/lib/auth/authService', () => ({
  authService: {
    login: vi.fn(),
    register: vi.fn(),
    getCurrentUser: vi.fn(),
    logout: vi.fn(),
  },
}));

const renderWithProviders = (ui: React.ReactElement) => {
  return render(
    <MemoryRouter>
      <ThemeProvider>
        <ToastProvider>
          <AuthProvider>{ui}</AuthProvider>
        </ToastProvider>
      </ThemeProvider>
    </MemoryRouter>
  );
};

describe('Authentication Flows', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
    localStorage.clear();
    tokenStorage.clearToken();
  });

  describe('LoginPage', () => {
    it('renders login form with accessible fields and controls', () => {
      renderWithProviders(<LoginPage />);

      expect(screen.getByRole('heading', { name: /welcome back/i })).toBeInTheDocument();
      expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/^password/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /register here/i })).toBeInTheDocument();
    });

    it('validates empty inputs on submit and displays error messages', async () => {
      renderWithProviders(<LoginPage />);

      const submitButton = screen.getByRole('button', { name: /sign in/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('Email address is required')).toBeInTheDocument();
        expect(screen.getByText('Password is required')).toBeInTheDocument();
      });
    });

    it('submits valid credentials and calls authService.login', async () => {
      vi.mocked(authService.login).mockResolvedValueOnce({
        accessToken: 'mock-jwt-token-xyz',
        tokenType: 'Bearer',
        expiresInMs: 86400000,
        user: {
          id: 1,
          fullName: 'John Patient',
          email: 'john@example.com',
          role: 'PATIENT',
          accountStatus: 'ACTIVE',
        },
      });

      renderWithProviders(<LoginPage />);

      fireEvent.change(screen.getByLabelText(/email address/i), {
        target: { value: 'john@example.com' },
      });
      fireEvent.change(screen.getByLabelText(/^password/i), {
        target: { value: 'Patient#1234' },
      });

      fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

      await waitFor(() => {
        expect(authService.login).toHaveBeenCalledWith({
          email: 'john@example.com',
          password: 'Patient#1234',
        });
      });
    });

    it('displays server error banner when login returns 401 Bad Credentials', async () => {
      vi.mocked(authService.login).mockRejectedValueOnce(
        new AppApiError('Invalid email or password.', 401, 'INVALID_CREDENTIALS')
      );

      renderWithProviders(<LoginPage />);

      fireEvent.change(screen.getByLabelText(/email address/i), {
        target: { value: 'john@example.com' },
      });
      fireEvent.change(screen.getByLabelText(/^password/i), {
        target: { value: 'WrongPassword#1' },
      });

      fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

      await waitFor(() => {
        expect(screen.getByRole('alert')).toHaveTextContent('Invalid email or password.');
      });
    });
  });

  describe('RegisterPage', () => {
    it('renders patient registration form with required fields', () => {
      renderWithProviders(<RegisterPage />);

      expect(screen.getByRole('heading', { name: /create patient account/i })).toBeInTheDocument();
      expect(screen.getByLabelText(/full name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/^password/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/confirm password/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /create patient account/i })).toBeInTheDocument();
    });

    it('validates password strength and password match', async () => {
      renderWithProviders(<RegisterPage />);

      fireEvent.change(screen.getByLabelText(/full name/i), { target: { value: 'Jane' } });
      fireEvent.change(screen.getByLabelText(/email address/i), { target: { value: 'jane@test.com' } });
      fireEvent.change(screen.getByLabelText(/^password/i), { target: { value: 'weak' } });
      fireEvent.change(screen.getByLabelText(/confirm password/i), { target: { value: 'mismatch' } });

      fireEvent.click(screen.getByRole('button', { name: /create patient account/i }));

      await waitFor(() => {
        expect(screen.getByText('Password must be at least 8 characters')).toBeInTheDocument();
        expect(screen.getByText('Passwords do not match')).toBeInTheDocument();
      });
    });

    it('submits valid registration and establishes session', async () => {
      vi.mocked(authService.register).mockResolvedValueOnce({
        accessToken: 'registered-jwt-token',
        tokenType: 'Bearer',
        expiresInMs: 86400000,
        user: {
          id: 2,
          fullName: 'Jane Doe',
          email: 'jane@example.com',
          role: 'PATIENT',
          accountStatus: 'ACTIVE',
        },
      });

      renderWithProviders(<RegisterPage />);

      fireEvent.change(screen.getByLabelText(/full name/i), { target: { value: 'Jane Doe' } });
      fireEvent.change(screen.getByLabelText(/email address/i), { target: { value: 'jane@example.com' } });
      fireEvent.change(screen.getByLabelText(/^password/i), { target: { value: 'Patient#9876' } });
      fireEvent.change(screen.getByLabelText(/confirm password/i), { target: { value: 'Patient#9876' } });

      fireEvent.click(screen.getByRole('button', { name: /create patient account/i }));

      await waitFor(() => {
        expect(authService.register).toHaveBeenCalledWith({
          fullName: 'Jane Doe',
          email: 'jane@example.com',
          password: 'Patient#9876',
          phoneNumber: undefined,
        });
      });
    });
  });
});
