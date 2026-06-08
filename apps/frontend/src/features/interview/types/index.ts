/*******************************************************************************
 * 機能ID      ：IVW
 * 機能名      ：面談メモ
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * 面談メモ機能で使用する型定義。
 * 面談メモ本体（InterviewMemo）と明細ごとのメモ（DetailNoteItem）を定義する。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
/** 面談ノートの対象種別（ITスキル明細・資格明細・セミナー明細・目標） */
export type DetailType = 'IT_SKILL' | 'QUALIFICATION' | 'SEMINAR' | 'GOAL';

export interface DetailNoteItem {
  id: number;
  detailType: DetailType;
  detailId: number;
  note: string;
}

/**
 * 面談メモ。interviewerId は面談実施者（TL/ADMIN）の ID。
 * generalNote は全体備忘録、detailNotes は各明細ごとのメモ。
 */
export interface InterviewMemo {
  id: number;
  inventoryId: number;
  interviewerId: number;
  interviewerName: string;
  generalNote: string | null;
  detailNotes: DetailNoteItem[];
}
