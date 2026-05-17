import type { FiscalYearRef, InventoryStatus } from '../../inventory/types/index';

export interface UserExpectation {
  tlExpectation: string | null;
  companyExpectation: string | null;
}

export interface TeamMember {
  id: number;
  userId: string;
  name: string;
  email: string | null;
  role: string;
  tlUserId: number | null;
  tlName: string | null;
  isActive: boolean;
  currentInventory: { id: number; fiscalYear: FiscalYearRef; status: InventoryStatus; } | null;
}
