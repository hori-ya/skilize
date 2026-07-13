/*******************************************************************************
 * 機能ID      ：USR
 * 機能名      ：ユーザー管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * 全ユーザー照会ページ（ADMIN 専用）。名前・チーム・ロール・棚卸ステータスでフィルタリングできる。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { getTeamMembers } from '../api/userApi';
import type { TeamMember } from '../types/index';
import NavBar from '../../../app/layouts/NavBar';
import { IconX, IconArrowRight } from '../../../shared/ui/Icons';
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
 * 全ユーザー照会ページ（ADMIN 専用）。
 *
 * 名前・チーム・ロール・棚卸ステータスのフィルタリング機能を持つ。
 * 各ユーザーの詳細はメンバー詳細ページへ遷移して確認できる。
 */
export default function AllUserListPage() {
  const navigate = useNavigate();
  const { t } = useTranslation('user');

  const [members, setMembers] = useState<TeamMember[]>([]);
  const [loading, setLoading] = useState(true);

  // Filter state
  const [filterName, setFilterName] = useState('');
  const [filterTlId, setFilterTlId] = useState<number | ''>('');
  const [filterRole, setFilterRole] = useState('');
  const [filterStatus, setFilterStatus] = useState('');

  // 初期表示時に全ユーザー（チームメンバー）一覧を取得する
  useEffect(() => {
    getTeamMembers()
      .then(res => setMembers(res.data))
      .finally(() => setLoading(false));
  }, []);

  // Build TL list for dropdown（名前の昇順でソート）
  const tlOptions = useMemo(() => {
    const tlMap = new Map<number, string>();
    for (const m of members) {
      if (m.tlUserId !== null && m.tlName !== null) {
        tlMap.set(m.tlUserId, m.tlName);
      }
    }
    const entries = Array.from(tlMap.entries());
    for (let i = 0; i < entries.length; i++) {
      for (let j = 0; j < entries.length - i - 1; j++) {
        if (entries[j][1].localeCompare(entries[j + 1][1]) > 0) {
          const temp = entries[j];
          entries[j] = entries[j + 1];
          entries[j + 1] = temp;
        }
      }
    }
    return entries;
  }, [members]);

  const filtered = useMemo(() => {
    const result: TeamMember[] = [];
    for (const m of members) {
      if (filterName && !m.name.includes(filterName)) continue;
      if (filterTlId !== '' && m.tlUserId !== filterTlId) continue;
      if (filterRole && m.role !== filterRole) continue;
      if (filterStatus) {
        let status = '';
        if (m.currentInventory != null) {
          status = m.currentInventory.status;
        }
        if (filterStatus === 'NONE') {
          if (m.currentInventory !== null) continue;
        } else if (status !== filterStatus) {
          continue;
        }
      }
      result.push(m);
    }
    return result;
  }, [members, filterName, filterTlId, filterRole, filterStatus]);

  const handleClear = () => {
    setFilterName('');
    setFilterTlId('');
    setFilterRole('');
    setFilterStatus('');
  };

  const handleDetail = (member: TeamMember) => {
    navigate(`/team/${member.id}`, {
      state: { from: '/admin/users-inquiry', fromLabel: t('allUserList.title') },
    });
  };

  const tlOptionElements: React.ReactNode[] = [];
  for (const [id, name] of tlOptions) {
    tlOptionElements.push(<option key={id} value={id}>{name}{t('allUserList.filter.teamSuffix')}</option>);
  }

  let content: React.ReactNode;
  if (loading) {
    content = <div className="loading">{t('loading')}</div>;
  } else {
    let tableSection: React.ReactNode;
    if (filtered.length === 0) {
      tableSection = <p className="no-data">{t('allUserList.noUsers')}</p>;
    } else {
      const rows: React.ReactNode[] = [];
      for (const member of filtered) {
        let roleLabelKey = member.role;
        if (ROLE_KEY[member.role] != null) {
          roleLabelKey = ROLE_KEY[member.role];
        }
        let tlNameLabel = '—';
        if (member.tlName != null) {
          tlNameLabel = member.tlName;
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
          statusCell = <span className="team-status team-status--none">{t('allUserList.noInventory')}</span>;
        }
        let fiscalYearName = '—';
        if (member.currentInventory != null) {
          fiscalYearName = member.currentInventory.fiscalYear.name;
        }
        rows.push(
          <tr key={member.id}>
            <td>{member.name}</td>
            <td>{t(roleLabelKey)}</td>
            <td>{tlNameLabel}</td>
            <td>{statusCell}</td>
            <td>{fiscalYearName}</td>
            <td>
              <button
                className="btn btn-sm btn-secondary"
                onClick={() => handleDetail(member)}
              >
                <IconArrowRight size={12} />
                {t('allUserList.table.detailButton')}
              </button>
            </td>
          </tr>,
        );
      }
      tableSection = (
        <StickyHorizontalScroll className="master-table-wrap">
          <table className="master-table">
            <thead>
              <tr>
                <th>{t('allUserList.table.name')}</th>
                <th>{t('allUserList.table.role')}</th>
                <th>{t('allUserList.table.team')}</th>
                <th>{t('allUserList.table.currentStatus')}</th>
                <th>{t('allUserList.table.fiscalYear')}</th>
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
    content = (
      <>
        <p className="all-user-count">{filtered.length}{t('allUserList.countSuffix')}</p>
        {tableSection}
      </>
    );
  }

  return (
    <div className="team-page">
      <NavBar />
      <main className="team-main">
        <button className="page-back-btn" onClick={() => navigate('/')}>{t('allUserList.backButton')}</button>
        <h1 className="page-title">{t('allUserList.title')}</h1>

        {/* ─── Search / Filter ─── */}
        <div className="all-user-filters">
          <input
            className="input all-user-filter-input"
            placeholder={t('allUserList.filter.namePlaceholder')}
            value={filterName}
            onChange={e => setFilterName(e.target.value)}
          />
          <select
            className="select all-user-filter-select"
            value={filterTlId}
            onChange={e => {
              if (e.target.value === '') {
                setFilterTlId('');
              } else {
                setFilterTlId(Number(e.target.value));
              }
            }}
          >
            <option value="">{t('allUserList.filter.teamAll')}</option>
            <option value={-1}>{t('allUserList.filter.teamNone')}</option>
            {tlOptionElements}
          </select>
          <select
            className="select all-user-filter-select"
            value={filterRole}
            onChange={e => setFilterRole(e.target.value)}
          >
            <option value="">{t('allUserList.filter.roleAll')}</option>
            <option value="GENERAL">{t('role.general')}</option>
            <option value="TL">{t('role.tl')}</option>
            <option value="ADMIN">{t('role.admin')}</option>
          </select>
          <select
            className="select all-user-filter-select"
            value={filterStatus}
            onChange={e => setFilterStatus(e.target.value)}
          >
            <option value="">{t('allUserList.filter.statusAll')}</option>
            <option value="COMPLETED">{t('status.completed')}</option>
            <option value="PENDING_GOAL">{t('status.pendingGoal')}</option>
            <option value="DRAFT">{t('status.draft')}</option>
            <option value="NONE">{t('allUserList.filter.statusNone')}</option>
          </select>
          <button className="btn btn-secondary" onClick={handleClear}><IconX size={13} />{t('allUserList.filter.clearButton')}</button>
        </div>

        {content}
      </main>
    </div>
  );
}
