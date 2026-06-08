/*******************************************************************************
 * 機能ID      ：SHR
 * 機能名      ：共通
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * ADMIN ロール専用のルートガードコンポーネント。
 * TL / GENERAL ロールのユーザーはトップページへリダイレクトする。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
/** ADMIN のみアクセス可能なルートガード。TL/GENERAL はトップにリダイレクトする。 */
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../app/providers/AuthProvider';
import type { ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

export default function AdminRoute({ children }: Props) {
  const { user, isLoading } = useAuth();

  if (isLoading) {
    return <div className="loading-screen"><span>読み込み中...</span></div>;
  }

  if (!user) return <Navigate to="/login" replace />;
  if (user.isInitialPassword) return <Navigate to="/change-password" replace />;
  if (user.role !== 'ADMIN') return <Navigate to="/" replace />;

  return <>{children}</>;
}
