/*******************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * 棚卸比較ページ。今年度と前年度の ITスキルレベルを比較し、差分を確認する。
 * レベルが変化した項目には備考の入力を促す。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/14 hori-ya 短縮記法の制限ルールに合わせて配列コールバック・三項演算子・Optional chaining・Nullish coalescingを排除
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
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

/**
 * 比較項目のソート順を判定する。
 * 大分類の表示順 → 中分類名 → スキルの表示順の優先度で比較する。
 */
function compareComparisonItems(a: ComparisonItem, b: ComparisonItem, skillMap: Map<number, ItSkill>): number {
  let aSkillId = a.itSkillId;
  if (aSkillId == null) {
    aSkillId = -1;
  }
  let bSkillId = b.itSkillId;
  if (bSkillId == null) {
    bSkillId = -1;
  }
  const ma = skillMap.get(aSkillId);
  const mb = skillMap.get(bSkillId);

  let aCat1Sort = 0;
  if (ma != null) {
    aCat1Sort = ma.category1SortOrder;
  }
  let bCat1Sort = 0;
  if (mb != null) {
    bCat1Sort = mb.category1SortOrder;
  }
  if (aCat1Sort !== bCat1Sort) {
    return aCat1Sort - bCat1Sort;
  }

  let aCat2Name = '';
  if (ma != null && ma.category2Name != null) {
    aCat2Name = ma.category2Name;
  }
  let bCat2Name = '';
  if (mb != null && mb.category2Name != null) {
    bCat2Name = mb.category2Name;
  }
  const cat2Compare = aCat2Name.localeCompare(bCat2Name);
  if (cat2Compare !== 0) {
    return cat2Compare;
  }

  let aSortOrder = 0;
  if (ma != null) {
    aSortOrder = ma.sortOrder;
  }
  let bSortOrder = 0;
  if (mb != null) {
    bSortOrder = mb.sortOrder;
  }
  return aSortOrder - bSortOrder;
}

/**
 * 比較項目配列をソート順で並び替える。
 * Array.prototype.sort を使わず、安定な挿入ソートで代替する。
 */
function sortComparisonItems(items: ComparisonItem[], skillMap: Map<number, ItSkill>): ComparisonItem[] {
  const result = [...items];
  for (let i = 1; i < result.length; i++) {
    const current = result[i];
    let j = i - 1;
    while (j >= 0 && compareComparisonItems(result[j], current, skillMap) > 0) {
      result[j + 1] = result[j];
      j--;
    }
    result[j + 1] = current;
  }
  return result;
}

/**
 * 棚卸比較ページ。
 *
 * 今年度と前年度の ITスキルレベルを比較し、差分（上昇・下降）を一覧表示する。
 * レベルが変化した項目には備考入力を促す。
 */
