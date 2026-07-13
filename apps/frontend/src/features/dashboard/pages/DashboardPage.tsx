/*******************************************************************************
 * 機能ID      ：DSH
 * 機能名      ：ダッシュボード
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * ダッシュボードページ。ログインユーザーの現在年度における棚卸状況を表示する。
 * グラフ（レーダー・成長推移・ヒートマップ・タイムライン）および
 * AI キャリア分析カードを併せて表示する。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getDashboard } from '../api/dashboardApi';
import { createInventory } from '../../inventory/api/inventoryApi';
import { getMyAiAnalyses } from '../../ai/api/aiAnalysisApi';
import { getRadarChart, getGrowthChart, getHeatmapChart, getTimelineChart } from '../../charts/api/chartApi';
import type { DashboardResponse } from '../types/index';
import type { AiAnalysis, AiAnalysisStatus } from '../../ai/types/index';
import type { RadarResponse, GrowthResponse, HeatmapResponse, TimelineResponse } from '../../charts/types/index';
import NavBar from '../../../app/layouts/NavBar';
import { IconPlay, IconEdit, IconEye, IconHistory } from '../../../shared/ui/Icons';
import RadarChartCard from '../../charts/components/RadarChartCard';
import GrowthChartCard from '../../charts/components/GrowthChartCard';
import HeatmapChartCard from '../../charts/components/HeatmapChartCard';
import TimelineChartCard from '../../charts/components/TimelineChartCard';
import AiAnalysisCard from '../../ai/components/AiAnalysisCard';
import { useTranslation } from 'react-i18next';

interface ChartsState {
  radar: RadarResponse | null;
  growth: GrowthResponse | null;
  heatmap: HeatmapResponse | null;
  timeline: TimelineResponse | null;
}

/**
 * ダッシュボードページ。
 *
 * ログインユーザーの現在年度の棚卸状況・チャート・AI 分析を表示する。
 * 棚卸が未作成の場合は開始ボタンを表示し、作成済みの場合は継続・閲覧ボタンを表示する。
 */
export default function DashboardPage() {
  const navigate = useNavigate();
  const { t } = useTranslation('inventory');
  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null);
  const [charts, setCharts] = useState<ChartsState>({ radar: null, growth: null, heatmap: null, timeline: null });
  const [isLoading, setIsLoading] = useState(true);
  const [chartsLoading, setChartsLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);
  const [aiAnalyses, setAiAnalyses] = useState<AiAnalysis[]>([]);

  // 初期表示時にダッシュボード情報・グラフデータ・AI分析一覧を取得する
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
  let latestAnalysisStatus: AiAnalysisStatus | undefined;
  if (aiAnalyses.length > 0) {
    latestAnalysisStatus = aiAnalyses[0].status;
  }
  useEffect(() => {
    if (latestAnalysisStatus !== 'PENDING' && latestAnalysisStatus !== 'PROCESSING') return;
    const timer = setInterval(() => {
      getMyAiAnalyses().then(res => setAiAnalyses(res.data)).catch(() => {});
    }, 10000);
    return () => clearInterval(timer);
  }, [latestAnalysisStatus]);

  const handleStartInventory = async () => {
    if (dashboard == null || dashboard.currentFiscalYear == null) return;
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
    if (dashboard == null || dashboard.currentInventory == null) return;
    const inv = dashboard.currentInventory;
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
    if (dashboard == null || dashboard.currentFiscalYear == null) return null;
    const fy = dashboard.currentFiscalYear;
    let invStatus: string | undefined;
    if (dashboard.currentInventory != null) {
      invStatus = dashboard.currentInventory.status;
    }
    // 提出済み（PENDING_GOAL / COMPLETED）はアラート不要
    if (invStatus === 'PENDING_GOAL' || invStatus === 'COMPLETED') return null;

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

  let deadlineIcon = '🚨';
  if (deadlineBanner && deadlineBanner.level === 'warning') {
    deadlineIcon = '⚠️';
  }

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

  let currentFiscalYear: DashboardResponse['currentFiscalYear'] = null;
  let inv: DashboardResponse['currentInventory'] = null;
  if (dashboard != null) {
    currentFiscalYear = dashboard.currentFiscalYear;
    inv = dashboard.currentInventory;
  }

  let inventoryStatusContent: React.ReactNode = null;
  if (inv != null) {
    let continueButtonIcon: React.ReactNode = <IconEdit size={15} />;
    let continueButtonLabel = t('dashboard.action.continueInventory');
    if (inv.status === 'COMPLETED') {
      continueButtonIcon = <IconEye size={15} />;
      continueButtonLabel = t('dashboard.action.viewInventory');
    }
    inventoryStatusContent = (
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
            {continueButtonIcon}
            {continueButtonLabel}
          </button>
        </div>
      </div>
    );
  } else {
    let startButtonLabel = t('dashboard.action.startInventory');
    if (isCreating) {
      startButtonLabel = t('dashboard.action.startingInventory');
    }
    inventoryStatusContent = (
      <div className="no-inventory">
        <p>{t('dashboard.noInventory')}</p>
        <button
          className="btn btn-primary"
          onClick={handleStartInventory}
          disabled={isCreating}
        >
          <IconPlay size={15} />
          {startButtonLabel}
        </button>
      </div>
    );
  }

  let chartSectionContent: React.ReactNode;
  if (chartsLoading) {
    chartSectionContent = <div className="chart-loading">{t('dashboard.chartsLoading')}</div>;
  } else {
    chartSectionContent = (
      <>
        <div className="chart-grid">
          {charts.radar && <RadarChartCard data={charts.radar} />}
          {charts.growth && <GrowthChartCard data={charts.growth} />}
        </div>
        {charts.heatmap && <HeatmapChartCard data={charts.heatmap} />}
        {charts.timeline && <TimelineChartCard events={charts.timeline.events} />}
      </>
    );
  }

  let aiAnalysisContent: React.ReactNode;
  if (aiAnalyses.length > 0) {
    let fiscalYearName: string | undefined;
    if (currentFiscalYear != null && aiAnalyses[0].fiscalYearId === currentFiscalYear.id) {
      fiscalYearName = currentFiscalYear.name;
    }
    aiAnalysisContent = (
      <AiAnalysisCard
        analysis={aiAnalyses[0]}
        fiscalYearName={fiscalYearName}
      />
    );
  } else {
    aiAnalysisContent = (
      <div className="ai-analysis-card ai-analysis-card--none">
        <p className="ai-no-analysis-text">
          {t('dashboard.aiAnalysisNone')}
        </p>
      </div>
    );
  }

  return (
    <div className="dashboard-page">
      <NavBar />
      <main className="dashboard-main">
        <h1 className="dashboard-title">{t('dashboard.title')}</h1>

        {deadlineBanner && (
          <div className={`deadline-banner deadline-banner--${deadlineBanner.level}`}>
            <span>{deadlineIcon}</span>
            <span>{deadlineBanner.message}</span>
          </div>
        )}

        {currentFiscalYear && (
          <div className="dashboard-card">
            <h2 className="card-title">{t('dashboard.currentFiscalYear', { name: currentFiscalYear.name })}</h2>
            {inventoryStatusContent}
          </div>
        )}

        {!currentFiscalYear && (
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
          {chartSectionContent}
        </section>

        {/* AI Analysis section */}
        {currentFiscalYear && (
          <section className="ai-analysis-section">
            <h2 className="chart-section__title">{t('dashboard.aiAnalysisSection')}</h2>
            {aiAnalysisContent}
          </section>
        )}
      </main>
    </div>
  );
}
