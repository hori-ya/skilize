/*******************************************************************************
 * 機能ID      ：USR
 * 機能名      ：ユーザー管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * チームメンバー一覧ページ。TL は担当メンバーの棚卸状況を、ADMIN は全メンバーを確認できる。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getTeamMembers } from '../api/userApi';
import type { TeamMember } from '../types/index';
import NavBar from '../../../app/layouts/NavBar';
import { IconArrowRight } from '../../../shared/ui/Icons';
import StickyHorizontalScroll from '../../../shared/ui/StickyHorizontalScroll';
import { useTranslation } from 'react-i18next';

const STATUS_ICON: Record<string, string> = {
  COMPLETED: '●',
  PENDING_GOAL: '○',
  DRAFT: '△',
};

const STATUS_KEY: Record<string, string> = {
  COMPLETED: 'status.completed',
  PENDING_GOAL: 'status.pendingGoal',
  DRAFT: 'status.draft',
};

const ROLE_KEY: Record<string, string> = {
  GENERAL: 'role.general',
  TL: 'role.tl',
  ADMIN: 'role.admin',
};

/**
 * チームメンバー一覧ページ。
 *
 * TL は担当メンバーの棚卸状況を確認できる。
 * ADMIN は全メンバーを確認できる（バックエンドでロール制御）。
 */
export default function TeamMemberListPage() {
  const navigate = useNavigate();
  const { t } = useTranslation('user');
  const [members, setMembers] = useState<TeamMember[]>([]);
  const [loading, setLoading] = useState(true);

  // 初期表示時にチームメンバー一覧を取得する
  useEffect(() => {
    getTeamMembers()
      .then(res => setMembers(res.data))
      .finally(() => setLoading(false));
  }, []);

  let content: React.ReactNode;
  if (loading) {
    content = <div className="loading">{t('loading')}</div>;
  } else if (members.length === 0) {
    content = <div className="info-card"><p>{t('teamMemberList.noMembers')}</p></div>;
  } else {
    const rows: React.ReactNode[] = [];
    for (const member of members) {
      let roleLabelKey = member.role;
      if (ROLE_KEY[member.role] != null) {
        roleLabelKey = ROLE_KEY[member.role];
      }

      let statusCell: React.ReactNode;
      if (member.currentInventory != null) {
        const inv = member.currentInventory;
        let statusLabelKey = inv.status;
        if (STATUS_KEY[inv.status] != null) {
          statusLabelKey = STATUS_KEY[inv.status];
        }
        statusCell = (
          <span className={`team-status team-status--${inv.status.toLowerCase()}`}>
            {STATUS_ICON[inv.status]}{' '}
            {t(statusLabelKey)}
          </span>
        );
      } else {
        statusCell = <span className="team-status team-status--none">{t('teamMemberList.noInventory')}</span>;
      }

      let fiscalYearName = '—';
      if (member.currentInventory != null) {
        fiscalYearName = member.currentInventory.fiscalYear.name;
      }

      rows.push(
        <tr key={member.id}>
          <td>{member.name}</td>
          <td>{t(roleLabelKey)}</td>
          <td>{statusCell}</td>
          <td>{fiscalYearName}</td>
          <td>
            <button
              className="btn btn-sm btn-secondary"
              onClick={() => navigate(`/team/${member.id}`)}
            >
              <IconArrowRight size={12} />
              {t('teamMemberList.table.detailButton')}
            </button>
          </td>
        </tr>,
      );
    }
    content = (
      <StickyHorizontalScroll className="master-table-wrap">
        <table className="master-table">
          <thead>
            <tr>
              <th>{t('teamMemberList.table.name')}</th>
              <th>{t('teamMemberList.table.role')}</th>
              <th>{t('teamMemberList.table.currentStatus')}</th>
              <th>{t('teamMemberList.table.fiscalYear')}</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {rows}
          </tbody>
        </table>
      </StickyHorizontalScroll>
    );
  }

  return (
    <div className="team-page">
      <NavBar />
      <main className="team-main">
        <button className="page-back-btn" onClick={() => navigate('/')}>{t('teamMemberList.backButton')}</button>
        <h1 className="page-title">{t('teamMemberList.title')}</h1>
        {content}
      </main>
    </div>
  );
}
