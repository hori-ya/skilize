/*******************************************************************************
 * 機能ID      ：SHR
 * 機能名      ：共通
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * マスタデータ（会計年度・スキルレベル・ITスキル・資格・セミナーなど）の型定義。
 * バックエンドのレスポンス JSON と 1:1 対応する。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
/**
 * マスタデータの型定義。複数 feature（inventory/master/team）から共有するため shared/types に配置する。
 * バックエンドのレスポンス JSON と 1:1 対応する。
 */

export interface FiscalYear {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  inputStartDate: string | null;  // 棚卸の入力受付開始日（null=制限なし）
  inputEndDate: string | null;    // 棚卸の入力受付終了日（null=制限なし）
  isActive: boolean;
}

export interface FiscalYearSettings {
  fiscalYearStartMonth: number;
}

export interface FiscalYearRequest {
  name: string;
  startDate: string;
  endDate: string;
  inputStartDate: string | null;
  inputEndDate: string | null;
  active?: boolean;
}

export interface SkillLevel {
  id: number;
  levelValue: number;
  description: string;
  isActive: boolean;
  scoreWeight: number;
}

export interface ItSkillCategory {
  id: number;
  parentId: number | null;
  name: string;
  level: number;
  sortOrder: number;
  isActive: boolean;
}

/**
 * ITスキル。分類は最大3階層で、バックエンドが大分類（category1）を解決して返す。
 * category1Id/Name: 大分類（レベル1）— グループ化・レーダーチャートの軸に使用
 * category2Name:   中分類（レベル2）— 表示用（IDは不要なため名前のみ）
 * category3Name:   小分類（レベル3）— 表示用（IDは不要なため名前のみ）
 */
export interface ItSkill {
  id: number;
  categoryId: number;          // スキルが直接属するカテゴリID（level=2 or 3 の場合もある）
  category1Id: number | null;  // 大分類ID（バックエンドが階層を遡って解決）
  category1Name: string | null;
  category1SortOrder: number;
  category2Name: string | null;
  category3Name: string | null;
  name: string;
  description: string | null;
  sortOrder: number;
  isActive: boolean;
}

export interface QualificationCategory {
  id: number;
  name: string;
  sortOrder: number;
  isActive: boolean;
}

export interface Qualification {
  id: number;
  categoryId: number | null;
  categoryName: string | null;
  name: string;
  description: string | null;
  sortOrder: number;
  isActive: boolean;
}

export interface AdSeminarCategory {
  id: number;
  name: string;
  sortOrder: number;
  isActive: boolean;
}

export interface AdSeminar {
  id: number;
  categoryId: number | null;
  categoryName: string | null;
  name: string;
  description: string | null;
  sortOrder: number;
  isActive: boolean;
}

export interface SeminarCategory {
  id: number;
  name: string;
  sortOrder: number;
  isActive: boolean;
}

export interface CustomUnregisteredItem {
  customName: string;
  usageCount: number;
}

export interface MasterImportResult {
  created: number;
  updated: number;
  deleted: number;
}

export interface MasterImportError {
  sheet: string;
  row: number;
  column: string;
  message: string;
}

export interface MasterImportErrorResponse {
  code: string;
  message: string;
  errors: MasterImportError[];
}
