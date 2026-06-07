// features 間の型参照: user の型が inventory の型を参照する（設計上許容されたクロス feature 参照）
import type { FiscalYearRef, InventoryStatus } from '../../inventory/types/index';

/** チームメンバー情報。TL が自チームメンバーの棚卸状況を確認するために使用する。 */
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
