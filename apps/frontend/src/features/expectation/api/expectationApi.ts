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
