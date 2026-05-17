/** DRAFT=下書き / PENDING_GOAL=提出済み・目標設定待ち / COMPLETED=目標設定完了 */
export type InventoryStatus = 'DRAFT' | 'PENDING_GOAL' | 'COMPLETED';
export type GoalCategory = 'IT_SKILL' | 'QUALIFICATION' | 'AD';
export type AchievementStatus = 'ACHIEVED' | 'PARTIAL' | 'NOT_ACHIEVED';

export interface FiscalYearRef {
  id: number;
  name: string;
}

export interface InventorySummary {
  id: number;
  fiscalYear: FiscalYearRef;
  status: InventoryStatus;
  submittedAt: string | null;
  goalCompletedAt: string | null;
}

export interface InventoryDetail {
  id: number;
  userId: number;
  fiscalYear: FiscalYearRef;
  status: InventoryStatus;
  submittedAt: string | null;
  goalReviewCompletedAt: string | null;
  goalCompletedAt: string | null;
}

export interface ItSkillDetailItem {
  id: number;
  itSkillId: number | null;
  itSkillName: string | null;
  customSkillName: string | null;
  skillLevelId: number;
  levelValue: number;
  remarks: string | null;
}

export interface QualificationDetailItem {
  id: number;
  qualificationId: number | null;
  qualificationName: string | null;
  qualificationCategoryName: string | null;
  customQualificationName: string | null;
  acquiredYearMonth: string | null;
  remarks: string | null;
}

export interface SeminarDetailItem {
  id: number;
  adSeminarId: number | null;
  adSeminarName: string | null;
  adSeminarCategoryId: number | null;
  adSeminarCategoryName: string | null;
  seminarName: string | null;
  seminarCategoryId: number | null;
  seminarCategoryName: string | null;
  attendedYearMonth: string | null;
  remarks: string | null;
}

export interface ComparisonItem {
  itSkillId: number | null;
  skillName: string;
  currentDetailId: number;
  currentLevelValue: number;
  currentRemarks: string | null;
  prevLevelValue: number | null;
  diff: number | null;
}

export interface ComparisonResponse {
  inventoryId: number;
  currentFiscalYear: string;
  prevFiscalYear: string | null;
  hasPrevYear: boolean;
  items: ComparisonItem[];
}

export interface GoalReviewItem {
  prevGoalId: number;
  goalCategory: GoalCategory;
  goalName: string;
  targetPeriod: string;
  reason: string | null;
  achievementStatus: AchievementStatus | null;
  reviewNote: string | null;
}

export interface GoalReviewResponse {
  prevFiscalYear: string | null;
  hasPrevGoals: boolean;
  items: GoalReviewItem[];
}

export interface GoalItem {
  id: number;
  goalCategory: GoalCategory;
  itSkillId: number | null;
  itSkillName: string | null;
  qualificationId: number | null;
  qualificationName: string | null;
  adSeminarId: number | null;
  adSeminarName: string | null;
  customName: string | null;
  targetPeriod: string;
  reason: string | null;
}

/** PENDING=待機中 / PROCESSING=分析中 / COMPLETED=完了 / FAILED=失敗 */
export type AiAnalysisStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface AiAnalysisResult {
  summary: string;
  strengths: string[];
  growth_areas: string[];
  expectation_fit: string;
  recommended_actions: string[];
}

export interface AiAnalysis {
  id: number;
  fiscalYearId: number;
  status: AiAnalysisStatus;
  analysisResult: AiAnalysisResult | null;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface DashboardResponse {
  user: { id: number; name: string; role: string };
  currentFiscalYear: { id: number; name: string } | null;
  currentInventory: {
    id: number;
    status: InventoryStatus;
    itSkillCount: number;
    qualificationCount: number;
    seminarCount: number;
    submittedAt: string | null;
    goalReviewCompletedAt: string | null;
    goalCompletedAt: string | null;
  } | null;
}
