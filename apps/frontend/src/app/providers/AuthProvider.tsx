import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import type { AuthUser } from '../../features/auth/types/index';
import * as authApi from '../../features/auth/api/authApi';

interface AuthContextType {
  user: AuthUser | null;
  isLoading: boolean;
  login: (userId: string, password: string) => Promise<AuthUser>;
  logout: () => void;
  changePassword: (currentPassword: string, newPassword: string) => Promise<void>;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('authToken');
    if (!token) {
      setIsLoading(false);
      return;
    }
    authApi.getMe()
      .then((res) => setUser(res.data))
      .catch(() => localStorage.removeItem('authToken'))
      .finally(() => setIsLoading(false));
  }, []);

  const login = async (userId: string, password: string): Promise<AuthUser> => {
    const { data } = await authApi.login(userId, password);
    localStorage.setItem('authToken', data.token);
    const meRes = await authApi.getMe();
    setUser(meRes.data);
    return meRes.data;
  };

  const logout = () => {
    localStorage.removeItem('authToken');
    setUser(null);
  };

  const changePassword = async (currentPassword: string, newPassword: string) => {
    await authApi.changePassword(currentPassword, newPassword);
    if (user) {
      setUser({ ...user, isInitialPassword: false });
    }
  };

  return (
    <AuthContext.Provider value={{ user, isLoading, login, logout, changePassword }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
