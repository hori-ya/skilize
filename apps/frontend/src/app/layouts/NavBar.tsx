import { useState, useRef, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../providers/AuthProvider';
import SkilizeLogo from '../../shared/ui/SkilizeLogo';

const ADMIN_MENU = [
  { label: '全ユーザー照会', path: '/admin/users-inquiry' },
  { label: '年度マスタ',     path: '/master/fiscal-years' },
  { label: 'レベルマスタ',   path: '/master/skill-levels' },
  { label: 'ITスキルマスタ', path: '/master/it-skills' },
  { label: '参考資格マスタ', path: '/master/qualifications' },
  { label: 'ADマスタ',       path: '/master/ad-seminars' },
  { label: 'ユーザー管理',   path: '/master/users' },
];

const roleLabel = (role: string) => {
  if (role === 'ADMIN') return '管理者';
  if (role === 'TL') return 'TL';
  return '一般';
};

export default function NavBar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [adminOpen, setAdminOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setAdminOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  // Close dropdown on route change
  useEffect(() => { setAdminOpen(false); }, [location.pathname]);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isAdminActive = ADMIN_MENU.some(item => location.pathname.startsWith(item.path));

  return (
    <nav className="navbar">
      <div className="navbar__left">
        <span className="navbar__logo" onClick={() => navigate('/')}>
          <SkilizeLogo size={20} />
          Skilize
        </span>

        <button
          className={`navbar__link${location.pathname === '/' ? ' active' : ''}`}
          onClick={() => navigate('/')}
        >
          ダッシュボード
        </button>

        {(user?.role === 'TL' || user?.role === 'ADMIN') && (
          <button
            className={`navbar__link${location.pathname.startsWith('/team') ? ' active' : ''}`}
            onClick={() => navigate('/team')}
          >
            チーム照会
          </button>
        )}

        {user?.role === 'ADMIN' && (
          <div className="navbar__dropdown" ref={dropdownRef}>
            <button
              className={`navbar__link navbar__link--has-arrow${isAdminActive ? ' active' : ''}${adminOpen ? ' open' : ''}`}
              onClick={() => setAdminOpen(v => !v)}
            >
              管理
              <span className="navbar__arrow">▾</span>
            </button>
            {adminOpen && (
              <div className="navbar__dropdown-menu">
                {ADMIN_MENU.map(item => (
                  <button
                    key={item.path}
                    className={`navbar__dropdown-item${location.pathname === item.path ? ' active' : ''}`}
                    onClick={() => navigate(item.path)}
                  >
                    {item.label}
                  </button>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      <div className="navbar__right">
        <span className="navbar__user">
          {user?.name}（{roleLabel(user?.role ?? '')}）
        </span>
        <button className="navbar__logout-btn" onClick={handleLogout}>ログアウト</button>
      </div>
    </nav>
  );
}
