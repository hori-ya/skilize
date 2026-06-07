import type { InventoryStatus } from '../../inventory/types/index';

export interface DashboardResponse {
  user: { id: number; name: string; role: string };
  currentFiscalYear: { id: number; name: string; inputStartDate: string | null; inputEndDate: string | null } | null;
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
