import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getComparison, patchItSkillRemarks } from '../api/inventory';
import type { ComparisonResponse, ComparisonItem } from '../types/inventory';
import NavBar from '../components/NavBar';

export default function ComparisonPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const inventoryId = Number(id);

  const [comparison, setComparison] = useState<ComparisonResponse | null>(null);
  const [editingRemarks, setEditingRemarks] = useState<Record<number, string>>({});
  const [savingId, setSavingId] = useState<number | null>(null);

  useEffect(() => {
    getComparison(inventoryId).then(res => {
      setComparison(res.data);
      const initial: Record<number, string> = {};
      res.data.items.forEach(item => {
        initial[item.currentDetailId] = item.currentRemarks ?? '';
      });
      setEditingRemarks(initial);
    });
  }, [inventoryId]);

  const handleSaveRemarks = async (item: ComparisonItem) => {
    setSavingId(item.currentDetailId);
    try {
      await patchItSkillRemarks(inventoryId, item.currentDetailId, editingRemarks[item.currentDetailId] ?? '');
    } finally {
      setSavingId(null);
    }
  };

  const handleNext = () => {
    navigate(`/inventory/${inventoryId}/goal-review`);
  };

  if (!comparison) return <div className="loading">読み込み中...</div>;

  const changedItems = comparison.items.filter(i => i.diff !== null && i.diff !== 0);
  const changedWithNoRemarks = changedItems.filter(i => !editingRemarks[i.currentDetailId]);

  return (
    <div className="comparison-page">
      <NavBar />

      <main className="comparison-main">
        <button className="page-back-btn" onClick={() => navigate(`/inventory/${inventoryId}`)}>← 棚卸入力に戻る</button>
        <h1 className="page-title">前年度比較 — {comparison.currentFiscalYear}</h1>

        {!comparison.hasPrevYear ? (
          <div className="info-card">
            <p>前年度の棚卸データがありません（初回）。</p>
            <button className="btn btn-primary" onClick={handleNext}>目標振り返りへ →</button>
          </div>
        ) : (
          <>
            <p className="comparison-subtitle">
              前年度（{comparison.prevFiscalYear}）との比較
            </p>

            {changedWithNoRemarks.length > 0 && (
              <div className="alert alert-info">
                採点が変化したスキルのうち、備考が未入力のものがあります。
                可能であれば採点根拠を記入してください（任意）。
              </div>
            )}

            <div className="comparison-table-wrapper">
              <table className="comparison-table">
                <thead>
                  <tr>
                    <th>スキル名</th>
                    <th>前年度</th>
                    <th>今年度</th>
                    <th>差分</th>
                    <th>備考</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {comparison.items.map(item => (
                    <tr key={item.currentDetailId} className={item.diff !== 0 && item.diff !== null ? 'changed-row' : ''}>
                      <td>{item.skillName}</td>
                      <td>{item.prevLevelValue ?? '—'}</td>
                      <td>{item.currentLevelValue}</td>
                      <td className={`diff-cell${item.diff != null && item.diff > 0 ? ' diff-up' : item.diff != null && item.diff < 0 ? ' diff-down' : ''}`}>
                        {item.diff != null ? (item.diff > 0 ? `+${item.diff}` : item.diff) : '—'}
                      </td>
                      <td>
                        <textarea
                          className="textarea remarks-input"
                          value={editingRemarks[item.currentDetailId] ?? ''}
                          placeholder={item.diff !== 0 && item.diff !== null ? '変化の理由を記入（任意）' : '任意'}
                          onChange={e => setEditingRemarks(prev => ({
                            ...prev, [item.currentDetailId]: e.target.value,
                          }))}
                        />
                      </td>
                      <td>
                        <button
                          className="btn btn-sm"
                          onClick={() => handleSaveRemarks(item)}
                          disabled={savingId === item.currentDetailId}
                        >
                          {savingId === item.currentDetailId ? '...' : '保存'}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="action-row">
              <button className="btn btn-primary" onClick={handleNext}>
                目標振り返りへ →
              </button>
            </div>
          </>
        )}
      </main>
    </div>
  );
}
