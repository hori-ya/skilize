import apiClient from '../../../shared/api/client';
import type { DashboardResponse } from '../types/index';

export const getDashboard = () => apiClient.get<DashboardResponse>('/dashboard');
