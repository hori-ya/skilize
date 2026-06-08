/*******************************************************************************
 * 機能ID      ：AI
 * 機能名      ：AI機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * AIキャリア分析取得 API。ログインユーザー自身またはメンバーの
 * AIキャリア分析一覧を取得する。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import apiClient from '../../../shared/api/client';
import type { AiAnalysis } from '../types';

/**
 * ログインユーザー自身のAIキャリア分析一覧取得。
 *
 * @returns AI分析エンティティの配列（新しい順）
 */
export const getMyAiAnalyses = () =>
  apiClient.get<AiAnalysis[]>('/users/me/ai-analyses');

/**
 * 指定メンバーのAIキャリア分析一覧取得（TL/ADMIN 向け）。
 *
 * @param userId 取得対象ユーザーの DB ID
 * @returns AI分析エンティティの配列（新しい順）
 */
export const getMemberAiAnalyses = (userId: number) =>
  apiClient.get<AiAnalysis[]>(`/users/${userId}/ai-analyses`);
