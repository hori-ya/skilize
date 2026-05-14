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
