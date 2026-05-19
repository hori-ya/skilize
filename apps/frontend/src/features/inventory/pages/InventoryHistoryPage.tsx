import { useEffect, useState, useMemo, Fragment } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  getMyInventories, getItSkillDetails, getQualificationDetails,
  getSeminarDetails, getGoals, getComparison, patchItSkillRemarks, getGoalReview,
  getMyAiAnalyses,
} from '../api/inventoryApi';
import { getItSkills } from '../../../shared/api/masterApi';
import type {
  InventorySummary, ItSkillDetailItem, QualificationDetailItem,
  SeminarDetailItem, GoalItem, ComparisonResponse, GoalReviewItem, AiAnalysis,
} from '../types/index';
import type { ItSkill } from '../../../shared/types/master';
import NavBar from '../../../app/layouts/NavBar';
import AiAnalysisCard from '../components/AiAnalysisCard';
import StickyHorizontalScroll from '../../../shared/ui/StickyHorizontalScroll';

type TabKey = 'it-skills' | 'qualifications' | 'seminars' | 'goals' | 'ai-analysis';

const TAB_LABELS: Record<TabKey, string> = {
  'it-skills': 'ITスキル',
  qualifications: '資格',
  seminars: 'セミナー',
  goals: '目標',
  'ai-analysis': 'AI分析',
};

const GOAL_CATEGORY_LABEL: Record<string, string> = {
  IT_SKILL: 'ITスキル',
  QUALIFICATION: '資格',
  AD: 'AD',
};

const STATUS_LABEL: Record<string, string> = {
  DRAFT: '入力中',
  PENDING_GOAL: '提出済み・目標未設定',
  COMPLETED: '完了',
};

const ACHIEVEMENT_LABEL: Record<string, string> = {
  ACHIEVED: '達成',
  PARTIAL: '一部達成',
  NOT_ACHIEVED: '未達成',
};

function DiffCell({ diff, hasPrevYear }: { diff: number | null | undefined; hasPrevYear: boolean }) {
  if (!hasPrevYear) return null;
  if (diff === null || diff === undefined) return <span className="diff-new">新規</span>;
  if (diff > 0) return <span className="diff-up">↑ +{diff}</span>;
  if (diff < 0) return <span className="diff-down">↓ {diff}</span>;
  return <span>—</span>;
}

