import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getTeamMembers } from '../api/user';
import type { TeamMember } from '../types/user';
import NavBar from '../components/NavBar';

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

export default function TeamMemberListPage() {
  const navigate = useNavigate();
  const [members, setMembers] = useState<TeamMember[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getTeamMembers()
      .then(res => setMembers(res.data))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="team-page">
      <NavBar />
      <main className="team-main">
        <button className="page-back-btn" onClick={() => navigate('/')}>← ダッシュボードに戻る</button>
        <h1 className="page-title">チームメンバー照会</h1>

        {loading ? (
          <div className="loading">読み込み中...</div>
        ) : members.length === 0 ? (
          <div className="info-card"><p>チームメンバーがいません。</p></div>
        ) : (
          <div className="master-table-wrap">
            <table className="master-table">
              <thead>
                <tr>
                  <th>名前</th>
                  <th>ロール</th>
                  <th>当年度ステータス</th>
                  <th>年度</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {members.map(member => (
                  <tr key={member.id}>
                    <td>{member.name}</td>
                    <td>{ROLE_LABEL[member.role] ?? member.role}</td>
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
                        onClick={() => navigate(`/team/${member.id}`)}
                      >
                        詳細を見る
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>
    </div>
  );
}
