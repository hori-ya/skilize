import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getDashboard, createInventory, getMyAiAnalyses } from '../api/inventoryApi';
import { getRadarChart, getGrowthChart, getHeatmapChart, getTimelineChart } from '../api/chartApi';
import type { DashboardResponse, AiAnalysis } from '../types/index';
import type { RadarResponse, GrowthResponse, HeatmapResponse, TimelineResponse } from '../types/charts';
import NavBar from '../../../app/layouts/NavBar';
import { IconPlay, IconEdit, IconEye, IconHistory } from '../../../shared/ui/Icons';
import RadarChartCard from '../components/RadarChartCard';
import GrowthChartCard from '../components/GrowthChartCard';
import HeatmapChartCard from '../components/HeatmapChartCard';
import TimelineChartCard from '../components/TimelineChartCard';
import AiAnalysisCard from '../components/AiAnalysisCard';

interface ChartsState {
  radar: RadarResponse | null;
  growth: GrowthResponse | null;
  heatmap: HeatmapResponse | null;
  timeline: TimelineResponse | null;
}

export default function DashboardPage() {
  const navigate = useNavigate();
  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null);
  const [charts, setCharts] = useState<ChartsState>({ radar: null, growth: null, heatmap: null, timeline: null });
  const [isLoading, setIsLoading] = useState(true);
  const [chartsLoading, setChartsLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);
  const [aiAnalyses, setAiAnalyses] = useState<AiAnalysis[]>([]);

  useEffect(() => {
    getDashboard()
      .then(res => setDashboard(res.data))
      .finally(() => setIsLoading(false));

    Promise.all([getRadarChart(), getGrowthChart(), getHeatmapChart(), getTimelineChart()])
      .then(([radarRes, growthRes, heatmapRes, timelineRes]) => {
        setCharts({
          radar: radarRes.data,
          growth: growthRes.data,
          heatmap: heatmapRes.data,
          timeline: timelineRes.data,
        });
      })
      .finally(() => setChartsLoading(false));

    getMyAiAnalyses().then(res => setAiAnalyses(res.data)).catch(() => {});
  }, []);

  const handleStartInventory = async () => {
    if (!dashboard?.currentFiscalYear) return;
    setIsCreating(true);
    try {
      const res = await createInventory(dashboard.currentFiscalYear.id);
      navigate(`/inventory/${res.data.id}`);
    } catch {
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
                    {inv.status === 'COMPLETED' ? <IconEye size={15} /> : <IconEdit size={15} />}
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
                  <IconPlay size={15} />
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
            <IconHistory size={15} />
            過去の棚卸を確認する
          </button>
        </div>

        {/* Charts section */}
        <section className="chart-section">
          <h2 className="chart-section__title">スキル可視化</h2>

          {chartsLoading ? (
            <div className="chart-loading">グラフを読み込み中...</div>
          ) : (
            <>
              <div className="chart-grid">
                {charts.radar && <RadarChartCard data={charts.radar} />}
                {charts.growth && <GrowthChartCard data={charts.growth} />}
              </div>
              {charts.heatmap && <HeatmapChartCard data={charts.heatmap} />}
              {charts.timeline && <TimelineChartCard events={charts.timeline.events} />}
            </>
          )}
        </section>

        {/* AI Analysis section */}
        {aiAnalyses.length > 0 && (
          <section className="ai-analysis-section">
            <h2 className="chart-section__title">AIキャリア分析</h2>
            <AiAnalysisCard analysis={aiAnalyses[0]} />
          </section>
        )}
      </main>
    </div>
  );
}
