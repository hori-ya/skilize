import type { AiAnalysis } from '../types/index';

interface Props {
  analysis: AiAnalysis;
  fiscalYearName?: string;
}

export default function AiAnalysisCard({ analysis, fiscalYearName }: Props) {
  const { status, analysisResult, errorMessage, updatedAt } = analysis;

  if (status === 'PENDING' || status === 'PROCESSING') {
    return (
      <div className="ai-analysis-card ai-analysis-card--pending">
        <p className="ai-analysis-pending-text">AIキャリア分析を準備中です...</p>
      </div>
    );
  }

  if (status === 'FAILED' || !analysisResult) {
    return (
      <div className="ai-analysis-card ai-analysis-card--failed">
        <p className="ai-analysis-failed-text">分析データがありません。</p>
        {errorMessage && <p className="ai-analysis-error-detail">{errorMessage}</p>}
      </div>
    );
  }

  return (
    <div className="ai-analysis-card">
      {fiscalYearName && <h3 className="ai-analysis-card__year">{fiscalYearName}</h3>}
      <div className="ai-analysis-updated">
        分析日時: {new Date(updatedAt).toLocaleString('ja-JP')}
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
