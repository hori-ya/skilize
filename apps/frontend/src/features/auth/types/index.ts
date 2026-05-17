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
