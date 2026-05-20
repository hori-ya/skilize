import { Fragment, useEffect, useState, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getComparison, patchItSkillRemarks } from '../api/inventoryApi';
import { getItSkills } from '../../../shared/api/masterApi';
import type { ComparisonResponse, ComparisonItem } from '../types/index';
import type { ItSkill } from '../../../shared/types/master';
import NavBar from '../../../app/layouts/NavBar';
import { IconArrowRight } from '../../../shared/ui/Icons';
import StickyHorizontalScroll from '../../../shared/ui/StickyHorizontalScroll';
import { useTranslation } from 'react-i18next';

export default function ComparisonPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation('inventory');
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

    const sortedItems = [...comparison.items].sort((a, b) => {
      const ma = skillMap.get(a.itSkillId ?? -1);
      const mb = skillMap.get(b.itSkillId ?? -1);
      return (ma?.category1SortOrder ?? 0) - (mb?.category1SortOrder ?? 0) ||
        (ma?.category2Name ?? '').localeCompare(mb?.category2Name ?? '') ||
        (ma?.sortOrder ?? 0) - (mb?.sortOrder ?? 0);
    });

    for (const item of sortedItems) {
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

  if (!comparison) return <div className="loading">{t('loading')}</div>;

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
          placeholder={item.diff !== 0 && item.diff !== null ? t('comparisonPage.table.changeRemarksPlaceholder') : t('inventoryPage.table.optional')}
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
          {savingId === item.currentDetailId ? '...' : t('inventoryPage.saveButton')}
        </button>
      </td>
    </tr>
  );

  return (
    <div className="comparison-page">
      <NavBar />

      <main className="comparison-main">
        <button className="page-back-btn" onClick={() => navigate(`/inventory/${inventoryId}`)}>{t('comparisonPage.backButton')}</button>
        <h1 className="page-title">{t('comparisonPage.title', { fiscalYear: comparison.currentFiscalYear })}</h1>

        {!comparison.hasPrevYear ? (
          <div className="info-card">
            <p>{t('comparisonPage.noPrevYear')}</p>
            <button className="btn btn-primary" onClick={handleNext}><IconArrowRight size={15} />{t('comparisonPage.nextButton')}</button>
          </div>
        ) : (
          <>
            <p className="comparison-subtitle">
              {t('comparisonPage.subtitle', { prevYear: comparison.prevFiscalYear })}
            </p>

            {changedWithNoRemarks.length > 0 && (
              <div className="alert alert-info">
                {t('comparisonPage.remarksAlert')}
              </div>
            )}

            <StickyHorizontalScroll className="comparison-table-wrapper">
              <table className="comparison-table">
                <thead>
                  <tr>
                    <th>{t('comparisonPage.table.skillName')}</th>
                    <th>{t('comparisonPage.table.prevYear')}</th>
                    <th>{t('comparisonPage.table.currentYear')}</th>
                    <th>{t('comparisonPage.table.diff')}</th>
                    <th>{t('comparisonPage.table.remarks')}</th>
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
                        <td colSpan={6}>{t('comparisonPage.customSkillLabel')}</td>
                      </tr>
                      {comparisonTree.customItems.map(renderRow)}
                    </Fragment>
                  )}
                </tbody>
              </table>
            </StickyHorizontalScroll>

            <div className="action-row">
              <button className="btn btn-primary" onClick={handleNext}>
                <IconArrowRight size={15} />
                {t('comparisonPage.nextButton')}
              </button>
            </div>
          </>
        )}
      </main>
    </div>
  );
}
