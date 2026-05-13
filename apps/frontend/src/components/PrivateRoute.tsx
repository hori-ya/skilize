import { Navigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import type { ReactNode } from 'react';

interface Props {
  children: ReactNode;
  requireInitialPassword?: boolean;
}

export default function PrivateRoute({ children, requireInitialPassword = false }: Props) {
  const { user, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="loading-screen">
        <span>読み込み中...</span>
      </div>
    );
  }

  if (!user) return <Navigate to="/login" replace />;

  if (requireInitialPassword && !user.isInitialPassword) {
    return <Navigate to="/" replace />;
  }

  if (!requireInitialPassword && user.isInitialPassword) {
    return <Navigate to="/change-password" replace />;
  }

  return <>{children}</>;
}
