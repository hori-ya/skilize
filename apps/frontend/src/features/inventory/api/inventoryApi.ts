/**
 * 棚卸関連 API。明細（ITスキル・資格・セミナー）は PUT で全件洗い替えを行う。
 * completeGoal は目標件数の条件（ITスキル/資格 ≥1・AD ≥2）を満たさない場合 422 を返す。
 * ダッシュボードは features/dashboard/api、帳票は features/report/api、AI分析は features/ai/api に分離している。
 */
import apiClient from '../../../shared/api/client';
import type {
  InventorySummary, InventoryDetail,
  ItSkillDetailItem, QualificationDetailItem, SeminarDetailItem,
  ComparisonResponse, GoalReviewResponse,
  GoalItem,
} from '../types/index';

export const getMyInventories = () => apiClient.get<InventorySummary[]>('/inventories/mine');

export const createInventory = (fiscalYearId: number) =>
  apiClient.post<InventorySummary>('/inventories', { fiscalYearId });

export const getInventory = (id: number) => apiClient.get<InventoryDetail>(`/inventories/${id}`);

export const getItSkillDetails = (id: number) =>
  apiClient.get<{ items: ItSkillDetailItem[] }>(`/inventories/${id}/it-skill-details`);

export const getQualificationDetails = (id: number) =>
  apiClient.get<{ items: QualificationDetailItem[] }>(`/inventories/${id}/qualification-details`);

export const getSeminarDetails = (id: number) =>
  apiClient.get<{ items: SeminarDetailItem[] }>(`/inventories/${id}/seminar-details`);

export const saveItSkillDetails = (id: number, items: Array<{
  id?: number | null; itSkillId?: number | null; customSkillName?: string | null;
  skillLevelId: number; remarks?: string | null;
}>) => apiClient.put<{ items: ItSkillDetailItem[] }>(`/inventories/${id}/it-skill-details`, { items });

export const patchItSkillRemarks = (inventoryId: number, detailId: number, remarks: string) =>
  apiClient.patch<{ id: number; remarks: string }>(
    `/inventories/${inventoryId}/it-skill-details/${detailId}`, { remarks });

export const saveQualificationDetails = (id: number, items: Array<{
  id?: number | null; qualificationId?: number | null; customQualificationName?: string | null;
  acquiredYearMonth?: string | null; remarks?: string | null;
}>) => apiClient.put<{ items: QualificationDetailItem[] }>(`/inventories/${id}/qualification-details`, { items });

export const saveSeminarDetails = (id: number, items: Array<{
  id?: number | null; adSeminarId?: number | null; seminarName?: string | null;
  seminarCategoryId?: number | null; attendedYearMonth?: string | null; remarks?: string | null;
}>) => apiClient.put<{ items: SeminarDetailItem[] }>(`/inventories/${id}/seminar-details`, { items });

export const submitInventory = (id: number) =>
  apiClient.post<{ id: number; status: string; submittedAt: string }>(`/inventories/${id}/submit`);

export const getComparison = (id: number) =>
  apiClient.get<ComparisonResponse>(`/inventories/${id}/comparison`);

export const getGoalReview = (id: number) =>
  apiClient.get<GoalReviewResponse>(`/inventories/${id}/goal-review`);

export const saveGoalReview = (id: number, items: Array<{
  prevGoalId: number; achievementStatus?: string | null; reviewNote?: string | null;
}>) => apiClient.put<GoalReviewResponse>(`/inventories/${id}/goal-review`, { items });

export const completeGoalReview = (id: number) =>
  apiClient.post<{ id: number; goalReviewCompletedAt: string }>(`/inventories/${id}/goal-review/complete`);

export const getGoals = (id: number) =>
  apiClient.get<{ items: GoalItem[] }>(`/inventories/${id}/goals`);

export const saveGoals = (id: number, items: Array<{
  id?: number | null; goalCategory: string; itSkillId?: number | null;
  qualificationId?: number | null; adSeminarId?: number | null;
  customName?: string | null; targetPeriod: string; reason?: string | null;
}>) => apiClient.put<{ items: GoalItem[] }>(`/inventories/${id}/goals`, { items });

export const completeGoal = (id: number) =>
  apiClient.post<{ id: number; status: string; goalCompletedAt: string }>(`/inventories/${id}/goals/complete`);

