import { Fragment, useEffect, useState, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getComparison, patchItSkillRemarks } from '../api/inventoryApi';
import { getItSkills } from '../../../shared/api/masterApi';
import type { ComparisonResponse, ComparisonItem } from '../types/index';
import type { ItSkill } from '../../../shared/types/master';
import NavBar from '../../../app/layouts/NavBar';
import { IconArrowRight } from '../../../shared/ui/Icons';

export default function ComparisonPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const inventoryId = Number(id);

  const [comparison, setComparison] = useState<ComparisonResponse | null>(null);
  const [itSkillMaster, setItSkillMaster] = useState<ItSkill[]>([]);
  const [editingRemarks, setEditingRemarks] = useState<Record<number, string>>({});
  const [savingId, setSavingId] = useState<number | null>(null);

  useEffect(() => {
    Promise.all([
      getComparison(inventoryId),
      getItSkills(),
    ]).then(([compRes, masterRes]) => {
      setComparison(compRes.data);
      setItSkillMaster(masterRes.data);
      const initial: Record<number, string> = {};
      compRes.data.items.forEach(item => {
        initial[item.currentDetailId] = item.currentRemarks ?? '';
      });
      setEditingRemarks(initial);
    });
  }, [inventoryId]);

  const comparisonTree = useMemo(() => {
    if (!comparison) return { groups: [], customItems: [] as ComparisonItem[] };

    const skillMap = new Map(itSkillMaster.map(s => [s.id, s]));
    const map = new Map<string, Map<string, ComparisonItem[]>>();
    const customItems: ComparisonItem[] = [];

    for (const item of comparison.items) {
      if (item.itSkillId === null) {
        customItems.push(item);
        continue;
      }
      const master = skillMap.get(item.itSkillId);
      const cat1 = master?.category1Name ?? '未分類';
      const cat2 = master?.category2Name ?? '';
      if (!map.has(cat1)) map.set(cat1, new Map());
      const cat2Map = map.get(cat1)!;
      if (!cat2Map.has(cat2)) cat2Map.set(cat2, []);
      cat2Map.get(cat2)!.push(item);
    }

    const groups = Array.from(map.entries()).map(([cat1, cat2Map]) => ({
      cat1,
      cat2Groups: Array.from(cat2Map.entries()).map(([cat2, items]) => ({ cat2, items })),
    }));

    return { groups, customItems };
  }, [comparison, itSkillMaster]);

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

  const renderRow = (item: ComparisonItem) => (
    <tr key={item.currentDetailId} className={item.diff !== 0 && item.diff !== null ? 'changed-row' : ''}>
      <td>{item.skillName}</td>
      <td>{item.prevLevelValue ?? '—'}</td>
      <td>{item.currentLevelValue}</td>
      <td className={`diff-cell${item.diff != null && item.diff > 0 ? ' diff-up' : item.diff != null && item.diff < 0 ? ' diff-down' : ''}`}>
        {item.diff != null ? (item.diff > 0 ? `+${item.diff}` : item.diff) : '—'}
      </td>
      <td>
        <textarea
          className="remarks-input"
          rows={2}
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
  );

  return (
    <div className="comparison-page">
      <NavBar />

      <main className="comparison-main">
        <button className="page-back-btn" onClick={() => navigate(`/inventory/${inventoryId}`)}>← 棚卸入力に戻る</button>
        <h1 className="page-title">前年度比較 — {comparison.currentFiscalYear}</h1>

        {!comparison.hasPrevYear ? (
          <div className="info-card">
            <p>前年度の棚卸データがありません（初回）。</p>
            <button className="btn btn-primary" onClick={handleNext}><IconArrowRight size={15} />目標振り返りへ</button>
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
                  {comparisonTree.groups.map(({ cat1, cat2Groups }) => (
                    <Fragment key={cat1}>
                      <tr className="scoring-cat1-row">
                        <td colSpan={6}>{cat1}</td>
                      </tr>
                      {cat2Groups.map(({ cat2, items }) => (
                        <Fragment key={`${cat1}-${cat2}`}>
                          {cat2 && (
                            <tr className="scoring-cat2-row">
                              <td colSpan={6}>{cat2}</td>
                            </tr>
                          )}
                          {items.map(renderRow)}
                        </Fragment>
                      ))}
                    </Fragment>
                  ))}
                  {comparisonTree.customItems.length > 0 && (
                    <Fragment key="__custom__">
                      <tr className="scoring-cat1-row">
                        <td colSpan={6}>カスタムスキル ※</td>
                      </tr>
                      {comparisonTree.customItems.map(renderRow)}
                    </Fragment>
                  )}
                </tbody>
              </table>
            </div>

            <div className="action-row">
              <button className="btn btn-primary" onClick={handleNext}>
                <IconArrowRight size={15} />
                目標振り返りへ
              </button>
            </div>
          </>
        )}
      </main>
    </div>
  );
}
