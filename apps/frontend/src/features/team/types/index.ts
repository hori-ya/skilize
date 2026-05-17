// features 間の型参照: team の型が inventory の型を参照する（設計上許容されたクロス feature 参照）
import type { FiscalYearRef, InventoryStatus } from '../../inventory/types/index';

/** TL/会社からの期待コメント。AI 分析の方向性づけに使用される（分析結果には直接引用しない）。 */
export interface UserExpectation {
  tlExpectation: string | null;
  companyExpectation: string | null;
}

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
