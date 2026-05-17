import apiClient from '../../../shared/api/client';
import type { UserAdmin } from '../../auth/types/index';
import type { TeamMember, UserExpectation } from '../types/index';
import type { InventorySummary, AiAnalysis } from '../../inventory/types/index';

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

export const getExpectations = (userId: number) =>
  apiClient.get<UserExpectation>(`/users/${userId}/expectations`);

export const saveTlExpectation = (userId: number, expectation: string) =>
  apiClient.put<UserExpectation>(`/users/${userId}/expectations/tl`, { expectation });

export const saveCompanyExpectation = (userId: number, expectation: string) =>
  apiClient.put<UserExpectation>(`/users/${userId}/expectations/company`, { expectation });

export const getMemberAiAnalyses = (userId: number) =>
  apiClient.get<AiAnalysis[]>(`/users/${userId}/ai-analyses`);
