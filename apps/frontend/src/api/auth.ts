import apiClient from './client';
import type { AuthUser } from '../types/auth';

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

export const login = (userId: string, password: string) =>
  apiClient.post<LoginResponse>('/auth/login', { userId, password });

export const changePassword = (currentPassword: string, newPassword: string) =>
  apiClient.post('/auth/change-password', { currentPassword, newPassword });

export const getMe = () =>
  apiClient.get<AuthUser>('/auth/me');
