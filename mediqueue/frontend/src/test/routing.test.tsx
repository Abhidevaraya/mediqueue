import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider } from '@/lib/theme/ThemeContext';
import { AuthProvider } from '@/lib/auth/AuthContext';
import { ToastProvider } from '@/lib/feedback/ToastContext';
import { AppRoutes } from '@/routes';
import { authService } from '@/lib/auth/authService';
import { tokenStorage } from '@/lib/auth/tokenStorage';

vi.mock('@/lib/auth/authService', () => ({
  authService: {
    login: vi.fn(),
    register: vi.fn(),
    getCurrentUser: vi.fn(),
    logout: vi.fn(),
  },
}));

const renderRoute = (initialPath: string) => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <AuthProvider>
          <ToastProvider>
            <MemoryRouter initialEntries={[initialPath]}>
              <AppRoutes />
            </MemoryRouter>
          </ToastProvider>
        </AuthProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
};

describe('Routing and Route Guards', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
    localStorage.clear();
    tokenStorage.clearToken();
  });

  it('renders landing page on public route /', async () => {
    renderRoute('/');
    expect(
      await screen.findByRole('heading', { name: /ditch the waiting room/i })
    ).toBeInTheDocument();
  });

  it('renders login page on /login', async () => {
    renderRoute('/login');
    expect(await screen.findByRole('heading', { name: /welcome back/i })).toBeInTheDocument();
  });

  it('renders register page on /register', async () => {
    renderRoute('/register');
    expect(
      await screen.findByRole('heading', { name: /create patient account/i })
    ).toBeInTheDocument();
  });

  it('redirects unauthenticated users from protected /patient route to /login', async () => {
    renderRoute('/patient');
    // Without active token, user should be redirected to /login
    expect(await screen.findByRole('heading', { name: /welcome back/i })).toBeInTheDocument();
  });

  it('renders unauthorized page on /unauthorized', async () => {
    renderRoute('/unauthorized');
    expect(
      await screen.findByRole('heading', { name: /access restricted/i })
    ).toBeInTheDocument();
  });

  it('renders 404 not found page on unknown route', async () => {
    renderRoute('/some-random-missing-route');
    expect(await screen.findByRole('heading', { name: /page not found/i })).toBeInTheDocument();
  });

  it('allows authenticated patient into /patient workspace and displays shell', async () => {
    tokenStorage.setToken('valid-patient-token');
    vi.mocked(authService.getCurrentUser).mockResolvedValueOnce({
      id: 42,
      fullName: 'Alice Patient',
      email: 'alice@example.com',
      role: 'PATIENT',
      accountStatus: 'ACTIVE',
    });

    renderRoute('/patient');
    expect(await screen.findByRole('heading', { name: /welcome back/i })).toBeInTheDocument();
    const nameMatches = await screen.findAllByText('Alice Patient');
    expect(nameMatches.length).toBeGreaterThanOrEqual(1);
  });

  it('blocks authenticated patient from accessing /admin and redirects to /unauthorized', async () => {
    tokenStorage.setToken('valid-patient-token');
    vi.mocked(authService.getCurrentUser).mockResolvedValueOnce({
      id: 42,
      fullName: 'Alice Patient',
      email: 'alice@example.com',
      role: 'PATIENT',
      accountStatus: 'ACTIVE',
    });

    renderRoute('/admin');
    expect(
      await screen.findByRole('heading', { name: /access restricted/i })
    ).toBeInTheDocument();
  });
});
