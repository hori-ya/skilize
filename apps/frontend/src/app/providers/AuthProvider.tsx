/**
 * 認証状態をアプリ全体で共有する Context + Provider。
 * アプリ起動時に localStorage の JWT を使って /auth/me を呼び出し、前回のセッションを復元する。
 * グローバル状態は AuthContext のみとし、外部ライブラリ（Redux 等）は使用しない。
 */
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

  // 第2引数 [] は「コンポーネントの初回マウント時に1回だけ実行」を意味する。
  // deps 配列を省略すると毎レンダリング後に実行、[] にすると初回マウントのみ実行される。
  useEffect(() => {
    const token = localStorage.getItem('authToken');
    if (!token) {
      setIsLoading(false);
      return;
    }
    // トークンが存在する場合 /auth/me でセッション復元を試みる（期限切れ等は失敗してトークン削除）
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
    // サーバーはステートレスのため localStorage からトークンを削除するだけでログアウト完了
    localStorage.removeItem('authToken');
    setUser(null);
  };

  const changePassword = async (currentPassword: string, newPassword: string) => {
    await authApi.changePassword(currentPassword, newPassword);
    if (user) {
      // パスワード変更成功後、isInitialPassword をローカルで false に更新して /change-password へのリダイレクトを防ぐ
      // `{ ...user, isInitialPassword: false }` はスプレッド構文で既存オブジェクトをコピーしつつ特定フィールドを上書きする
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
  // AuthProvider の外で useAuth を呼んだ場合は ctx が null になるため、明示的にエラーを出す
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
