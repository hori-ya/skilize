export type Role = 'GENERAL' | 'TL' | 'ADMIN';

export interface TlUser {
  id: number;
  name: string;
}

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
