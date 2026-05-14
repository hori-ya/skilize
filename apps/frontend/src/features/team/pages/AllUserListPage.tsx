import { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { getTeamMembers } from '../api/userApi';
import type { TeamMember } from '../types/index';
import NavBar from '../../../app/layouts/NavBar';
import { IconX, IconArrowRight } from '../../../shared/ui/Icons';

const STATUS_LABEL: Record<string, string> = {
  COMPLETED: '完了',
  PENDING_GOAL: '提出済み・目標未設定',
  DRAFT: '入力中',
};

const STATUS_ICON: Record<string, string> = {
  COMPLETED: '●',
  PENDING_GOAL: '○',
  DRAFT: '△',
};

const ROLE_LABEL: Record<string, string> = {
  GENERAL: '一般',
  TL: 'TL',
  ADMIN: '管理者',
};

export default function AllUserListPage() {
  const navigate = useNavigate();

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
      state: { from: '/admin/users-inquiry', fromLabel: '全ユーザー照会' },
    });
  };

  return (
    <div className="team-page">
      <NavBar />
      <main className="team-main">
        <button className="page-back-btn" onClick={() => navigate('/')}>← ダッシュボードに戻る</button>
        <h1 className="page-title">全ユーザー照会</h1>

        {/* ─── Search / Filter ─── */}
        <div className="all-user-filters">
          <input
            className="input all-user-filter-input"
            placeholder="名前で検索"
            value={filterName}
            onChange={e => setFilterName(e.target.value)}
          />
          <select
            className="select all-user-filter-select"
            value={filterTlId}
            onChange={e => setFilterTlId(e.target.value === '' ? '' : Number(e.target.value))}
          >
            <option value="">チーム（全て）</option>
            <option value={-1}>チームなし</option>
            {tlOptions.map(([id, name]) => (
              <option key={id} value={id}>{name} チーム</option>
            ))}
          </select>
          <select
            className="select all-user-filter-select"
            value={filterRole}
            onChange={e => setFilterRole(e.target.value)}
          >
            <option value="">ロール（全て）</option>
            <option value="GENERAL">一般</option>
            <option value="TL">TL</option>
            <option value="ADMIN">管理者</option>
          </select>
          <select
            className="select all-user-filter-select"
            value={filterStatus}
            onChange={e => setFilterStatus(e.target.value)}
          >
            <option value="">ステータス（全て）</option>
            <option value="COMPLETED">完了</option>
            <option value="PENDING_GOAL">提出済み・目標未設定</option>
            <option value="DRAFT">入力中</option>
            <option value="NONE">未作成</option>
          </select>
          <button className="btn btn-secondary" onClick={handleClear}><IconX size={13} />クリア</button>
        </div>

        {loading ? (
          <div className="loading">読み込み中...</div>
        ) : (
          <>
            <p className="all-user-count">{filtered.length} 件</p>
            {filtered.length === 0 ? (
              <p className="no-data">該当するユーザーがいません</p>
            ) : (
              <div className="master-table-wrap">
                <table className="master-table">
                  <thead>
                    <tr>
                      <th>名前</th>
                      <th>ロール</th>
                      <th>チーム（TL）</th>
                      <th>当年度ステータス</th>
                      <th>年度</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    {filtered.map(member => (
                      <tr key={member.id}>
                        <td>{member.name}</td>
                        <td>{ROLE_LABEL[member.role] ?? member.role}</td>
                        <td>{member.tlName ?? '—'}</td>
                        <td>
                          {member.currentInventory ? (
                            <span className={`team-status team-status--${member.currentInventory.status.toLowerCase()}`}>
                              {STATUS_ICON[member.currentInventory.status]}{' '}
                              {STATUS_LABEL[member.currentInventory.status]}
                            </span>
                          ) : (
                            <span className="team-status team-status--none">— 未作成</span>
                          )}
                        </td>
                        <td>{member.currentInventory?.fiscalYear.name ?? '—'}</td>
                        <td>
                          <button
                            className="btn btn-sm btn-secondary"
                            onClick={() => handleDetail(member)}
                          >
                            <IconArrowRight size={12} />
                            詳細を見る
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </>
        )}
      </main>
    </div>
  );
}
