import type { FiscalYearRef, InventoryStatus } from './inventory';

export interface TeamMember {
  id: number;
  userId: string;
  name: string;
  email: string | null;
  role: string;
  tlUserId: number | null;
  tlName: string | null;
  isActive: boolean;
  currentInventory: {
    id: number;
    fiscalYear: FiscalYearRef;
    status: InventoryStatus;
  } | null;
}
