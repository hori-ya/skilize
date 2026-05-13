import { useEffect, useState, useMemo, Fragment } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  getMyInventories, getItSkillDetails, getQualificationDetails,
  getSeminarDetails, getGoals, getComparison, patchItSkillRemarks,
} from '../api/inventory';
import { getItSkills } from '../api/master';
import type {
  InventorySummary, ItSkillDetailItem, QualificationDetailItem,
  SeminarDetailItem, GoalItem, ComparisonResponse,
} from '../types/inventory';
import type { ItSkill } from '../types/master';
import NavBar from '../components/NavBar';

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

  const [editingRemarks, setEditingRemarks] = useState<Record<number, string>>({});
  const [savingId, setSavingId] = useState<number | null>(null);

  useEffect(() => {
    getMyInventories().then(res => {
      setInventories(res.data);
      if (res.data.length > 0) setSelectedId(res.data[0].id);
    });
  }, []);

  useEffect(() => {
    if (!selectedId) return;
    setLoading(true);

    Promise.all([
      getItSkillDetails(selectedId),
      getItSkills(),
      getQualificationDetails(selectedId),
      getSeminarDetails(selectedId),
      getGoals(selectedId),
      getComparison(selectedId).catch(() => null),
    ]).then(([itRes, masterRes, qualRes, semRes, goalRes, compRes]) => {
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
    }).finally(() => setLoading(false));
  }, [selectedId]);

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
                    <div className="comparison-table-wrapper">
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
                                        <td>
                                          {isCurrentYear ? (
                                            <textarea
                                              className="textarea remarks-input"
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
                              {itSkillTree.customItems.length > 0 && (
                                <Fragment key="__custom__">
                                  <tr className="scoring-cat1-row">
                                    <td colSpan={itSkillColCount}>カスタムスキル ※</td>
                                  </tr>
                                  {itSkillTree.customItems.map(detail => (
                                    <tr key={detail.id}>
                                      <td>{detail.customSkillName} ※</td>
                                      {hasPrevYear && <td>—</td>}
                                      <td>{detail.levelValue}</td>
                                      {hasPrevYear && <td>—</td>}
                                      <td>
                                        {isCurrentYear ? (
                                          <textarea
                                            className="textarea remarks-input"
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
                                <td>{s.adSeminarCategoryName ?? s.seminarCategoryName ?? '—'}</td>
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
                    {goals.length === 0 ? (
                      <p className="no-data">目標データがありません</p>
                    ) : (
                      (['IT_SKILL', 'QUALIFICATION', 'AD'] as const).map(category => {
                        const categoryGoals = goals.filter(g => g.goalCategory === category);
                        if (categoryGoals.length === 0) return null;
                        return (
                          <div key={category} className="history-goal-section">
                            <h3 className="history-goal-title">{GOAL_CATEGORY_LABEL[category]}</h3>
                            <div className="master-table-wrap">
                              <table className="master-table">
                                <thead>
                                  <tr>
                                    <th>目標名</th>
                                    <th>達成予定時期</th>
                                    <th>理由・計画</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  {categoryGoals.map(g => (
                                    <tr key={g.id}>
                                      <td>
                                        {g.itSkillName ?? g.qualificationName ?? g.adSeminarName ?? g.customName ?? '—'}
                                      </td>
                                      <td>{g.targetPeriod?.slice(0, 7) ?? '—'}</td>
                                      <td>{g.reason || '—'}</td>
                                    </tr>
                                  ))}
                                </tbody>
                              </table>
                            </div>
                          </div>
                        );
                      })
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
