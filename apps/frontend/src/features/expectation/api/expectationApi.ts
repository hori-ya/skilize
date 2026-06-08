/*******************************************************************************
 * 機能ID      ：EXP
 * 機能名      ：期待コメント
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * 期待コメント CRUD API。TLによる期待コメントと会社からの期待コメントを
 * 取得・更新する。TL期待は担当TLのみ、会社期待はADMINのみ更新可能。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import apiClient from '../../../shared/api/client';
import type { UserExpectation } from '../types/index';

/** 指定ユーザーへの期待情報（TL期待・会社期待）を取得する。 */
export const getExpectations = (userId: number) =>
  apiClient.get<UserExpectation>(`/users/${userId}/expectations`);

/** 担当TLによる期待コメントを保存する（担当TLのみ）。 */
export const saveTlExpectation = (userId: number, expectation: string) =>
  apiClient.put<UserExpectation>(`/users/${userId}/expectations/tl`, { expectation });

/** 会社からの期待コメントを保存する（ADMIN のみ）。 */
export const saveCompanyExpectation = (userId: number, expectation: string) =>
  apiClient.put<UserExpectation>(`/users/${userId}/expectations/company`, { expectation });
