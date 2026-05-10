import apiClient from './client';
import type { UserAdmin } from '../types/auth';
import type { TeamMember } from '../types/user';
import type { InventorySummary } from '../types/inventory';

export const getUsers = () => apiClient.get<UserAdmin[]>('/users');

export const createUser = (data: {
  userId: string;
  name: string;
  email: string | null;
  role: string;
  tlUserId: number | null;
}) => apiClient.post<UserAdmin>('/users', data);

export const updateUser = (id: number, data: {
  name: string;
  email: string | null;
  role: string;
  tlUserId: number | null;
  active: boolean;
}) => apiClient.put<UserAdmin>(`/users/${id}`, data);

export const resetUserPassword = (id: number) =>
  apiClient.post<{ temporaryPassword: string }>(`/users/${id}/reset-password`);

export const getTeamMembers = () =>
  apiClient.get<TeamMember[]>('/users/me/team-members');

export const getMemberInventories = (userId: number) =>
  apiClient.get<InventorySummary[]>(`/users/${userId}/inventories`);
