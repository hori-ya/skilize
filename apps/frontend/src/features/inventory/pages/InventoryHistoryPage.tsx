import { useEffect, useState, useMemo, Fragment } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  getMyInventories, getItSkillDetails, getQualificationDetails,
  getSeminarDetails, getGoals, getComparison, patchItSkillRemarks, getGoalReview,
  getMyAiAnalyses,
} from '../api/inventoryApi';
import { getItSkills, getFiscalYears } from '../../../shared/api/masterApi';
import type { FiscalYear } from '../../../shared/types/master';
import type {
  InventorySummary, ItSkillDetailItem, QualificationDetailItem,
  SeminarDetailItem, GoalItem, ComparisonResponse, GoalReviewItem, AiAnalysis,
} from '../types/index';
import type { ItSkill } from '../../../shared/types/master';
import NavBar from '../../../app/layouts/NavBar';
import AiAnalysisCard from '../components/AiAnalysisCard';
import StickyHorizontalScroll from '../../../shared/ui/StickyHorizontalScroll';
import { useTranslation } from 'react-i18next';

type TabKey = 'it-skills' | 'qualifications' | 'seminars' | 'goals' | 'ai-analysis';

const GOAL_CATEGORY_KEY: Record<string, string> = {
  IT_SKILL: 'historyPage.goalCategory.itSkill',
  QUALIFICATION: 'historyPage.goalCategory.qualification',
  AD: 'historyPage.goalCategory.ad',
};

const ACHIEVEMENT_KEY: Record<string, string> = {
  ACHIEVED: 'historyPage.achievement.achieved',
  PARTIAL: 'historyPage.achievement.partial',
  NOT_ACHIEVED: 'historyPage.achievement.notAchieved',
};

function DiffCell({ diff, hasPrevYear }: { diff: number | null | undefined; hasPrevYear: boolean }) {
  const { t } = useTranslation('inventory');
  if (!hasPrevYear) return null;
  if (diff === null || diff === undefined) return <span className="diff-new">{t('historyPage.diffNew')}</span>;
  if (diff > 0) return <span className="diff-up">↑ +{diff}</span>;
  if (diff < 0) return <span className="diff-down">↓ {diff}</span>;
  return <span>—</span>;
}

