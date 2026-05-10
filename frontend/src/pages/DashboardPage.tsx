import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { getDashboard, createInventory } from '../api/inventory';
import type { DashboardResponse } from '../types/inventory';
import NavBar from '../components/NavBar';

export default function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);

  useEffect(() => {
    getDashboard()
      .then((res) => setDashboard(res.data))
      .finally(() => setIsLoading(false));
  }, []);

  const handleStartInventory = async () => {
    if (!dashboard?.currentFiscalYear) return;
    setIsCreating(true);
    try {
      const res = await createInventory(dashboard.currentFiscalYear.id);
      navigate(`/inventory/${res.data.id}`);
    } catch {
      // 既に存在する場合はダッシュボードを再取得
      const res = await getDashboard();
      setDashboard(res.data);
    } finally {
      setIsCreating(false);
    }
  };

  const handleContinueInventory = () => {
    const inv = dashboard?.currentInventory;
    if (!inv) return;
    if (!inv.submittedAt) {
      navigate(`/inventory/${inv.id}`);
    } else if (inv.goalCompletedAt) {
      navigate(`/inventory/${inv.id}`);
    } else if (inv.goalReviewCompletedAt) {
      navigate(`/inventory/${inv.id}/goals`);
    } else {
      navigate(`/inventory/${inv.id}/comparison`);
    }
  };

  const statusLabel = (status: string) => {
    switch (status) {
      case 'DRAFT': return '入力中（下書き）';
      case 'PENDING_GOAL': return '提出済み・目標設定待ち';
      case 'COMPLETED': return '完了';
      default: return status;
    }
  };

  if (isLoading) {
    return (
      <div className="dashboard-page">
        <div className="loading">読み込み中...</div>
      </div>
    );
  }

  const inv = dashboard?.currentInventory;

  return (
    <div className="dashboard-page">
      <NavBar />
      <main className="dashboard-main">
        <h1 className="dashboard-title">ダッシュボード</h1>

        {dashboard?.currentFiscalYear && (
          <div className="dashboard-card">
            <h2 className="card-title">当年度：{dashboard.currentFiscalYear.name}</h2>

            {inv ? (
              <div className="inventory-status">
                <div className="status-badge" data-status={inv.status}>
                  {statusLabel(inv.status)}
                </div>
                <div className="inventory-counts">
                  <span>ITスキル：{inv.itSkillCount}件</span>
                  <span>資格：{inv.qualificationCount}件</span>
                  <span>セミナー：{inv.seminarCount}件</span>
                </div>
                {inv.submittedAt && (
                  <p className="submitted-at">
                    提出日時：{new Date(inv.submittedAt).toLocaleString('ja-JP')}
                  </p>
                )}
                {inv.goalCompletedAt && (
                  <p className="submitted-at">
                    目標設定完了：{new Date(inv.goalCompletedAt).toLocaleString('ja-JP')}
                  </p>
                )}
                <div className="action-buttons">
                  <button className="btn btn-primary" onClick={handleContinueInventory}>
                    棚卸を{inv.status === 'COMPLETED' ? '確認する' : '続ける'}
                  </button>
                </div>
              </div>
            ) : (
              <div className="no-inventory">
                <p>今年度の棚卸がまだ作成されていません。</p>
                <button
                  className="btn btn-primary"
                  onClick={handleStartInventory}
                  disabled={isCreating}
                >
                  {isCreating ? '作成中...' : '棚卸を開始する'}
                </button>
              </div>
            )}
          </div>
        )}

        {!dashboard?.currentFiscalYear && (
          <div className="dashboard-card">
            <p>有効な年度が設定されていません。管理者にお問い合わせください。</p>
          </div>
        )}

        <div className="dashboard-nav">
          <button className="btn btn-secondary" onClick={() => navigate('/inventory/history')}>
            過去の棚卸を確認する
          </button>
        </div>
      </main>
    </div>
  );
}
