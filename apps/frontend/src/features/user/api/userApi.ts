/*******************************************************************************
 * 機能ID      ：USR
 * 機能名      ：ユーザー管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * ユーザー管理・チーム照会の API 関数群。ADMIN 操作と TL/ADMIN 操作が混在している。
 * ロール制御はバックエンドで行い、フロントエンドでは UI 表示の出し分けのみを担う。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
/**
 * ユーザー管理・チーム照会の API。
 * ADMIN 操作（getUsers/createUser/updateUser/resetUserPassword）と TL/ADMIN 操作が混在している。
 * ロール制御はバックエンドで行うため、フロントエンドでは UI 表示の出し分けのみを担う。
 * 期待情報は features/expectation/api/expectationApi に、AI分析は features/ai/api/aiAnalysisApi に分離している。
 */
import apiClient from '../../../shared/api/client';
import type { UserAdmin } from '../../auth/types/index';
import type { TeamMember } from '../types/index';
import type { InventorySummary } from '../../inventory/types/index';

/** 全ユーザー一覧を取得する（ADMIN のみ）。 */
export const getUsers = () => apiClient.get<UserAdmin[]>('/users');

/** ユーザーを新規作成する（ADMIN のみ）。初期パスワードはバックエンドでユーザーIDに設定される。 */
export const createUser = (data: {
  userId: string;
  name: string;
  email: string | null;
  role: string;
  tlUserId: number | null;
}) => apiClient.post<UserAdmin>('/users', data);

/** ユーザー情報を更新する（ADMIN のみ）。 */
export const updateUser = (id: number, data: {
  name: string;
  email: string | null;
  role: string;
  tlUserId: number | null;
  active: boolean;
}) => apiClient.put<UserAdmin>(`/users/${id}`, data);

/**
 * パスワードをリセットする（ADMIN のみ）。
 * 仮パスワード（= ユーザーID）が返るので、管理者がユーザーに口頭等で伝える。
 */
export const resetUserPassword = (id: number) =>
  apiClient.post<{ temporaryPassword: string }>(`/users/${id}/reset-password`);

/** チームメンバー一覧を取得する。ADMIN は全ユーザー、TL は担当メンバーのみ返る。 */
export const getTeamMembers = () =>
  apiClient.get<TeamMember[]>('/users/me/team-members');

/** 指定ユーザーの棚卸一覧を取得する（TL/ADMIN のみ）。 */
export const getMemberInventories = (userId: number) =>
  apiClient.get<InventorySummary[]>(`/users/${userId}/inventories`);
