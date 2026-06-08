/*******************************************************************************
 * 機能ID      ：DSH
 * 機能名      ：ダッシュボード
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * ダッシュボード機能で使用する型定義。
 * バックエンドの DashboardResponse と 1:1 対応する。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import type { InventoryStatus } from '../../inventory/types/index';

/**
 * ダッシュボード情報レスポンス。
 *
 * ログインユーザーの情報・現在年度・現在の棚卸状況を保持する。
 */
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
