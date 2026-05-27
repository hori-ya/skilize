import { useState, useRef, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../providers/AuthProvider';
import { useTranslation } from 'react-i18next';
import SkilizeLogo from '../../shared/ui/SkilizeLogo';
import AiSupportWidget from '../../features/ai-support/components/AiSupportWidget';

export default function NavBar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useTranslation('nav');
  const [adminOpen, setAdminOpen] = useState(false);
  // useRef: DOM 要素への参照を保持する。useState と異なり値変更時に再レンダリングを起こさない。
  // ドロップダウンの div 要素を参照し、クリックが要素の外側かどうかを判定するために使う。
  const dropdownRef = useRef<HTMLDivElement>(null);

  const ADMIN_MENU = [
    { label: t('adminMenu.allUsers'),     path: '/admin/users-inquiry' },
    { label: t('adminMenu.fiscalYear'),   path: '/master/fiscal-years' },
    { label: t('adminMenu.skillLevel'),   path: '/master/skill-levels' },
    { label: t('adminMenu.itSkill'),      path: '/master/it-skills' },
    { label: t('adminMenu.qualification'), path: '/master/qualifications' },
    { label: t('adminMenu.adSeminar'),    path: '/master/ad-seminars' },
    { label: t('adminMenu.userManagement'), path: '/master/users' },
  ];

  const roleLabel = (role: string) => {
    if (role === 'ADMIN') return t('role.admin');
    if (role === 'TL') return t('role.tl');
    return t('role.general');
  };

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      // contains(): クリックされた要素がドロップダウン内部かどうかを確認する DOM API
      // 内部クリックは無視し、外部クリック時のみドロップダウンを閉じる
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setAdminOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    // useEffect の戻り値はクリーンアップ関数。コンポーネントのアンマウント時に実行される。
    // イベントリスナーを解除しないとメモリリークの原因になる。
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  // location.pathname を deps に指定することで、ルート変遷のたびにドロップダウンを閉じる
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
          {t('menu.dashboard')}
        </button>

        {(user?.role === 'TL' || user?.role === 'ADMIN') && (
          <button
            className={`navbar__link${location.pathname.startsWith('/team') ? ' active' : ''}`}
            onClick={() => navigate('/team')}
          >
            {t('menu.team')}
          </button>
        )}

        {user?.role === 'ADMIN' && (
          <div className="navbar__dropdown" ref={dropdownRef}>
            <button
              className={`navbar__link navbar__link--has-arrow${isAdminActive ? ' active' : ''}${adminOpen ? ' open' : ''}`}
              onClick={() => setAdminOpen(v => !v)}
            >
              {t('menu.admin')}
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
        {user?.userId !== 'admin' && (
          <button className="navbar__logout-btn" onClick={() => navigate('/settings/password')}>
            {t('action.changePassword')}
          </button>
        )}
        <button className="navbar__logout-btn" onClick={handleLogout}>{t('action.logout')}</button>
        <AiSupportWidget />
      </div>
    </nav>
  );
}
