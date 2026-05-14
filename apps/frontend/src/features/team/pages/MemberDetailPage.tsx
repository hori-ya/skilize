import { useEffect, useState, useMemo, Fragment } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { getMemberInventories } from '../api/userApi';
import {
  getItSkillDetails, getQualificationDetails,
  getSeminarDetails, getGoals, getComparison, getGoalReview,
} from '../../inventory/api/inventoryApi';
import { getItSkills } from '../../../shared/api/masterApi';
import type {
  InventorySummary, ItSkillDetailItem, QualificationDetailItem,
  SeminarDetailItem, GoalItem, ComparisonResponse, GoalReviewItem,
} from '../../inventory/types/index';
import type { ItSkill } from '../../../shared/types/master';
import NavBar from '../../../app/layouts/NavBar';

type TabKey = 'it-skills' | 'qualifications' | 'seminars' | 'goals';

const TAB_LABELS: Record<TabKey, string> = {
  'it-skills': 'ITスキル',
  qualifications: '資格',
  seminars: 'セミナー',
  goals: '目標',
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

export default function MemberDetailPage() {
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const userIdNum = Number(userId);
  const backPath: string = (location.state as { from?: string } | null)?.from ?? '/team';
  const backLabel: string = (location.state as { fromLabel?: string } | null)?.fromLabel ?? 'チーム照会';

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

  useEffect(() => {
    getMemberInventories(userIdNum).then(res => {
      setInventories(res.data);
      if (res.data.length > 0) setSelectedId(res.data[0].id);
    });
  }, [userIdNum]);

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
      setItSkillDetails(itRes.data.items);
      setItSkillMaster(masterRes.data);
      setQualificationDetails(qualRes.data.items);
      setSeminarDetails(semRes.data.items);
      setGoals(goalRes.data.items);
      setComparison(compRes?.data ?? null);

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

    for (const detail of itSkillDetails) {
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
  const itSkillColCount = 3 + (hasPrevYear ? 2 : 0);

  const selectedInventory = inventories.find(i => i.id === selectedId);

  return (
    <div className="team-page">
      <NavBar />
      <main className="team-main">
        <button className="page-back-btn" onClick={() => navigate(backPath)}>← {backLabel}に戻る</button>
        <h1 className="page-title">メンバー詳細照会</h1>
        {selectedInventory && (
          <p className="page-subtitle">読み取り専用表示</p>
        )}

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
                    <div className="comparison-table-wrapper">
                      <table className="comparison-table">
                        <thead>
                          <tr>
                            <th>スキル名</th>
                            {hasPrevYear && <th>前年度</th>}
                            <th>今年度</th>
                            {hasPrevYear && <th>差分</th>}
                            <th>備考</th>
                          </tr>
                        </thead>
                        <tbody>
                          {itSkillDetails.length === 0 ? (
                            <tr>
                              <td colSpan={itSkillColCount} className="no-data-cell">
                                ITスキルデータがありません
                              </td>
                            </tr>
                          ) : (
                            <>
                              {Array.from(itSkillTree.groups.entries()).map(([cat1, items]) => (
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
                                        <td>{detail.remarks || '—'}</td>
                                      </tr>
                                    );
                                  })}
                                </Fragment>
                              ))}
                              {itSkillTree.customItems.length > 0 && (
                                <Fragment key="__custom__">
                                  <tr className="scoring-cat1-row">
                                    <td colSpan={itSkillColCount}>カスタムスキル ※</td>
                                  </tr>
                                  {itSkillTree.customItems.map(detail => (
                                    <tr key={detail.id}>
                                      <td>{detail.customSkillName} ※</td>
                                      {hasPrevYear && <td>—</td>}
                                      <td>—</td>
                                      {hasPrevYear && <td>—</td>}
                                      <td>{detail.remarks || '—'}</td>
                                    </tr>
                                  ))}
                                </Fragment>
                              )}
                            </>
                          )}
                        </tbody>
                      </table>
                    </div>
                  </div>
                )}

                {/* ── 資格タブ ── */}
                {activeTab === 'qualifications' && (
                  <div className="history-tab-content">
                    {qualificationDetails.length === 0 ? (
                      <p className="no-data">資格データがありません</p>
                    ) : (
                      <div className="master-table-wrap">
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
                            {qualificationDetails.map(q => (
                              <tr key={q.id}>
                                <td>{q.qualificationCategoryName ?? '—'}</td>
                                <td>
                                  {q.qualificationName ?? q.customQualificationName ?? '—'}
                                  {q.qualificationId === null && ' ※'}
                                </td>
                                <td>{q.acquiredYearMonth?.slice(0, 7) ?? '—'}</td>
                                <td>{q.remarks || '—'}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                )}

                {/* ── セミナータブ ── */}
                {activeTab === 'seminars' && (
                  <div className="history-tab-content">
                    {seminarDetails.length === 0 ? (
                      <p className="no-data">セミナーデータがありません</p>
                    ) : (
                      <div className="master-table-wrap">
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
                            {seminarDetails.map(s => (
                              <tr key={s.id}>
                                <td>{s.adSeminarId !== null ? 'AD' : 'フリー'}</td>
                                <td>{s.adSeminarId !== null ? (s.adSeminarCategoryName ?? '—') : '—'}</td>
                                <td>{s.adSeminarName ?? s.seminarName ?? '—'}</td>
                                <td>{s.attendedYearMonth?.slice(0, 7) ?? '—'}</td>
                                <td>{s.remarks || '—'}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                )}

                {/* ── 目標タブ ── */}
                {activeTab === 'goals' && (
                  <div className="history-tab-content">
                    {prevGoals.length === 0 && goals.length === 0 ? (
                      <p className="no-data">目標データがありません</p>
                    ) : (
                      <>
                        {prevGoals.length > 0 && (
                          <div className="history-goal-section">
                            <h3 className="history-goal-title">前年度目標</h3>
                            <div className="master-table-wrap">
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
                                  {prevGoals.map(g => {
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
                            </div>
                          </div>
                        )}
                        {goals.length > 0 && (
                          <div className="history-goal-section">
                            <h3 className="history-goal-title">今年度目標</h3>
                            <div className="master-table-wrap">
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
                                  {goals.map(g => {
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
                            </div>
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
