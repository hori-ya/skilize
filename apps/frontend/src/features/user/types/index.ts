/*******************************************************************************
 * 機能ID      ：USR
 * 機能名      ：ユーザー管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * ユーザー管理機能に関する型定義。チームメンバー情報の型を定義する。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
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
