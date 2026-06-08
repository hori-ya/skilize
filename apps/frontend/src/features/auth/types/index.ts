/*******************************************************************************
 * 機能ID      ：AUTH
 * 機能名      ：認証機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * 認証機能に関する型定義。ロール・認証ユーザー情報・ユーザー管理用の型を定義する。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
/** ロール定義: GENERAL=一般ユーザー / TL=チームリーダー / ADMIN=管理者 */
export type Role = 'GENERAL' | 'TL' | 'ADMIN';

/** ユーザー管理画面（ADMIN用）で使用するユーザー情報。tlName は表示用の上長氏名。 */
export interface UserAdmin {
  id: number;
  userId: string;
  name: string;
  email: string | null;
  role: Role;
  tlUserId: number | null;
  tlName: string | null;
  isInitialPassword: boolean;
  isActive: boolean;
  createdAt: string | null;
}

export interface TlUser {
  id: number;
  name: string;
}

/** ログイン中ユーザーの認証情報。AuthProvider で保持し useAuth() で取得する。 */
export interface AuthUser {
  id: number;
  userId: string;
  name: string;
  email: string | null;
  role: Role;
  isInitialPassword: boolean;
  tlUser: TlUser | null;
  isActive: boolean;
}
