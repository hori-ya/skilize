/*******************************************************************************
 * 機能ID      ：IVW
 * 機能名      ：面談メモ
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * 面談メモ API。TL/ADMIN のみ使用可。面談者（interviewerId）ごとにメモを管理し、
 * 他者のメモは取得できない。UI は MemberDetailPage に実装されており、
 * このファイルは API 呼び出しのみを担当する。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
/**
 * 面談メモ API。TL/ADMIN のみ使用可。interviewerId（面談者）ごとにメモを管理し、他者のメモは取得できない。
 * UI は features/user/pages/MemberDetailPage.tsx に実装されており、このファイルは API 呼び出しのみ担当する。
 */
import apiClient from '../../../shared/api/client';
import type { InterviewMemo, DetailType } from '../types';

/**
 * 面談メモ取得。
 *
 * @param inventoryId 棚卸 ID
 * @returns ログインユーザーが記録した面談メモ
 */
export const getInterview = (inventoryId: number) =>
  apiClient.get<InterviewMemo>(`/interviews/inventory/${inventoryId}`);

/**
 * 面談メモ保存（作成または更新）。
 *
 * @param inventoryId 棚卸 ID
 * @param data 全体メモと明細ごとのメモ
 * @returns 保存後の面談メモ
 */
export const saveInterview = (
  inventoryId: number,
  data: {
    generalNote: string | null;
    detailNotes: { detailType: DetailType; detailId: number; note: string }[];
  },
) => apiClient.put<InterviewMemo>(`/interviews/inventory/${inventoryId}`, data);

/**
 * 前年度の面談メモ取得。目標設定画面で前年のコメントを参照するために使用する。
 *
 * @param inventoryId 棚卸 ID
 * @returns 前年度の面談メモ
 */
export const getPrevYearInterview = (inventoryId: number) =>
  apiClient.get<InterviewMemo>(`/interviews/inventory/${inventoryId}/prev-year`);
