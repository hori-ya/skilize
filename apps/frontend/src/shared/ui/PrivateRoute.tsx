/**
 * 認証ガード。未ログイン → /login、初期PWのまま通常ページへアクセス → /change-password にリダイレクトする。
 * requireInitialPassword=true の場合は初期PW保持者専用ページ（/change-password）として機能する。
 */
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../app/providers/AuthProvider';
import type { ReactNode } from 'react';

interface Props {
  children: ReactNode;
  requireInitialPassword?: boolean;
}

export default function PrivateRoute({ children, requireInitialPassword = false }: Props) {
  // useAuth() で AuthContext から認証済みユーザーと読み込み状態を取得する
  const { user, isLoading } = useAuth();

  // アプリ起動時の /auth/me 通信中は判定を行わず読み込み画面を表示する
  // （isLoading=true のまま判定すると、セッション復元前に誤って /login にリダイレクトされる）
  if (isLoading) {
    return (
      <div className="loading-screen">
        <span>読み込み中...</span>
      </div>
    );
  }

  // 未認証（トークンなし・期限切れ等）の場合はログインページへ
  // replace: ブラウザの戻るボタンで元のページに戻れないようにする
  if (!user) return <Navigate to="/login" replace />;

  // requireInitialPassword=true: 初期PW変更ページ専用。変更済みユーザーはトップへ送る（再表示を防ぐ）
  if (requireInitialPassword && !user.isInitialPassword) {
    return <Navigate to="/" replace />;
  }

  // requireInitialPassword=false（通常ページ）: 初期PW保持者はパスワード変更を強制する
  if (!requireInitialPassword && user.isInitialPassword) {
    return <Navigate to="/change-password" replace />;
  }

  // すべてのチェックを通過した場合のみ子コンポーネントを描画する
  // <>{children}</> は React.Fragment の省略記法。余分な DOM 要素を追加しない。
  return <>{children}</>;
}