export default function InventoryHistoryPage() {
  const navigate = useNavigate();

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

  // Filter states
  const [itSkillSearch, setItSkillSearch] = useState('');
  const [itSkillCategory1Filter, setItSkillCategory1Filter] = useState('');
  const [itSkillDiffFilter, setItSkillDiffFilter] = useState<'' | 'up' | 'down' | 'new'>('');
  const [qualSearch, setQualSearch] = useState('');
  const [seminarSearch, setSeminarSearch] = useState('');
  const [seminarTypeFilter, setSeminarTypeFilter] = useState<'' | 'AD' | 'FREE'>('');
  const [goalCategoryFilter, setGoalCategoryFilter] = useState<'' | 'IT_SKILL' | 'QUALIFICATION' | 'AD'>('');
  const [goalSearch, setGoalSearch] = useState('');

  useEffect(() => {
    getMyInventories().then(res => {
      setInventories(res.data);
      if (res.data.length > 0) setSelectedId(res.data[0].id);
    });
    getMyAiAnalyses().then(res => setAiAnalyses(res.data)).catch(() => {});
  }, []);

  useEffect(() => {
    setItSkillSearch('');
    setItSkillCategory1Filter('');
    setItSkillDiffFilter('');
    setQualSearch('');
    setSeminarSearch('');
    setSeminarTypeFilter('');
    setGoalCategoryFilter('');
    setGoalSearch('');
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
    const groups = new Map<string, ItSkillDetailItem[]>();
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
        if (!groups.has(cat1)) groups.set(cat1, []);
        groups.get(cat1)!.push(detail);
      }
    }

    return { groups, customItems };
  }, [itSkillDetails, itSkillMaster]);

  const comparisonMap = useMemo(() => {
    if (!comparison) return new Map<number, { prevLevelValue: number | null; diff: number | null }>();
    return new Map(comparison.items.map(item => [item.currentDetailId, item]));
  }, [comparison]);

  const hasPrevYear = comparison?.hasPrevYear ?? false;

  const filteredItSkillTree = useMemo(() => {
    const searchLower = itSkillSearch.toLowerCase();
    const filteredGroups = new Map<string, ItSkillDetailItem[]>();
    for (const [cat1, items] of itSkillTree.groups.entries()) {
      if (itSkillCategory1Filter && cat1 !== itSkillCategory1Filter) continue;
      const filtered = items.filter(item => {
        if (searchLower && !item.itSkillName?.toLowerCase().includes(searchLower)) return false;
        if (itSkillDiffFilter) {
          const comp = comparisonMap.get(item.id);
          if (itSkillDiffFilter === 'new' && comp !== undefined) return false;
          if (itSkillDiffFilter === 'up' && (comp === undefined || (comp.diff ?? 0) <= 0)) return false;
          if (itSkillDiffFilter === 'down' && (comp === undefined || (comp.diff ?? 0) >= 0)) return false;
        }
        return true;
      });
      if (filtered.length > 0) filteredGroups.set(cat1, filtered);
    }
    const filteredCustom = itSkillCategory1Filter ? [] : itSkillTree.customItems.filter(item =>
      !searchLower || item.customSkillName?.toLowerCase().includes(searchLower)
    );
    return { groups: filteredGroups, customItems: filteredCustom };
  }, [itSkillTree, itSkillSearch, itSkillCategory1Filter, itSkillDiffFilter, comparisonMap]);

  const filteredItSkillCount = useMemo(() => {
    let count = 0;
    for (const items of filteredItSkillTree.groups.values()) count += items.length;
    return count + filteredItSkillTree.customItems.length;
  }, [filteredItSkillTree]);

  const filteredQualifications = useMemo(() => {
    const searchLower = qualSearch.toLowerCase();
    if (!searchLower) return qualificationDetails;
    return qualificationDetails.filter(q => {
      const name = (q.qualificationName ?? q.customQualificationName ?? '').toLowerCase();
      const cat = (q.qualificationCategoryName ?? '').toLowerCase();
      return name.includes(searchLower) || cat.includes(searchLower);
    });
  }, [qualificationDetails, qualSearch]);

  const filteredSeminars = useMemo(() => {
    const searchLower = seminarSearch.toLowerCase();
    return seminarDetails.filter(s => {
      if (seminarTypeFilter === 'AD' && s.adSeminarId === null) return false;
      if (seminarTypeFilter === 'FREE' && s.adSeminarId !== null) return false;
      if (searchLower) {
        const name = (s.adSeminarName ?? s.seminarName ?? '').toLowerCase();
        const cat = (s.adSeminarCategoryName ?? '').toLowerCase();
        if (!name.includes(searchLower) && !cat.includes(searchLower)) return false;
      }
      return true;
    });
  }, [seminarDetails, seminarSearch, seminarTypeFilter]);

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
        <button className="page-back-btn" onClick={() => navigate('/')}>← ダッシュボードに戻る</button>
        <h1 className="page-title">棚卸照会</h1>

        {inventories.length === 0 ? (
          <div className="info-card"><p>棚卸データがありません。</p></div>
        ) : (
          <>
            <div className="history-selector-row">
              <label className="form-label">年度</label>
              <select
                className="select history-year-select"
                value={selectedId ?? ''}
                onChange={e => setSelectedId(Number(e.target.value))}
              >
                {inventories.map(inv => (
                  <option key={inv.id} value={inv.id}>
                    {inv.fiscalYear.name}（{STATUS_LABEL[inv.status] ?? inv.status}）
                  </option>
                ))}
              </select>
            </div>

            {loading ? (
              <div className="loading">読み込み中...</div>
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
                          placeholder="スキル名で検索"
                          value={itSkillSearch}
                          onChange={e => setItSkillSearch(e.target.value)}
                        />
                        <select
                          className="history-filter-bar__select"
                          value={itSkillCategory1Filter}
                          onChange={e => setItSkillCategory1Filter(e.target.value)}
                        >
                          <option value="">大分類：すべて</option>
                          {Array.from(itSkillTree.groups.keys()).map(cat1 => (
                            <option key={cat1} value={cat1}>{cat1}</option>
                          ))}
                        </select>
                        {hasPrevYear && (
                          <select
                            className="history-filter-bar__select"
                            value={itSkillDiffFilter}
                            onChange={e => setItSkillDiffFilter(e.target.value as '' | 'up' | 'down' | 'new')}
                          >
                            <option value="">差分：すべて</option>
                            <option value="up">上昇</option>
                            <option value="down">下降</option>
                            <option value="new">新規</option>
                          </select>
                        )}
                        <span className="history-result-count">{filteredItSkillCount}件</span>
                      </div>
                    )}
                    <StickyHorizontalScroll className="comparison-table-wrapper">
                      <table className="comparison-table">
                        <thead>
                          <tr>
                            <th>スキル名</th>
                            {hasPrevYear && <th>前年度</th>}
                            <th>今年度</th>
                            {hasPrevYear && <th>差分</th>}
                            <th>備考</th>
                            {isCurrentYear && <th></th>}
                          </tr>
                        </thead>
                        <tbody>
                          {itSkillDetails.length === 0 ? (
                            <tr>
                              <td colSpan={itSkillColCount} className="no-data-cell">
                                ITスキルデータがありません
                              </td>
                            </tr>
                          ) : filteredItSkillCount === 0 ? (
                            <tr>
                              <td colSpan={itSkillColCount} className="no-data-cell">
                                条件に一致するスキルがありません
                              </td>
                            </tr>
                          ) : (
                            <>
                              {Array.from(filteredItSkillTree.groups.entries()).map(([cat1, items]) => (
                                <Fragment key={cat1}>
                                  <tr className="scoring-cat1-row">
                                    <td colSpan={itSkillColCount}>{cat1}</td>
                                  </tr>
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
                                              {savingId === detail.id ? '...' : '保存'}
                                            </button>
                                          </td>
                                        )}
                                      </tr>
                                    );
                                  })}
                                </Fragment>
                              ))}
                              {filteredItSkillTree.customItems.length > 0 && (
                                <Fragment key="__custom__">
                                  <tr className="scoring-cat1-row">
                                    <td colSpan={itSkillColCount}>カスタムスキル ※</td>
                                  </tr>
                                  {filteredItSkillTree.customItems.map(detail => (
                                    <tr key={detail.id}>
                                      <td>{detail.customSkillName} ※</td>
                                      {hasPrevYear && <td>—</td>}
                                      <td>—</td>
                                      {hasPrevYear && <td>—</td>}
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
                                            {savingId === detail.id ? '...' : '保存'}
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
                      <p className="no-data">資格データがありません</p>
                    ) : (
                      <>
                        <div className="history-filter-bar">
                          <input
                            className="history-filter-bar__input"
                            placeholder="資格名・分類で検索"
                            value={qualSearch}
                            onChange={e => setQualSearch(e.target.value)}
                          />
                          <span className="history-result-count">{filteredQualifications.length}件</span>
                        </div>
                        <StickyHorizontalScroll className="master-table-wrap">
                          <table className="master-table">
                            <thead>
                              <tr>
                                <th>分類</th>
                                <th>資格名</th>
                                <th>取得年月</th>
                                <th>備考</th>
                              </tr>
                            </thead>
                            <tbody>
                              {filteredQualifications.length === 0 ? (
                                <tr><td colSpan={4} className="no-data-cell">条件に一致する資格がありません</td></tr>
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
                      <p className="no-data">セミナーデータがありません</p>
                    ) : (
                      <>
                        <div className="history-filter-bar">
                          <input
                            className="history-filter-bar__input"
                            placeholder="セミナー名・分類で検索"
                            value={seminarSearch}
                            onChange={e => setSeminarSearch(e.target.value)}
                          />
                          <select
                            className="history-filter-bar__select"
                            value={seminarTypeFilter}
                            onChange={e => setSeminarTypeFilter(e.target.value as '' | 'AD' | 'FREE')}
                          >
                            <option value="">区分：すべて</option>
                            <option value="AD">AD</option>
                            <option value="FREE">フリー</option>
                          </select>
                          <span className="history-result-count">{filteredSeminars.length}件</span>
                        </div>
                        <StickyHorizontalScroll className="master-table-wrap">
                          <table className="master-table">
                            <thead>
                              <tr>
                                <th>区分</th>
                                <th>分類</th>
                                <th>セミナー名</th>
                                <th>受講年月</th>
                                <th>備考</th>
                              </tr>
                            </thead>
                            <tbody>
                              {filteredSeminars.length === 0 ? (
                                <tr><td colSpan={5} className="no-data-cell">条件に一致するセミナーがありません</td></tr>
                              ) : (
                                filteredSeminars.map(s => (
                                  <tr key={s.id}>
                                    <td>{s.adSeminarId !== null ? 'AD' : 'フリー'}</td>
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
                      if (!analysis) return <p className="no-data">この年度のAI分析データはありません。目標設定完了後に自動生成されます。</p>;
                      return <AiAnalysisCard analysis={analysis} />;
                    })()}
                  </div>
                )}

                {/* ── 目標タブ ── */}
                {activeTab === 'goals' && (
                  <div className="history-tab-content">
                    {prevGoals.length === 0 && goals.length === 0 ? (
                      <p className="no-data">目標データがありません</p>
                    ) : (
                      <>
                        <div className="history-filter-bar">
                          <select
                            className="history-filter-bar__select"
                            value={goalCategoryFilter}
                            onChange={e => setGoalCategoryFilter(e.target.value as '' | 'IT_SKILL' | 'QUALIFICATION' | 'AD')}
                          >
                            <option value="">カテゴリ：すべて</option>
                            <option value="IT_SKILL">ITスキル</option>
                            <option value="QUALIFICATION">資格</option>
                            <option value="AD">AD</option>
                          </select>
                          <input
                            className="history-filter-bar__input"
                            placeholder="目標名で検索"
                            value={goalSearch}
                            onChange={e => setGoalSearch(e.target.value)}
                          />
                          <span className="history-result-count">
                            {filteredPrevGoals.length + filteredGoals.length}件
                          </span>
                        </div>
                        {prevGoals.length > 0 && (
                          <div className="history-goal-section">
                            <h3 className="history-goal-title">前年度目標</h3>
                            {filteredPrevGoals.length === 0 ? (
                              <p className="no-data">条件に一致する目標がありません</p>
                            ) : (
                              <StickyHorizontalScroll className="master-table-wrap">
                                <table className="master-table">
                                  <thead>
                                    <tr>
                                      <th style={{ width: 80 }}>カテゴリ</th>
                                      <th>目標名</th>
                                      <th style={{ width: 120 }}>達成予定時期</th>
                                      <th>理由・計画</th>
                                      <th style={{ width: 90 }}>達成状況</th>
                                      <th>振り返りコメント</th>
                                    </tr>
                                  </thead>
                                  <tbody>
                                    {filteredPrevGoals.map(g => {
                                      const review = prevGoalReviewMap.get(g.id);
                                      return (
                                        <tr key={g.id}>
                                          <td><span className="goal-category-badge">{GOAL_CATEGORY_LABEL[g.goalCategory]}</span></td>
                                          <td>{g.itSkillName ?? g.qualificationName ?? g.adSeminarName ?? g.customName ?? '—'}</td>
                                          <td>{g.targetPeriod?.slice(0, 7) ?? '—'}</td>
                                          <td>{g.reason || '—'}</td>
                                          <td>{review?.achievementStatus ? (ACHIEVEMENT_LABEL[review.achievementStatus] ?? review.achievementStatus) : '—'}</td>
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
                            <h3 className="history-goal-title">今年度目標</h3>
                            {filteredGoals.length === 0 ? (
                              <p className="no-data">条件に一致する目標がありません</p>
                            ) : (
                              <StickyHorizontalScroll className="master-table-wrap">
                                <table className="master-table">
                                  <thead>
                                    <tr>
                                      <th style={{ width: 80 }}>カテゴリ</th>
                                      <th>目標名</th>
                                      <th style={{ width: 120 }}>達成予定時期</th>
                                      <th>理由・計画</th>
                                      <th style={{ width: 90 }}>達成状況</th>
                                      <th>振り返りコメント</th>
                                    </tr>
                                  </thead>
                                  <tbody>
                                    {filteredGoals.map(g => {
                                      const review = goalReviewMap.get(g.id);
                                      return (
                                        <tr key={g.id}>
                                          <td><span className="goal-category-badge">{GOAL_CATEGORY_LABEL[g.goalCategory]}</span></td>
                                          <td>{g.itSkillName ?? g.qualificationName ?? g.adSeminarName ?? g.customName ?? '—'}</td>
                                          <td>{g.targetPeriod?.slice(0, 7) ?? '—'}</td>
                                          <td>{g.reason || '—'}</td>
                                          <td>{review?.achievementStatus ? (ACHIEVEMENT_LABEL[review.achievementStatus] ?? review.achievementStatus) : '—'}</td>
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
