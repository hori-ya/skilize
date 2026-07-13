/*******************************************************************************
 * 機能ID      ：SHR
 * 機能名      ：共通
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * アプリ共通ナビゲーションバー。ロールに応じてメニュー項目を出し分け、
 * ログアウト・パスワード変更・ADMIN ドロップダウンメニューを提供する。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import { useState, useRef, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../providers/AuthProvider';
import { useTranslation } from 'react-i18next';
import SkilizeLogo from '../../shared/ui/SkilizeLogo';
import AiSupportWidget from '../../features/ai/components/AiSupportWidget';

/**
 * アプリ共通ナビゲーションバー。
 *
 * ロール（GENERAL / TL / ADMIN）に応じてメニュー項目を出し分ける。
 * ADMIN はドロップダウンメニューからマスタ管理・ユーザー照会へアクセスできる。
 */
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

  let isAdminActive = false;
  for (const item of ADMIN_MENU) {
    if (location.pathname.startsWith(item.path)) {
      isAdminActive = true;
      break;
    }
  }

  let dashboardLinkClassName = 'navbar__link';
  if (location.pathname === '/') {
    dashboardLinkClassName += ' active';
  }

  let teamLinkClassName = 'navbar__link';
  if (location.pathname.startsWith('/team')) {
    teamLinkClassName += ' active';
  }

  let adminLinkClassName = 'navbar__link navbar__link--has-arrow';
  if (isAdminActive) {
    adminLinkClassName += ' active';
  }
  if (adminOpen) {
    adminLinkClassName += ' open';
  }

  const adminMenuButtons: React.ReactNode[] = [];
  for (const item of ADMIN_MENU) {
    let itemClassName = 'navbar__dropdown-item';
    if (location.pathname === item.path) {
      itemClassName += ' active';
    }
    adminMenuButtons.push(
      <button
        key={item.path}
        className={itemClassName}
        onClick={() => navigate(item.path)}
      >
        {item.label}
      </button>,
    );
  }

  let userName = '';
  let userRole = '';
  let showChangePasswordButton = true;
  if (user != null) {
    userName = user.name;
    userRole = user.role;
    showChangePasswordButton = user.userId !== 'admin';
  }

  const showTeamLink = user != null && (user.role === 'TL' || user.role === 'ADMIN');
  const showAdminMenu = user != null && user.role === 'ADMIN';

  return (
    <nav className="navbar">
      <div className="navbar__left">
        <span className="navbar__logo" onClick={() => navigate('/')}>
          <SkilizeLogo size={20} />
          Skilize
        </span>

        <button
          className={dashboardLinkClassName}
          onClick={() => navigate('/')}
        >
          {t('menu.dashboard')}
        </button>

        {showTeamLink && (
          <button
            className={teamLinkClassName}
            onClick={() => navigate('/team')}
          >
            {t('menu.team')}
          </button>
        )}

        {showAdminMenu && (
          <div className="navbar__dropdown" ref={dropdownRef}>
            <button
              className={adminLinkClassName}
              onClick={() => setAdminOpen(v => !v)}
            >
              {t('menu.admin')}
              <span className="navbar__arrow">▾</span>
            </button>
            {adminOpen && (
              <div className="navbar__dropdown-menu">
                {adminMenuButtons}
              </div>
            )}
          </div>
        )}
      </div>

      <div className="navbar__right">
        <span className="navbar__user">
          {userName}（{roleLabel(userRole)}）
        </span>
        {showChangePasswordButton && (
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