export default function InventoryHistoryPage() {
  const navigate = useNavigate();
  const { t } = useTranslation('inventory');

  const TAB_LABELS: Record<TabKey, string> = {
    'it-skills': t('historyPage.tab.itSkills'),
    qualifications: t('historyPage.tab.qualifications'),
    seminars: t('historyPage.tab.seminars'),
    goals: t('historyPage.tab.goals'),
    'ai-analysis': t('historyPage.tab.aiAnalysis'),
  };

  const getStatusLabel = (status: string) => {
    const map: Record<string, string> = {
      DRAFT: t('historyPage.status.draft'),
      PENDING_GOAL: t('historyPage.status.pendingGoal'),
      COMPLETED: t('historyPage.status.completed'),
    };
    return map[status] ?? status;
  };

  const [inventories, setInventories] = useState<InventorySummary[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [activeTab, setActiveTab] = useState<TabKey>('it-skills');
  const [loading, setLoading] = useState(false);

  const [itSkillDetails, setItSkillDetails] = useState<ItSkillDetailItem[]>([]);
  const [itSkillMaster, setItSkillMaster] = useState<ItSkill[]>([]);
  const [qualificationDetails, setQualificationDetails] = useState<QualificationDetailItem[]>([]);
  const [seminarDetails, setSeminarDetails] = useState<SeminarDetailItem[]>([]);
  const [goals, setGoals] = useState<GoalItem[]>([]);
  const [comparison, setComparison] = useState<ComparisonResponse | null>(null);
  const [goalReviewMap, setGoalReviewMap] = useState<Map<number, GoalReviewItem>>(new Map());
  const [prevGoals, setPrevGoals] = useState<GoalItem[]>([]);
  const [prevGoalReviewMap, setPrevGoalReviewMap] = useState<Map<number, GoalReviewItem>>(new Map());

  const [editingRemarks, setEditingRemarks] = useState<Record<number, string>>({});
  const [savingId, setSavingId] = useState<number | null>(null);
  const [aiAnalyses, setAiAnalyses] = useState<AiAnalysis[]>([]);

  const [fiscalYears, setFiscalYears] = useState<FiscalYear[]>([]);

  // Filter states
  const [itSkillSearch, setItSkillSearch] = useState('');
  const [itSkillCategory1Filter, setItSkillCategory1Filter] = useState('');
  const [itSkillCategory2Filter, setItSkillCategory2Filter] = useState('');
  const [itSkillDiffFilter, setItSkillDiffFilter] = useState<'' | 'up' | 'down' | 'new'>('');
  const [qualNameSearch, setQualNameSearch] = useState('');
  const [qualCategoryFilter, setQualCategoryFilter] = useState('');
  const [qualFiscalYearFilter, setQualFiscalYearFilter] = useState('');
  const [seminarNameSearch, setSeminarNameSearch] = useState('');
  const [seminarCategoryFilter, setSeminarCategoryFilter] = useState('');
  const [seminarFiscalYearFilter, setSeminarFiscalYearFilter] = useState('');
  const [seminarTypeFilter, setSeminarTypeFilter] = useState<'' | 'AD' | 'FREE'>('');
  const [goalSearch, setGoalSearch] = useState('');
  const [goalCategoryFilter, setGoalCategoryFilter] = useState<'' | 'IT_SKILL' | 'QUALIFICATION' | 'AD'>('');

  useEffect(() => {
    getMyInventories().then(res => {
      setInventories(res.data);
      if (res.data.length > 0) setSelectedId(res.data[0].id);
    });
    getMyAiAnalyses().then(res => setAiAnalyses(res.data)).catch(() => {});
    getFiscalYears().then(res => setFiscalYears(res.data)).catch(() => {});
  }, []);

  useEffect(() => {
    setItSkillSearch('');
    setItSkillCategory1Filter('');
    setItSkillCategory2Filter('');
    setItSkillDiffFilter('');
    setQualNameSearch('');
    setQualCategoryFilter('');
    setQualFiscalYearFilter('');
    setSeminarNameSearch('');
    setSeminarCategoryFilter('');
    setSeminarFiscalYearFilter('');
    setSeminarTypeFilter('');
    setGoalSearch('');
    setGoalCategoryFilter('');
  }, [selectedId]);

  useEffect(() => {
    if (!selectedId) return;
    setLoading(true);

    const selectedIndex = inventories.findIndex(inv => inv.id === selectedId);
    const nextInventoryId = selectedIndex > 0 ? inventories[selectedIndex - 1].id : null;
    const prevInventoryId = selectedIndex < inventories.length - 1 ? inventories[selectedIndex + 1].id : null;

    Promise.all([
      getItSkillDetails(selectedId),
      getItSkills(),
      getQualificationDetails(selectedId),
      getSeminarDetails(selectedId),
      getGoals(selectedId),
      getComparison(selectedId).catch(() => null),
      nextInventoryId ? getGoalReview(nextInventoryId).catch(() => null) : Promise.resolve(null),
      prevInventoryId ? getGoals(prevInventoryId).catch(() => null) : Promise.resolve(null),
      prevInventoryId ? getGoalReview(selectedId).catch(() => null) : Promise.resolve(null),
    ]).then(([itRes, masterRes, qualRes, semRes, goalRes, compRes, reviewRes, prevGoalsRes, prevReviewRes]) => {
      const itItems = itRes.data.items;
      setItSkillDetails(itItems);
      setItSkillMaster(masterRes.data);
      setQualificationDetails(qualRes.data.items);
      setSeminarDetails(semRes.data.items);
      setGoals(goalRes.data.items);
      setComparison(compRes?.data ?? null);

      const remarks: Record<number, string> = {};
      itItems.forEach(item => { remarks[item.id] = item.remarks ?? ''; });
      setEditingRemarks(remarks);

      const reviewMap = new Map<number, GoalReviewItem>();
      if (reviewRes?.data?.items) {
        for (const item of reviewRes.data.items) {
          reviewMap.set(item.prevGoalId, item);
        }
      }
      setGoalReviewMap(reviewMap);

      setPrevGoals(prevGoalsRes?.data?.items ?? []);

      const prevRevMap = new Map<number, GoalReviewItem>();
      if (prevReviewRes?.data?.items) {
        for (const item of prevReviewRes.data.items) {
          prevRevMap.set(item.prevGoalId, item);
        }
      }
      setPrevGoalReviewMap(prevRevMap);
    }).finally(() => setLoading(false));
  }, [selectedId, inventories]);

  const itSkillTree = useMemo(() => {
    const skillMap = new Map(itSkillMaster.map(s => [s.id, s]));
    const map = new Map<string, Map<string, ItSkillDetailItem[]>>();
    const customItems: ItSkillDetailItem[] = [];

    const sortedDetails = [...itSkillDetails].sort((a, b) => {
      const ma = skillMap.get(a.itSkillId ?? -1);
      const mb = skillMap.get(b.itSkillId ?? -1);
      return (ma?.category1SortOrder ?? 0) - (mb?.category1SortOrder ?? 0) ||
        (ma?.category2Name ?? '').localeCompare(mb?.category2Name ?? '') ||
        (ma?.sortOrder ?? 0) - (mb?.sortOrder ?? 0);
    });

    for (const detail of sortedDetails) {
      if (detail.itSkillId === null) {
        customItems.push(detail);
      } else {
        const master = skillMap.get(detail.itSkillId);
        const cat1 = master?.category1Name ?? '未分類';
        const cat2 = master?.category2Name ?? '';
        if (!map.has(cat1)) map.set(cat1, new Map());
        const cat2Map = map.get(cat1)!;
        if (!cat2Map.has(cat2)) cat2Map.set(cat2, []);
        cat2Map.get(cat2)!.push(detail);
      }
    }

    const groups = Array.from(map.entries()).map(([cat1, cat2Map]) => ({
      cat1,
      cat2Groups: Array.from(cat2Map.entries()).map(([cat2, items]) => ({ cat2, items })),
    }));

    return { groups, customItems };
  }, [itSkillDetails, itSkillMaster]);

  const comparisonMap = useMemo(() => {
    if (!comparison) return new Map<number, { prevLevelValue: number | null; diff: number | null }>();
    return new Map(comparison.items.map(item => [item.currentDetailId, item]));
  }, [comparison]);

  const hasPrevYear = comparison?.hasPrevYear ?? false;

  const itSkillCat2Options = useMemo(() => {
    const sourceGroups = itSkillCategory1Filter
      ? itSkillTree.groups.filter(g => g.cat1 === itSkillCategory1Filter)
      : itSkillTree.groups;
    const seen = new Set<string>();
    for (const g of sourceGroups) {
      for (const cg of g.cat2Groups) {
        if (cg.cat2) seen.add(cg.cat2);
      }
    }
    return Array.from(seen);
  }, [itSkillTree, itSkillCategory1Filter]);

  const filteredItSkillTree = useMemo(() => {
    const searchLower = itSkillSearch.toLowerCase();
    const filteredGroups = itSkillTree.groups
      .filter(g => !itSkillCategory1Filter || g.cat1 === itSkillCategory1Filter)
      .map(g => ({
        cat1: g.cat1,
        cat2Groups: g.cat2Groups
          .filter(cg => !itSkillCategory2Filter || cg.cat2 === itSkillCategory2Filter)
          .map(cg => ({
            cat2: cg.cat2,
            items: cg.items.filter(item => {
              if (searchLower && !item.itSkillName?.toLowerCase().includes(searchLower)) return false;
              if (itSkillDiffFilter) {
                const comp = comparisonMap.get(item.id);
                if (itSkillDiffFilter === 'new' && comp !== undefined) return false;
                if (itSkillDiffFilter === 'up' && (comp === undefined || (comp.diff ?? 0) <= 0)) return false;
                if (itSkillDiffFilter === 'down' && (comp === undefined || (comp.diff ?? 0) >= 0)) return false;
              }
              return true;
            }),
          })).filter(cg => cg.items.length > 0),
      }))
      .filter(g => g.cat2Groups.length > 0);
    const filteredCustom = itSkillCategory1Filter === '__custom__'
      ? itSkillTree.customItems.filter(item =>
          !searchLower || item.customSkillName?.toLowerCase().includes(searchLower)
        )
      : (itSkillCategory1Filter || itSkillCategory2Filter || itSkillDiffFilter === 'up' || itSkillDiffFilter === 'down')
        ? []
        : itSkillTree.customItems.filter(item =>
            !searchLower || item.customSkillName?.toLowerCase().includes(searchLower)
          );
    return { groups: filteredGroups, customItems: filteredCustom };
  }, [itSkillTree, itSkillSearch, itSkillCategory1Filter, itSkillCategory2Filter, itSkillDiffFilter, comparisonMap]);

  const filteredItSkillCount = useMemo(() => {
    let count = 0;
    for (const { cat2Groups } of filteredItSkillTree.groups) {
      for (const { items } of cat2Groups) count += items.length;
    }
    return count + filteredItSkillTree.customItems.length;
  }, [filteredItSkillTree]);

  const qualCategories = useMemo(() =>
    [...new Set(qualificationDetails.map(q => q.qualificationCategoryName).filter((c): c is string => c !== null))].sort(),
    [qualificationDetails]
  );

  const seminarAdCategories = useMemo(() =>
    [...new Set(seminarDetails.filter(s => s.adSeminarId !== null).map(s => s.adSeminarCategoryName).filter((c): c is string => c !== null))].sort(),
    [seminarDetails]
  );

  const filteredQualifications = useMemo(() => {
    const searchLower = qualNameSearch.toLowerCase();
    return qualificationDetails.filter(q => {
      if (qualCategoryFilter) {
        if (qualCategoryFilter === '__custom__') {
          if (q.qualificationId !== null) return false;
        } else {
          if (q.qualificationCategoryName !== qualCategoryFilter) return false;
        }
      }
      if (searchLower) {
        const name = (q.qualificationName ?? q.customQualificationName ?? '').toLowerCase();
        if (!name.includes(searchLower)) return false;
      }
      if (qualFiscalYearFilter) {
        const fy = fiscalYears.find(f => String(f.id) === qualFiscalYearFilter);
        if (fy) {
          if (!q.acquiredYearMonth) return false;
          const ym = q.acquiredYearMonth.slice(0, 7);
          if (ym < fy.startDate.slice(0, 7) || ym > fy.endDate.slice(0, 7)) return false;
        }
      }
      return true;
    });
  }, [qualificationDetails, qualNameSearch, qualCategoryFilter, qualFiscalYearFilter, fiscalYears]);

  const filteredSeminars = useMemo(() => {
    const searchLower = seminarNameSearch.toLowerCase();
    return seminarDetails.filter(s => {
      if (seminarTypeFilter === 'AD' && s.adSeminarId === null) return false;
      if (seminarTypeFilter === 'FREE' && s.adSeminarId !== null) return false;
      if (seminarCategoryFilter && s.adSeminarCategoryName !== seminarCategoryFilter) return false;
      if (searchLower) {
        const name = (s.adSeminarName ?? s.seminarName ?? '').toLowerCase();
        if (!name.includes(searchLower)) return false;
      }
      if (seminarFiscalYearFilter) {
        const fy = fiscalYears.find(f => String(f.id) === seminarFiscalYearFilter);
        if (fy) {
          if (!s.attendedYearMonth) return false;
          const ym = s.attendedYearMonth.slice(0, 7);
          if (ym < fy.startDate.slice(0, 7) || ym > fy.endDate.slice(0, 7)) return false;
        }
      }
      return true;
    });
  }, [seminarDetails, seminarNameSearch, seminarTypeFilter, seminarCategoryFilter, seminarFiscalYearFilter, fiscalYears]);

  const filteredGoals = useMemo(() => {
    const searchLower = goalSearch.toLowerCase();
    return goals.filter(g => {
      if (goalCategoryFilter && g.goalCategory !== goalCategoryFilter) return false;
      if (searchLower) {
        const name = (g.itSkillName ?? g.qualificationName ?? g.adSeminarName ?? g.customName ?? '').toLowerCase();
        if (!name.includes(searchLower)) return false;
      }
      return true;
    });
  }, [goals, goalSearch, goalCategoryFilter]);

  const filteredPrevGoals = useMemo(() => {
    const searchLower = goalSearch.toLowerCase();
    return prevGoals.filter(g => {
      if (goalCategoryFilter && g.goalCategory !== goalCategoryFilter) return false;
      if (searchLower) {
        const name = (g.itSkillName ?? g.qualificationName ?? g.adSeminarName ?? g.customName ?? '').toLowerCase();
        if (!name.includes(searchLower)) return false;
      }
      return true;
    });
  }, [prevGoals, goalSearch, goalCategoryFilter]);

  // The first inventory in the list is the most recent (current year's)
  const isCurrentYear = inventories.length > 0 && selectedId === inventories[0].id;

  const itSkillColCount = 3 + (hasPrevYear ? 2 : 0) + (isCurrentYear ? 1 : 0);

  const handleSaveRemarks = async (detailId: number) => {
    if (!selectedId) return;
    setSavingId(detailId);
    try {
      await patchItSkillRemarks(selectedId, detailId, editingRemarks[detailId] ?? '');
    } finally {
      setSavingId(null);
    }
  };

  return (
    <div className="history-page">
      <NavBar />
      <main className="history-main">
        <button className="page-back-btn" onClick={() => navigate('/')}>{t('historyPage.backButton')}</button>
        <h1 className="page-title">{t('historyPage.title')}</h1>

        {inventories.length === 0 ? (
          <div className="info-card"><p>{t('historyPage.noData')}</p></div>
        ) : (
          <>
            <div className="history-selector-row">
              <label className="form-label">{t('historyPage.yearLabel')}</label>
              <select
                className="select history-year-select"
                value={selectedId ?? ''}
                onChange={e => setSelectedId(Number(e.target.value))}
              >
                {inventories.map(inv => (
                  <option key={inv.id} value={inv.id}>
                    {inv.fiscalYear.name}（{getStatusLabel(inv.status)}）
                  </option>
                ))}
              </select>
            </div>

            {loading ? (
              <div className="loading">{t('loading')}</div>
            ) : (
              <>
                <div className="tab-bar">
                  {(Object.keys(TAB_LABELS) as TabKey[]).map(tab => (
                    <button
                      key={tab}
                      className={`tab-btn${activeTab === tab ? ' active' : ''}`}
                      onClick={() => setActiveTab(tab)}
                    >
                      {TAB_LABELS[tab]}
                    </button>
                  ))}
                </div>

                {/* ── ITスキルタブ ── */}
                {activeTab === 'it-skills' && (
                  <div className="history-tab-content">
                    {itSkillDetails.length > 0 && (
                      <div className="history-filter-bar">
                        <input
                          className="history-filter-bar__input"
                          placeholder={t('historyPage.filter.skillSearch')}
                          value={itSkillSearch}
                          onChange={e => setItSkillSearch(e.target.value)}
                        />
                        <select
                          className="history-filter-bar__select"
                          value={itSkillCategory1Filter}
                          onChange={e => { setItSkillCategory1Filter(e.target.value); setItSkillCategory2Filter(''); }}
                        >
                          <option value="">{t('historyPage.filter.category1All')}</option>
                          {itSkillTree.groups.map(({ cat1 }) => (
                            <option key={cat1} value={cat1}>{cat1}</option>
                          ))}
                          {itSkillTree.customItems.length > 0 && (
                            <option value="__custom__">{t('historyPage.filter.custom')}</option>
                          )}
                        </select>
                        {itSkillCat2Options.length > 0 && (
                          <select
                            className="history-filter-bar__select"
                            value={itSkillCategory2Filter}
                            onChange={e => setItSkillCategory2Filter(e.target.value)}
                          >
                            <option value="">{t('historyPage.filter.category2All')}</option>
                            {itSkillCat2Options.map(cat2 => (
                              <option key={cat2} value={cat2}>{cat2}</option>
                            ))}
                          </select>
                        )}
                        {hasPrevYear && (
                          <select
                            className="history-filter-bar__select"
                            value={itSkillDiffFilter}
                            onChange={e => setItSkillDiffFilter(e.target.value as '' | 'up' | 'down' | 'new')}
                          >
                            <option value="">{t('historyPage.filter.diffAll')}</option>
                            <option value="up">{t('historyPage.filter.diffUp')}</option>
                            <option value="down">{t('historyPage.filter.diffDown')}</option>
                            <option value="new">{t('historyPage.filter.diffNew')}</option>
                          </select>
                        )}
                        <span className="history-result-count">{filteredItSkillCount}件</span>
                      </div>
                    )}
                    <StickyHorizontalScroll className="comparison-table-wrapper">
                      <table className="comparison-table">
                        <thead>
                          <tr>
                            <th>{t('historyPage.table.skillName')}</th>
                            {hasPrevYear && <th>{t('historyPage.table.prevYear')}</th>}
                            <th>{t('historyPage.table.currentYear')}</th>
                            {hasPrevYear && <th>{t('historyPage.table.diff')}</th>}
                            <th>{t('historyPage.table.remarks')}</th>
                            {isCurrentYear && <th></th>}
                          </tr>
                        </thead>
                        <tbody>
                          {itSkillDetails.length === 0 ? (
                            <tr>
                              <td colSpan={itSkillColCount} className="no-data-cell">
                                {t('historyPage.noDataCell.itSkills')}
                              </td>
                            </tr>
                          ) : filteredItSkillCount === 0 ? (
                            <tr>
                              <td colSpan={itSkillColCount} className="no-data-cell">
                                {t('historyPage.noDataCell.noMatchSkills')}
                              </td>
                            </tr>
                          ) : (
                            <>
                              {filteredItSkillTree.groups.map(({ cat1, cat2Groups }) => (
                                <Fragment key={cat1}>
                                  <tr className="scoring-cat1-row">
                                    <td colSpan={itSkillColCount}>{cat1}</td>
                                  </tr>
                                  {cat2Groups.map(({ cat2, items }) => (
                                    <Fragment key={`${cat1}-${cat2}`}>
                                      {cat2 && (
                                        <tr className="scoring-cat2-row">
                                          <td colSpan={itSkillColCount}>{cat2}</td>
                                        </tr>
                                      )}
                                  {items.map(detail => {
                                    const comp = comparisonMap.get(detail.id);
                                    return (
                                      <tr key={detail.id}>
                                        <td>{detail.itSkillName}</td>
                                        {hasPrevYear && <td>{comp?.prevLevelValue ?? '—'}</td>}
                                        <td>{detail.levelValue}</td>
                                        {hasPrevYear && (
                                          <td className="diff-cell">
                                            <DiffCell diff={comp?.diff} hasPrevYear={hasPrevYear} />
                                          </td>
                                        )}
                                        <td>
                                          {isCurrentYear ? (
                                            <textarea
                                              className="remarks-input"
                                              rows={2}
                                              value={editingRemarks[detail.id] ?? ''}
                                              onChange={e => setEditingRemarks(prev => ({
                                                ...prev, [detail.id]: e.target.value,
                                              }))}
                                            />
                                          ) : (
                                            detail.remarks || '—'
                                          )}
                                        </td>
                                        {isCurrentYear && (
                                          <td>
                                            <button
                                              className="btn btn-sm"
                                              onClick={() => handleSaveRemarks(detail.id)}
                                              disabled={savingId === detail.id}
                                            >
                                              {savingId === detail.id ? '...' : t('inventoryPage.saveButton')}
                                            </button>
                                          </td>
                                        )}
                                      </tr>
                                    );
                                  })}
                                    </Fragment>
                                  ))}
                                </Fragment>
                              ))}
                              {filteredItSkillTree.customItems.length > 0 && (
                                <Fragment key="__custom__">
                                  <tr className="scoring-cat1-row">
                                    <td colSpan={itSkillColCount}>{t('historyPage.customSkillLabel')}</td>
                                  </tr>
                                  {filteredItSkillTree.customItems.map(detail => (
                                    <tr key={detail.id}>
                                      <td>{detail.customSkillName} ※</td>
                                      {hasPrevYear && <td>—</td>}
                                      <td>—</td>
                                      {hasPrevYear && (
                                        <td className="diff-cell">
                                          <DiffCell diff={null} hasPrevYear={hasPrevYear} />
                                        </td>
                                      )}
                                      <td>
                                        {isCurrentYear ? (
                                          <textarea
                                            className="remarks-input"
                                            rows={2}
                                            value={editingRemarks[detail.id] ?? ''}
                                            onChange={e => setEditingRemarks(prev => ({
                                              ...prev, [detail.id]: e.target.value,
                                            }))}
                                          />
                                        ) : (
                                          detail.remarks || '—'
                                        )}
                                      </td>
                                      {isCurrentYear && (
                                        <td>
                                          <button
                                            className="btn btn-sm"
                                            onClick={() => handleSaveRemarks(detail.id)}
                                            disabled={savingId === detail.id}
                                          >
                                            {savingId === detail.id ? '...' : t('inventoryPage.saveButton')}
                                          </button>
                                        </td>
                                      )}
                                    </tr>
                                  ))}
                                </Fragment>
                              )}
                            </>
                          )}
                        </tbody>
                      </table>
                    </StickyHorizontalScroll>
                  </div>
                )}

                {/* ── 資格タブ ── */}
                {activeTab === 'qualifications' && (
                  <div className="history-tab-content">
                    {qualificationDetails.length === 0 ? (
                      <p className="no-data">{t('historyPage.noDataCell.qualifications')}</p>
                    ) : (
                      <>
                        <div className="history-filter-bar">
                          <input
                            className="history-filter-bar__input"
                            placeholder={t('historyPage.filter.qualNameSearch')}
                            value={qualNameSearch}
                            onChange={e => setQualNameSearch(e.target.value)}
                          />
                          <select
                            className="history-filter-bar__select"
                            value={qualCategoryFilter}
                            onChange={e => setQualCategoryFilter(e.target.value)}
                          >
                            <option value="">{t('historyPage.filter.qualCategoryAll')}</option>
                            {qualCategories.map(cat => (
                              <option key={cat} value={cat}>{cat}</option>
                            ))}
                            {qualificationDetails.some(q => q.qualificationId === null) && (
                              <option value="__custom__">{t('historyPage.filter.custom')}</option>
                            )}
                          </select>
                          <select
                            className="history-filter-bar__select"
                            value={qualFiscalYearFilter}
                            onChange={e => setQualFiscalYearFilter(e.target.value)}
                          >
                            <option value="">{t('historyPage.filter.qualFiscalYearAll')}</option>
                            {fiscalYears.map(fy => (
                              <option key={fy.id} value={String(fy.id)}>{fy.name}</option>
                            ))}
                          </select>
                          <span className="history-result-count">{filteredQualifications.length}件</span>
                        </div>
                        <StickyHorizontalScroll className="master-table-wrap">
                          <table className="master-table">
                            <thead>
                              <tr>
                                <th>{t('historyPage.table.category')}</th>
                                <th>{t('historyPage.table.qualName')}</th>
                                <th>{t('historyPage.table.acquiredYearMonth')}</th>
                                <th>{t('historyPage.table.remarks')}</th>
                              </tr>
                            </thead>
                            <tbody>
                              {filteredQualifications.length === 0 ? (
                                <tr><td colSpan={4} className="no-data-cell">{t('historyPage.noDataCell.noMatchQuals')}</td></tr>
                              ) : (
                                filteredQualifications.map(q => (
                                  <tr key={q.id}>
                                    <td>{q.qualificationCategoryName ?? '—'}</td>
                                    <td>
                                      {q.qualificationName ?? q.customQualificationName ?? '—'}
                                      {q.qualificationId === null && ' ※'}
                                    </td>
                                    <td>{q.acquiredYearMonth?.slice(0, 7) ?? '—'}</td>
                                    <td>{q.remarks || '—'}</td>
                                  </tr>
                                ))
                              )}
                            </tbody>
                          </table>
                        </StickyHorizontalScroll>
                      </>
                    )}
                  </div>
                )}

                {/* ── セミナータブ ── */}
                {activeTab === 'seminars' && (
                  <div className="history-tab-content">
                    {seminarDetails.length === 0 ? (
                      <p className="no-data">{t('historyPage.noDataCell.seminars')}</p>
                    ) : (
                      <>
                        <div className="history-filter-bar">
                          <input
                            className="history-filter-bar__input"
                            placeholder={t('historyPage.filter.seminarNameSearch')}
                            value={seminarNameSearch}
                            onChange={e => setSeminarNameSearch(e.target.value)}
                          />
                          <select
                            className="history-filter-bar__select"
                            value={seminarTypeFilter}
                            onChange={e => { setSeminarTypeFilter(e.target.value as '' | 'AD' | 'FREE'); setSeminarCategoryFilter(''); }}
                          >
                            <option value="">{t('historyPage.filter.seminarTypeAll')}</option>
                            <option value="AD">AD</option>
                            <option value="FREE">{t('historyPage.filter.seminarTypeFree')}</option>
                          </select>
                          {seminarTypeFilter !== 'FREE' && seminarAdCategories.length > 0 && (
                            <select
                              className="history-filter-bar__select"
                              value={seminarCategoryFilter}
                              onChange={e => setSeminarCategoryFilter(e.target.value)}
                            >
                              <option value="">{t('historyPage.filter.seminarCategoryAll')}</option>
                              {seminarAdCategories.map(cat => (
                                <option key={cat} value={cat}>{cat}</option>
                              ))}
                            </select>
                          )}
                          <select
                            className="history-filter-bar__select"
                            value={seminarFiscalYearFilter}
                            onChange={e => setSeminarFiscalYearFilter(e.target.value)}
                          >
                            <option value="">{t('historyPage.filter.seminarFiscalYearAll')}</option>
                            {fiscalYears.map(fy => (
                              <option key={fy.id} value={String(fy.id)}>{fy.name}</option>
                            ))}
                          </select>
                          <span className="history-result-count">{filteredSeminars.length}件</span>
                        </div>
                        <StickyHorizontalScroll className="master-table-wrap">
                          <table className="master-table">
                            <thead>
                              <tr>
                                <th>{t('historyPage.table.seminarType')}</th>
                                <th>{t('historyPage.table.category')}</th>
                                <th>{t('historyPage.table.seminarName')}</th>
                                <th>{t('historyPage.table.attendedYearMonth')}</th>
                                <th>{t('historyPage.table.remarks')}</th>
                              </tr>
                            </thead>
                            <tbody>
                              {filteredSeminars.length === 0 ? (
                                <tr><td colSpan={5} className="no-data-cell">{t('historyPage.noDataCell.noMatchSeminars')}</td></tr>
                              ) : (
                                filteredSeminars.map(s => (
                                  <tr key={s.id}>
                                    <td>{s.adSeminarId !== null ? 'AD' : t('historyPage.filter.seminarTypeFree')}</td>
                                    <td>{s.adSeminarId !== null ? (s.adSeminarCategoryName ?? '—') : '—'}</td>
                                    <td>{s.adSeminarName ?? s.seminarName ?? '—'}</td>
                                    <td>{s.attendedYearMonth?.slice(0, 7) ?? '—'}</td>
                                    <td>{s.remarks || '—'}</td>
                                  </tr>
                                ))
                              )}
                            </tbody>
                          </table>
                        </StickyHorizontalScroll>
                      </>
                    )}
                  </div>
                )}

                {/* ── AI分析タブ ── */}
                {activeTab === 'ai-analysis' && (
                  <div className="history-tab-content">
                    {(() => {
                      const analysis = aiAnalyses.find(a => {
                        const inv = inventories.find(i => i.id === selectedId);
                        return inv && a.fiscalYearId === inv.fiscalYear.id;
                      });
                      if (!analysis) return <p className="no-data">{t('historyPage.noDataCell.aiAnalysis')}</p>;
                      return <AiAnalysisCard analysis={analysis} />;
                    })()}
                  </div>
                )}

                {/* ── 目標タブ ── */}
                {activeTab === 'goals' && (
                  <div className="history-tab-content">
                    {prevGoals.length === 0 && goals.length === 0 ? (
                      <p className="no-data">{t('historyPage.noDataCell.goals')}</p>
                    ) : (
                      <>
                        <div className="history-filter-bar">
                          <input
                            className="history-filter-bar__input"
                            placeholder={t('historyPage.filter.goalSearch')}
                            value={goalSearch}
                            onChange={e => setGoalSearch(e.target.value)}
                          />
                          <select
                            className="history-filter-bar__select"
                            value={goalCategoryFilter}
                            onChange={e => setGoalCategoryFilter(e.target.value as '' | 'IT_SKILL' | 'QUALIFICATION' | 'AD')}
                          >
                            <option value="">{t('historyPage.filter.goalCategoryAll')}</option>
                            <option value="IT_SKILL">{t('historyPage.goalCategory.itSkill')}</option>
                            <option value="QUALIFICATION">{t('historyPage.goalCategory.qualification')}</option>
                            <option value="AD">AD</option>
                          </select>
                          <span className="history-result-count">
                            {filteredPrevGoals.length + filteredGoals.length}件
                          </span>
                        </div>
                        {prevGoals.length > 0 && (
                          <div className="history-goal-section">
                            <h3 className="history-goal-title">{t('historyPage.goalSection.prevYear')}</h3>
                            {filteredPrevGoals.length === 0 ? (
                              <p className="no-data">{t('historyPage.noDataCell.noMatchGoals')}</p>
                            ) : (
                              <StickyHorizontalScroll className="master-table-wrap">
                                <table className="master-table">
                                  <thead>
                                    <tr>
                                      <th style={{ width: 80 }}>{t('historyPage.table.goalCategory')}</th>
                                      <th>{t('historyPage.table.goalName')}</th>
                                      <th style={{ width: 120 }}>{t('historyPage.table.targetPeriod')}</th>
                                      <th>{t('historyPage.table.reasonPlan')}</th>
                                      <th style={{ width: 90 }}>{t('historyPage.table.achievementStatus')}</th>
                                      <th>{t('historyPage.table.reviewNote')}</th>
                                    </tr>
                                  </thead>
                                  <tbody>
                                    {filteredPrevGoals.map(g => {
                                      const review = prevGoalReviewMap.get(g.id);
                                      return (
                                        <tr key={g.id}>
                                          <td><span className="goal-category-badge">{t(GOAL_CATEGORY_KEY[g.goalCategory] ?? g.goalCategory)}</span></td>
                                          <td>{g.itSkillName ?? g.qualificationName ?? g.adSeminarName ?? g.customName ?? '—'}</td>
                                          <td>{g.targetPeriod?.slice(0, 7) ?? '—'}</td>
                                          <td>{g.reason || '—'}</td>
                                          <td>{review?.achievementStatus ? t(ACHIEVEMENT_KEY[review.achievementStatus] ?? review.achievementStatus) : '—'}</td>
                                          <td>{review?.reviewNote || '—'}</td>
                                        </tr>
                                      );
                                    })}
                                  </tbody>
                                </table>
                              </StickyHorizontalScroll>
                            )}
                          </div>
                        )}
                        {goals.length > 0 && (
                          <div className="history-goal-section">
                            <h3 className="history-goal-title">{t('historyPage.goalSection.currentYear')}</h3>
                            {filteredGoals.length === 0 ? (
                              <p className="no-data">{t('historyPage.noDataCell.noMatchGoals')}</p>
                            ) : (
                              <StickyHorizontalScroll className="master-table-wrap">
                                <table className="master-table">
                                  <thead>
                                    <tr>
                                      <th style={{ width: 80 }}>{t('historyPage.table.goalCategory')}</th>
                                      <th>{t('historyPage.table.goalName')}</th>
                                      <th style={{ width: 120 }}>{t('historyPage.table.targetPeriod')}</th>
                                      <th>{t('historyPage.table.reasonPlan')}</th>
                                      <th style={{ width: 90 }}>{t('historyPage.table.achievementStatus')}</th>
                                      <th>{t('historyPage.table.reviewNote')}</th>
                                    </tr>
                                  </thead>
                                  <tbody>
                                    {filteredGoals.map(g => {
                                      const review = goalReviewMap.get(g.id);
                                      return (
                                        <tr key={g.id}>
                                          <td><span className="goal-category-badge">{t(GOAL_CATEGORY_KEY[g.goalCategory] ?? g.goalCategory)}</span></td>
                                          <td>{g.itSkillName ?? g.qualificationName ?? g.adSeminarName ?? g.customName ?? '—'}</td>
                                          <td>{g.targetPeriod?.slice(0, 7) ?? '—'}</td>
                                          <td>{g.reason || '—'}</td>
                                          <td>{review?.achievementStatus ? t(ACHIEVEMENT_KEY[review.achievementStatus] ?? review.achievementStatus) : '—'}</td>
                                          <td>{review?.reviewNote || '—'}</td>
                                        </tr>
                                      );
                                    })}
                                  </tbody>
                                </table>
                              </StickyHorizontalScroll>
                            )}
                          </div>
                        )}
                      </>
                    )}
                  </div>
                )}
              </>
            )}
          </>
        )}
      </main>
    </div>
  );
}
