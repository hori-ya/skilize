/**
 * 認証関連 API。login のみ JWT 不要（他はすべて Axios インターセプターが自動付与）。
 * ログイン成功時は token を localStorage に保存し、以降のリクエストで使用する。
 */
import apiClient from '../../../shared/api/client';
import type { AuthUser } from '../types/index';

export interface LoginResponse {
  token: string;
  user: {
    id: number;
    name: string;
    role: string;
    isInitialPassword: boolean;
    tlUser: { id: number; name: string } | null;
  };
}

/** ログイン。userId・password を送信し、JWT と認証ユーザー情報を受け取る。 */
export const login = (userId: string, password: string) =>
  apiClient.post<LoginResponse>('/auth/login', { userId, password });

/** パスワード変更。現在のパスワードと新しいパスワードを送信する。 */
export const changePassword = (currentPassword: string, newPassword: string) =>
  apiClient.post('/auth/change-password', { currentPassword, newPassword });

/**
 * 認証済みユーザー情報を取得する。アプリ起動時にセッション復元のため呼び出す。
 * JWT が有効であれば認証済みユーザー情報を返し、無効であれば 401 を返す。
 */
export const getMe = () =>
  apiClient.get<AuthUser>('/auth/me');
