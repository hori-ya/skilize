import apiClient from '../../../shared/api/client';
import type { InterviewMemo, DetailType } from '../types';

export const getInterview = (inventoryId: number) =>
  apiClient.get<InterviewMemo>(`/interviews/inventory/${inventoryId}`);

export const saveInterview = (
  inventoryId: number,
  data: {
    generalNote: string | null;
    detailNotes: { detailType: DetailType; detailId: number; note: string }[];
  },
) => apiClient.put<InterviewMemo>(`/interviews/inventory/${inventoryId}`, data);

export const getPrevYearInterview = (inventoryId: number) =>
  apiClient.get<InterviewMemo>(`/interviews/inventory/${inventoryId}/prev-year`);
