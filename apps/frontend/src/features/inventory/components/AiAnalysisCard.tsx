import type { AiAnalysis } from '../types/index';

interface Props {
  analysis: AiAnalysis;
  fiscalYearName?: string;
}

const STATUS_LABEL: Record<string, string> = {
  PENDING:    '待機中',
  PROCESSING: '分析中',
  COMPLETED:  '完了',
  FAILED:     '失敗',
};

export default function AiAnalysisCard({ analysis, fiscalYearName }: Props) {
  const { status, analysisResult, errorMessage, createdAt, updatedAt } = analysis;

  const fmtDate = (iso: string) => new Date(iso).toLocaleString('ja-JP');

  if (status === 'PENDING') {
    return (
      <div className="ai-analysis-card ai-analysis-card--pending">
        <div className="ai-status-row">
          <span className="ai-spinner" />
          <span className="ai-status-badge ai-status-badge--pending">{STATUS_LABEL.PENDING}</span>
        </div>
        <p className="ai-analysis-pending-text">
          AI分析のリクエストを受け付けました。しばらくお待ちください。
        </p>
        <p className="ai-meta">リクエスト日時：{fmtDate(createdAt)}</p>
      </div>
    );
  }

  if (status === 'PROCESSING') {
    return (
      <div className="ai-analysis-card ai-analysis-card--processing">
        <div className="ai-status-row">
          <span className="ai-spinner ai-spinner--amber" />
          <span className="ai-status-badge ai-status-badge--processing">{STATUS_LABEL.PROCESSING}</span>
        </div>
        <p className="ai-analysis-pending-text">
          AIがキャリアデータを分析中です。完了すると自動的に表示されます。
        </p>
        <p className="ai-meta">リクエスト日時：{fmtDate(createdAt)}</p>
      </div>
    );
  }

  if (status === 'FAILED' || !analysisResult) {
    return (
      <div className="ai-analysis-card ai-analysis-card--failed">
        <div className="ai-status-row">
          <span className="ai-status-badge ai-status-badge--failed">{STATUS_LABEL.FAILED}</span>
        </div>
        <p className="ai-analysis-failed-text">AI分析に失敗しました。</p>
        {errorMessage && <p className="ai-analysis-error-detail">{errorMessage}</p>}
        <p className="ai-meta">更新日時：{fmtDate(updatedAt)}</p>
      </div>
    );
  }

  return (
    <div className="ai-analysis-card">
      <div className="ai-status-row">
        {fiscalYearName && <h3 className="ai-analysis-card__year" style={{ margin: 0 }}>{fiscalYearName}</h3>}
        <span className="ai-status-badge ai-status-badge--completed">{STATUS_LABEL.COMPLETED}</span>
      </div>
      <div className="ai-analysis-updated">
        分析日時：{fmtDate(updatedAt)}
      </div>

      <div className="ai-analysis-section-block">
        <h4 className="ai-analysis-section-title">総括</h4>
        <p className="ai-analysis-text">{analysisResult.summary}</p>
      </div>

      <div className="ai-analysis-section-block">
        <h4 className="ai-analysis-section-title">強み・得意領域</h4>
        <ul className="ai-analysis-list">
          {analysisResult.strengths.map((s, i) => <li key={i}>{s}</li>)}
        </ul>
      </div>

      <div className="ai-analysis-section-block">
        <h4 className="ai-analysis-section-title">成長が期待される領域</h4>
        <ul className="ai-analysis-list">
          {analysisResult.growth_areas.map((g, i) => <li key={i}>{g}</li>)}
        </ul>
      </div>

      <div className="ai-analysis-section-block">
        <h4 className="ai-analysis-section-title">目標・期待とのフィット感</h4>
        <p className="ai-analysis-text">{analysisResult.expectation_fit}</p>
      </div>

      <div className="ai-analysis-section-block">
        <h4 className="ai-analysis-section-title">ネクストステップの提案</h4>
        <ul className="ai-analysis-list">
          {analysisResult.recommended_actions.map((a, i) => <li key={i}>{a}</li>)}
        </ul>
      </div>
    </div>
  );
}
