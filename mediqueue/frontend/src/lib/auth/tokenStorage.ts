/**
 * Token Storage Manager
 * Stores access token in memory with sessionStorage fallback for tab lifecycle.
 * Avoids localStorage to prevent long-lived persistent token exposure across browser restarts.
 */

const SESSION_TOKEN_KEY = 'mediqueue_session_token';
let inMemoryToken: string | null = null;

export const tokenStorage = {
  getToken(): string | null {
    if (inMemoryToken) {
      return inMemoryToken;
    }
    try {
      const stored = sessionStorage.getItem(SESSION_TOKEN_KEY);
      if (stored) {
        inMemoryToken = stored;
        return stored;
      }
    } catch {
      // Storage access blocked or unavailable
    }
    return null;
  },

  setToken(token: string): void {
    inMemoryToken = token;
    try {
      sessionStorage.setItem(SESSION_TOKEN_KEY, token);
    } catch {
      // Storage access blocked
    }
  },

  clearToken(): void {
    inMemoryToken = null;
    try {
      sessionStorage.removeItem(SESSION_TOKEN_KEY);
    } catch {
      // Storage access blocked
    }
  },

  hasToken(): boolean {
    return Boolean(this.getToken());
  },
};
