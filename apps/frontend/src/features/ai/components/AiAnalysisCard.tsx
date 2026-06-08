/*******************************************************************************
 * 機能ID      ：AI
 * 機能名      ：AI機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * AIキャリア分析カード。分析の状態（PENDING / PROCESSING / FAILED / 完了）に
 * 応じた表示を行う。完了時は要約・強み・成長領域・推奨アクションを表示する。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import type { AiAnalysis } from '../types/index';
import { useTranslation } from 'react-i18next';

interface Props {
  analysis: AiAnalysis;
  fiscalYearName?: string;
}

/**
 * AIキャリア分析カード。
 *
 * 分析状態に応じた表示を行う。PENDING/PROCESSING はスピナー付きで待機中を示し、
 * FAILED はエラーメッセージを、完了時は分析結果の詳細を表示する。
 */
export default function AiAnalysisCard({ analysis, fiscalYearName }: Props) {
  const { t } = useTranslation('inventory');
  const { status, analysisResult, errorMessage, createdAt, updatedAt } = analysis;

  const fmtDate = (iso: string) => new Date(iso).toLocaleString('ja-JP');

  if (status === 'PENDING') {
    return (
      <div className="ai-analysis-card ai-analysis-card--pending">
        <div className="ai-status-row">
          <span className="ai-spinner" />
          <span className="ai-status-badge ai-status-badge--pending">{t('aiAnalysisCard.status.pending')}</span>
        </div>
        <p className="ai-analysis-pending-text">
          {t('aiAnalysisCard.pendingText')}
        </p>
        <p className="ai-meta">{t('aiAnalysisCard.requestedAt')}{fmtDate(createdAt)}</p>
      </div>
    );
  }

  if (status === 'PROCESSING') {
    return (
      <div className="ai-analysis-card ai-analysis-card--processing">
        <div className="ai-status-row">
          <span className="ai-spinner ai-spinner--amber" />
          <span className="ai-status-badge ai-status-badge--processing">{t('aiAnalysisCard.status.processing')}</span>
        </div>
        <p className="ai-analysis-pending-text">
          {t('aiAnalysisCard.processingText')}
        </p>
        <p className="ai-meta">{t('aiAnalysisCard.requestedAt')}{fmtDate(createdAt)}</p>
      </div>
    );
  }

  if (status === 'FAILED' || !analysisResult) {
    return (
      <div className="ai-analysis-card ai-analysis-card--failed">
        <div className="ai-status-row">
          <span className="ai-status-badge ai-status-badge--failed">{t('aiAnalysisCard.status.failed')}</span>
        </div>
        <p className="ai-analysis-failed-text">{t('aiAnalysisCard.failedText')}</p>
        {errorMessage && <p className="ai-analysis-error-detail">{errorMessage}</p>}
        <p className="ai-meta">{t('aiAnalysisCard.updatedAt')}{fmtDate(updatedAt)}</p>
      </div>
    );
  }

  return (
    <div className="ai-analysis-card">
      {fiscalYearName && (
        <div className="ai-status-row">
          <h3 className="ai-analysis-card__year" style={{ margin: 0 }}>{fiscalYearName}</h3>
        </div>
      )}
      <div className="ai-analysis-updated">
        {t('aiAnalysisCard.analyzedAt')}{fmtDate(updatedAt)}
      </div>

      <div className="ai-analysis-section-block">
        <h4 className="ai-analysis-section-title">{t('aiAnalysisCard.section.summary')}</h4>
        <p className="ai-analysis-text">{analysisResult.summary}</p>
      </div>

      <div className="ai-analysis-section-block">
        <h4 className="ai-analysis-section-title">{t('aiAnalysisCard.section.strengths')}</h4>
        <ul className="ai-analysis-list">
          {analysisResult.strengths.map((s, i) => <li key={i}>{s}</li>)}
        </ul>
      </div>

      <div className="ai-analysis-section-block">
        <h4 className="ai-analysis-section-title">{t('aiAnalysisCard.section.growthAreas')}</h4>
        <ul className="ai-analysis-list">
          {analysisResult.growth_areas.map((g, i) => <li key={i}>{g}</li>)}
        </ul>
      </div>

      <div className="ai-analysis-section-block">
        <h4 className="ai-analysis-section-title">{t('aiAnalysisCard.section.expectationFit')}</h4>
        <p className="ai-analysis-text">{analysisResult.expectation_fit}</p>
      </div>

      <div className="ai-analysis-section-block">
        <h4 className="ai-analysis-section-title">{t('aiAnalysisCard.section.recommendedActions')}</h4>
        <ul className="ai-analysis-list">
          {analysisResult.recommended_actions.map((a, i) => <li key={i}>{a}</li>)}
        </ul>
      </div>
    </div>
  );
}
