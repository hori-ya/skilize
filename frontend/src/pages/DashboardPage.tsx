import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';

export default function DashboardPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="dashboard-page">
      <header className="dashboard-header">
        <span className="dashboard-logo">Skilize</span>
        <div className="dashboard-user">
          <span>{user?.name}（{user?.role}）</span>
          <button className="btn btn-outline" onClick={handleLogout}>ログアウト</button>
        </div>
      </header>
      <main className="dashboard-main">
        <p className="placeholder-text">ダッシュボード（準備中）</p>
      </main>
    </div>
  );
}