export default function ComparisonPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation('inventory');
  const inventoryId = Number(id);

  const [comparison, setComparison] = useState<ComparisonResponse | null>(null);
  const [itSkillMaster, setItSkillMaster] = useState<ItSkill[]>([]);
  const [editingRemarks, setEditingRemarks] = useState<Record<number, string>>({});
  const [savingId, setSavingId] = useState<number | null>(null);

  // 初期表示時に比較データと ITスキルマスタを取得する
  useEffect(() => {
    Promise.all([
      getComparison(inventoryId),
      getItSkills(),
    ]).then(([compRes, masterRes]) => {
      setComparison(compRes.data);
      setItSkillMaster(masterRes.data);
      const initial: Record<number, string> = {};
      for (const item of compRes.data.items) {
        let remarks = '';
        if (item.currentRemarks != null) {
          remarks = item.currentRemarks;
        }
        initial[item.currentDetailId] = remarks;
      }
      setEditingRemarks(initial);
    });
  }, [inventoryId]);

  const comparisonTree = useMemo(() => {
    if (!comparison) return { groups: [], customItems: [] as ComparisonItem[] };

    const skillMap = new Map<number, ItSkill>();
    for (const s of itSkillMaster) {
      skillMap.set(s.id, s);
    }
    const map = new Map<string, Map<string, ComparisonItem[]>>();
    const customItems: ComparisonItem[] = [];

    const sortedItems = sortComparisonItems(comparison.items, skillMap);

    for (const item of sortedItems) {
      if (item.itSkillId === null) {
        customItems.push(item);
        continue;
      }
      const master = skillMap.get(item.itSkillId);
      let cat1 = '未分類';
      if (master != null && master.category1Name != null) {
        cat1 = master.category1Name;
      }
      let cat2 = '';
      if (master != null && master.category2Name != null) {
        cat2 = master.category2Name;
      }
      if (!map.has(cat1)) map.set(cat1, new Map());
      const cat2Map = map.get(cat1)!;
      if (!cat2Map.has(cat2)) cat2Map.set(cat2, []);
      cat2Map.get(cat2)!.push(item);
    }

    const groups: { cat1: string; cat2Groups: { cat2: string; items: ComparisonItem[] }[] }[] = [];
    for (const [cat1, cat2Map] of map.entries()) {
      const cat2Groups: { cat2: string; items: ComparisonItem[] }[] = [];
      for (const [cat2, items] of cat2Map.entries()) {
        cat2Groups.push({ cat2, items });
      }
      groups.push({ cat1, cat2Groups });
    }

    return { groups, customItems };
  }, [comparison, itSkillMaster]);

  const handleSaveRemarks = async (item: ComparisonItem) => {
    setSavingId(item.currentDetailId);
    try {
      let remarks = '';
      if (editingRemarks[item.currentDetailId] != null) {
        remarks = editingRemarks[item.currentDetailId];
      }
      await patchItSkillRemarks(inventoryId, item.currentDetailId, remarks);
    } finally {
      setSavingId(null);
    }
  };

  const handleNext = () => {
    navigate(`/inventory/${inventoryId}/goal-review`);
  };

  if (!comparison) return <div className="loading">{t('loading')}</div>;

  const changedItems: ComparisonItem[] = [];
  for (const i of comparison.items) {
    if (i.diff !== null && i.diff !== 0) {
      changedItems.push(i);
    }
  }
  const changedWithNoRemarks: ComparisonItem[] = [];
  for (const i of changedItems) {
    if (!editingRemarks[i.currentDetailId]) {
      changedWithNoRemarks.push(i);
    }
  }

  const renderRow = (item: ComparisonItem): React.ReactNode => {
    const isCustom = item.itSkillId === null;

    let rowClassName = '';
    if (item.diff !== 0 && item.diff !== null) {
      rowClassName = 'changed-row';
    }

    let prevLevelContent: React.ReactNode = '—';
    if (item.prevLevelValue != null) {
      prevLevelContent = item.prevLevelValue;
    }

    let currentLevelContent: React.ReactNode = '—';
    if (!isCustom) {
      currentLevelContent = item.currentLevelValue;
    }

    let diffCellSuffix = '';
    if (isCustom) {
      diffCellSuffix = ' diff-new';
    } else if (item.diff != null && item.diff > 0) {
      diffCellSuffix = ' diff-up';
    } else if (item.diff != null && item.diff < 0) {
      diffCellSuffix = ' diff-down';
    }
    const diffCellClassName = `diff-cell${diffCellSuffix}`;

    let diffContent: React.ReactNode = '—';
    if (isCustom) {
      diffContent = t('comparisonPage.diffNew');
    } else if (item.diff != null) {
      if (item.diff > 0) {
        diffContent = `+${item.diff}`;
      } else {
        diffContent = item.diff;
      }
    }

    let remarksValue = '';
    if (editingRemarks[item.currentDetailId] != null) {
      remarksValue = editingRemarks[item.currentDetailId];
    }

    let remarksPlaceholder = t('inventoryPage.table.optional');
    if (item.diff !== 0 && item.diff !== null) {
      remarksPlaceholder = t('comparisonPage.table.changeRemarksPlaceholder');
    }

    let saveButtonLabel = t('inventoryPage.saveButton');
    if (savingId === item.currentDetailId) {
      saveButtonLabel = '...';
    }

    return (
      <tr key={item.currentDetailId} className={rowClassName}>
        <td>{item.skillName}</td>
        <td>{prevLevelContent}</td>
        <td>{currentLevelContent}</td>
        <td className={diffCellClassName}>
          {diffContent}
        </td>
        <td>
          <textarea
            className="remarks-input"
            rows={2}
            value={remarksValue}
            placeholder={remarksPlaceholder}
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
            {saveButtonLabel}
          </button>
        </td>
      </tr>
    );
  };

  let mainContent: React.ReactNode;
  if (!comparison.hasPrevYear) {
    mainContent = (
      <div className="info-card">
        <p>{t('comparisonPage.noPrevYear')}</p>
        <button className="btn btn-primary" onClick={handleNext}><IconArrowRight size={15} />{t('comparisonPage.nextButton')}</button>
      </div>
    );
  } else {
    const groupElements: React.ReactNode[] = [];
    for (const group of comparisonTree.groups) {
      const cat1 = group.cat1;
      const cat2Groups = group.cat2Groups;
      const cat2Elements: React.ReactNode[] = [];
      for (const cat2Group of cat2Groups) {
        const cat2 = cat2Group.cat2;
        const items = cat2Group.items;
        const rowElements: React.ReactNode[] = [];
        for (const item of items) {
          rowElements.push(renderRow(item));
        }
        cat2Elements.push(
          <Fragment key={`${cat1}-${cat2}`}>
            {cat2 && (
              <tr className="scoring-cat2-row">
                <td colSpan={6}>{cat2}</td>
              </tr>
            )}
            {rowElements}
          </Fragment>,
        );
      }
      groupElements.push(
        <Fragment key={cat1}>
          <tr className="scoring-cat1-row">
            <td colSpan={6}>{cat1}</td>
          </tr>
          {cat2Elements}
        </Fragment>,
      );
    }

    const customRowElements: React.ReactNode[] = [];
    for (const item of comparisonTree.customItems) {
      customRowElements.push(renderRow(item));
    }

    mainContent = (
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
              {groupElements}
              {comparisonTree.customItems.length > 0 && (
                <Fragment key="__custom__">
                  <tr className="scoring-cat1-row">
                    <td colSpan={6}>{t('comparisonPage.customSkillLabel')}</td>
                  </tr>
                  {customRowElements}
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
    );
  }

  return (
    <div className="comparison-page">
      <NavBar />

      <main className="comparison-main">
        <button className="page-back-btn" onClick={() => navigate(`/inventory/${inventoryId}`)}>{t('comparisonPage.backButton')}</button>
        <h1 className="page-title">{t('comparisonPage.title', { fiscalYear: comparison.currentFiscalYear })}</h1>

        {mainContent}
      </main>
    </div>
  );
}
