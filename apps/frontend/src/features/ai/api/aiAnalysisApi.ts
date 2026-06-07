import apiClient from '../../../shared/api/client';
import type { AiAnalysis } from '../types';

export const getMyAiAnalyses = () =>
  apiClient.get<AiAnalysis[]>('/users/me/ai-analyses');

export const getMemberAiAnalyses = (userId: number) =>
  apiClient.get<AiAnalysis[]>(`/users/${userId}/ai-analyses`);
