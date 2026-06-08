/*******************************************************************************
 * 機能ID      ：SHR
 * 機能名      ：共通
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * TL または ADMIN ロール専用のルートガードコンポーネント。
 * GENERAL ロールのユーザーはトップページへリダイレクトする。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
/** TL または ADMIN のみアクセス可能なルートガード。GENERAL はトップにリダイレクトする。 */
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../app/providers/AuthProvider';
import type { ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

export default function TlAdminRoute({ children }: Props) {
  const { user, isLoading } = useAuth();

  if (isLoading) {
    return <div className="loading-screen"><span>読み込み中...</span></div>;
  }

  if (!user) return <Navigate to="/login" replace />;
  if (user.isInitialPassword) return <Navigate to="/change-password" replace />;
  if (user.role !== 'TL' && user.role !== 'ADMIN') return <Navigate to="/" replace />;

  return <>{children}</>;
}
