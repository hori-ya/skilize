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

  useEffect(() => {
    getTeamMembers()
      .then(res => setMembers(res.data))
      .finally(() => setLoading(false));
  }, []);

  // Build TL list for dropdown
  const tlOptions = useMemo(() => {
    const tlMap = new Map<number, string>();
    members.forEach(m => {
      if (m.tlUserId !== null && m.tlName !== null) {
        tlMap.set(m.tlUserId, m.tlName);
      }
    });
    return Array.from(tlMap.entries()).sort((a, b) => a[1].localeCompare(b[1]));
  }, [members]);

  const filtered = useMemo(() => {
    return members.filter(m => {
      if (filterName && !m.name.includes(filterName)) return false;
      if (filterTlId !== '' && m.tlUserId !== filterTlId) return false;
      if (filterRole && m.role !== filterRole) return false;
      if (filterStatus) {
        const status = m.currentInventory?.status ?? '';
        if (filterStatus === 'NONE') {
          if (m.currentInventory !== null) return false;
        } else if (status !== filterStatus) {
          return false;
        }
      }
      return true;
    });
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
            onChange={e => setFilterTlId(e.target.value === '' ? '' : Number(e.target.value))}
          >
            <option value="">{t('allUserList.filter.teamAll')}</option>
            <option value={-1}>{t('allUserList.filter.teamNone')}</option>
            {tlOptions.map(([id, name]) => (
              <option key={id} value={id}>{name}{t('allUserList.filter.teamSuffix')}</option>
            ))}
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

        {loading ? (
          <div className="loading">{t('loading')}</div>
        ) : (
          <>
            <p className="all-user-count">{filtered.length}{t('allUserList.countSuffix')}</p>
            {filtered.length === 0 ? (
              <p className="no-data">{t('allUserList.noUsers')}</p>
            ) : (
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
                    {filtered.map(member => (
                      <tr key={member.id}>
                        <td>{member.name}</td>
                        <td>{t(ROLE_KEY[member.role] ?? member.role)}</td>
                        <td>{member.tlName ?? '—'}</td>
                        <td>
                          {member.currentInventory ? (
                            <span className={`team-status team-status--${member.currentInventory.status.toLowerCase()}`}>
                              {STATUS_ICON[member.currentInventory.status]}{' '}
                              {t(STATUS_KEY[member.currentInventory.status] ?? member.currentInventory.status)}
                            </span>
                          ) : (
                            <span className="team-status team-status--none">{t('allUserList.noInventory')}</span>
                          )}
                        </td>
                        <td>{member.currentInventory?.fiscalYear.name ?? '—'}</td>
                        <td>
                          <button
                            className="btn btn-sm btn-secondary"
                            onClick={() => handleDetail(member)}
                          >
                            <IconArrowRight size={12} />
                            {t('allUserList.table.detailButton')}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </StickyHorizontalScroll>
            )}
          </>
        )}
      </main>
    </div>
  );
}
