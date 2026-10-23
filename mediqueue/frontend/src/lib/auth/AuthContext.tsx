import React, { createContext, useContext, useEffect, useState, useCallback, useMemo } from 'react';
import { AuthResponse, DoctorRegisterPayload, LoginCredentials, RegisterPayload, UserSummary } from '@/types/auth';
import { authService } from '@/lib/auth/authService';
import { tokenStorage } from '@/lib/auth/tokenStorage';

export interface AuthContextType {
  user: UserSummary | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: LoginCredentials) => Promise<AuthResponse>;
  register: (payload: RegisterPayload) => Promise<AuthResponse>;
  registerDoctor: (payload: DoctorRegisterPayload) => Promise<AuthResponse>;
  logout: () => Promise<void>;
  refreshProfile: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserSummary | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  // Initialize session on mount
  const initSession = useCallback(async () => {
    const token = tokenStorage.getToken();
    if (!token) {
      setUser(null);
      setIsLoading(false);
      return;
    }

    try {
      const profile = await authService.getCurrentUser();
      setUser(profile);
    } catch {
      // Token is invalid or expired
      tokenStorage.clearToken();
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    initSession();

    // Listen to 401 expiration events emitted by API client
    const handleAuthExpired = () => {
      setUser(null);
    };

    window.addEventListener('mediqueue:auth_expired', handleAuthExpired);
    return () => {
      window.removeEventListener('mediqueue:auth_expired', handleAuthExpired);
    };
  }, [initSession]);

  const login = useCallback(async (credentials: LoginCredentials): Promise<AuthResponse> => {
    const response = await authService.login(credentials);
    tokenStorage.setToken(response.accessToken);
    setUser(response.user);
    return response;
  }, []);

  const register = useCallback(async (payload: RegisterPayload): Promise<AuthResponse> => {
    const response = await authService.register(payload);
    tokenStorage.setToken(response.accessToken);
    setUser(response.user);
    return response;
  }, []);

  const registerDoctor = useCallback(async (payload: DoctorRegisterPayload): Promise<AuthResponse> => {
    const response = await authService.registerDoctor(payload);
    if (response.accessToken) {
      tokenStorage.setToken(response.accessToken);
      setUser(response.user);
    }
    return response;
  }, []);

  const logout = useCallback(async (): Promise<void> => {
    try {
      await authService.logout();
    } finally {
      tokenStorage.clearToken();
      setUser(null);
    }
  }, []);

  const refreshProfile = useCallback(async (): Promise<void> => {
    if (!tokenStorage.hasToken()) {
      setUser(null);
      return;
    }
    const profile = await authService.getCurrentUser();
    setUser(profile);
  }, []);

  const value = useMemo<AuthContextType>(
    () => ({
      user,
      isAuthenticated: Boolean(user),
      isLoading,
      login,
      register,
      registerDoctor,
      logout,
      refreshProfile,
    }),
    [user, isLoading, login, register, registerDoctor, logout, refreshProfile]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
