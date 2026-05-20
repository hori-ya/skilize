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
import { useTranslation } from 'react-i18next';

interface ChartsState {
  radar: RadarResponse | null;
  growth: GrowthResponse | null;
  heatmap: HeatmapResponse | null;
  timeline: TimelineResponse | null;
}

export default function DashboardPage() {
  const navigate = useNavigate();
  const { t } = useTranslation('inventory');
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

  // PENDING / PROCESSING 中は 10 秒ごとにポーリングして完了を検知する
  const latestAnalysisStatus = aiAnalyses[0]?.status;
  useEffect(() => {
    if (latestAnalysisStatus !== 'PENDING' && latestAnalysisStatus !== 'PROCESSING') return;
    const timer = setInterval(() => {
      getMyAiAnalyses().then(res => setAiAnalyses(res.data)).catch(() => {});
    }, 10000);
    return () => clearInterval(timer);
  }, [latestAnalysisStatus]);

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

  const deadlineBanner = (() => {
    const fy = dashboard?.currentFiscalYear;
    const invStatus = dashboard?.currentInventory?.status;
    // 提出済み（PENDING_GOAL / COMPLETED）はアラート不要
    if (!fy || invStatus === 'PENDING_GOAL' || invStatus === 'COMPLETED') return null;

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (fy.inputStartDate) {
      const start = new Date(fy.inputStartDate);
      if (today < start) {
        const daysUntil = Math.ceil((start.getTime() - today.getTime()) / 86400000);
        return { level: 'warning' as const, message: t('dashboard.deadline.notStarted', { date: fy.inputStartDate, days: daysUntil }) };
      }
    }

    if (!fy.inputEndDate) return null;

    const end = new Date(fy.inputEndDate);
    const daysLeft = Math.ceil((end.getTime() - today.getTime()) / 86400000);

    if (daysLeft < 0) return { level: 'error' as const, message: t('dashboard.deadline.expired', { date: fy.inputEndDate }) };
    if (daysLeft === 0) return { level: 'urgent' as const, message: t('dashboard.deadline.today', { date: fy.inputEndDate }) };
    if (daysLeft <= 3)  return { level: 'urgent' as const, message: t('dashboard.deadline.daysLeft', { days: daysLeft, date: fy.inputEndDate }) };
    if (daysLeft <= 7)  return { level: 'warning' as const, message: t('dashboard.deadline.daysLeft', { days: daysLeft, date: fy.inputEndDate }) };
    return null;
  })();

  const statusLabel = (status: string) => {
    switch (status) {
      case 'DRAFT': return t('dashboard.inventoryStatus.draft');
      case 'PENDING_GOAL': return t('dashboard.inventoryStatus.pendingGoal');
      case 'COMPLETED': return t('dashboard.inventoryStatus.completed');
      default: return status;
    }
  };

  if (isLoading) {
    return (
      <div className="dashboard-page">
        <div className="loading">{t('loading')}</div>
      </div>
    );
  }

  const inv = dashboard?.currentInventory;

  return (
    <div className="dashboard-page">
      <NavBar />
      <main className="dashboard-main">
        <h1 className="dashboard-title">{t('dashboard.title')}</h1>

        {deadlineBanner && (
          <div className={`deadline-banner deadline-banner--${deadlineBanner.level}`}>
            <span>{deadlineBanner.level === 'warning' ? '⚠️' : '🚨'}</span>
            <span>{deadlineBanner.message}</span>
          </div>
        )}

        {dashboard?.currentFiscalYear && (
          <div className="dashboard-card">
            <h2 className="card-title">{t('dashboard.currentFiscalYear', { name: dashboard.currentFiscalYear.name })}</h2>

            {inv ? (
              <div className="inventory-status">
                <div className="status-badge" data-status={inv.status}>
                  {statusLabel(inv.status)}
                </div>
                <div className="inventory-counts">
                  <span>{t('dashboard.inventoryCount.itSkill', { count: inv.itSkillCount })}</span>
                  <span>{t('dashboard.inventoryCount.qualification', { count: inv.qualificationCount })}</span>
                  <span>{t('dashboard.inventoryCount.seminar', { count: inv.seminarCount })}</span>
                </div>
                {inv.submittedAt && (
                  <p className="submitted-at">
                    {t('dashboard.submittedAt')}{new Date(inv.submittedAt).toLocaleString('ja-JP')}
                  </p>
                )}
                {inv.goalCompletedAt && (
                  <p className="submitted-at">
                    {t('dashboard.goalCompletedAt')}{new Date(inv.goalCompletedAt).toLocaleString('ja-JP')}
                  </p>
                )}
                <div className="action-buttons">
                  <button className="btn btn-primary" onClick={handleContinueInventory}>
                    {inv.status === 'COMPLETED' ? <IconEye size={15} /> : <IconEdit size={15} />}
                    {inv.status === 'COMPLETED' ? t('dashboard.action.viewInventory') : t('dashboard.action.continueInventory')}
                  </button>
                </div>
              </div>
            ) : (
              <div className="no-inventory">
                <p>{t('dashboard.noInventory')}</p>
                <button
                  className="btn btn-primary"
                  onClick={handleStartInventory}
                  disabled={isCreating}
                >
                  <IconPlay size={15} />
                  {isCreating ? t('dashboard.action.startingInventory') : t('dashboard.action.startInventory')}
                </button>
              </div>
            )}
          </div>
        )}

        {!dashboard?.currentFiscalYear && (
          <div className="dashboard-card">
            <p>{t('dashboard.noFiscalYear')}</p>
          </div>
        )}

        <div className="dashboard-nav">
          <button className="btn btn-secondary" onClick={() => navigate('/inventory/history')}>
            <IconHistory size={15} />
            {t('dashboard.viewHistory')}
          </button>
        </div>

        {/* Charts section */}
        <section className="chart-section">
          <h2 className="chart-section__title">{t('dashboard.chartsSection')}</h2>

          {chartsLoading ? (
            <div className="chart-loading">{t('dashboard.chartsLoading')}</div>
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
        {dashboard?.currentFiscalYear && (
          <section className="ai-analysis-section">
            <h2 className="chart-section__title">{t('dashboard.aiAnalysisSection')}</h2>
            {aiAnalyses.length > 0 ? (
              <AiAnalysisCard
                analysis={aiAnalyses[0]}
                fiscalYearName={
                  aiAnalyses[0].fiscalYearId === dashboard.currentFiscalYear.id
                    ? dashboard.currentFiscalYear.name
                    : undefined
                }
              />
            ) : (
              <div className="ai-analysis-card ai-analysis-card--none">
                <p className="ai-no-analysis-text">
                  {t('dashboard.aiAnalysisNone')}
                </p>
              </div>
            )}
          </section>
        )}
      </main>
    </div>
  );
}
