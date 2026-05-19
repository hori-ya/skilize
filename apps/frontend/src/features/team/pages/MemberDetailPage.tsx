import { useEffect, useRef, useState, useMemo, Fragment } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { getMemberInventories, getExpectations, saveTlExpectation, saveCompanyExpectation, getMemberAiAnalyses } from '../api/userApi';
import {
  getItSkillDetails, getQualificationDetails,
  getSeminarDetails, getGoals, getComparison, getGoalReview,
} from '../../inventory/api/inventoryApi';
import { getItSkills } from '../../../shared/api/masterApi';
import { getInterview, saveInterview, getPrevYearInterview } from '../../interview/api/interviewApi';
import type {
  InventorySummary, ItSkillDetailItem, QualificationDetailItem,
  SeminarDetailItem, GoalItem, ComparisonResponse, GoalReviewItem, AiAnalysis,
} from '../../inventory/types/index';
import type { ItSkill } from '../../../shared/types/master';
import type { InterviewMemo, DetailType } from '../../interview/types';
import type { UserExpectation } from '../types/index';
import { useAuth } from '../../../app/providers/AuthProvider';
import NavBar from '../../../app/layouts/NavBar';
import AiAnalysisCard from '../../inventory/components/AiAnalysisCard';
import StickyHorizontalScroll from '../../../shared/ui/StickyHorizontalScroll';

type TabKey = 'it-skills' | 'qualifications' | 'seminars' | 'goals' | 'expectations' | 'ai-analysis';

const TAB_LABELS: Record<TabKey, string> = {
  'it-skills': 'ITスキル',
  qualifications: '資格',
  seminars: 'セミナー',
  goals: '目標',
  expectations: '期待',
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

function buildNoteKey(detailType: DetailType, detailId: number): string {
  return `${detailType}|${detailId}`;
}

export default function MemberDetailPage() {
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();
  const userIdNum = Number(userId);
  const backPath: string = (location.state as { from?: string } | null)?.from ?? '/team';
  const backLabel: string = (location.state as { fromLabel?: string } | null)?.fromLabel ?? 'チーム照会';

  const isTlOrAdmin = user?.role === 'TL' || user?.role === 'ADMIN';

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

  // Interview memo state
  const [interview, setInterview] = useState<InterviewMemo | null>(null);
  const [prevYearInterview, setPrevYearInterview] = useState<InterviewMemo | null>(null);
  const [generalNote, setGeneralNote] = useState('');
  const [detailNotes, setDetailNotes] = useState<Map<string, string>>(new Map());
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [panelOpen, setPanelOpen] = useState(false);
  const [panelRect, setPanelRect] = useState(() => ({
    left: window.innerWidth - 24 - 320,
    top: window.innerHeight - 72 - 280,
    width: 320,
    height: 280,
  }));

  // AI分析 state
  const [memberAiAnalyses, setMemberAiAnalyses] = useState<AiAnalysis[]>([]);
  const [aiAnalysisLoaded, setAiAnalysisLoaded] = useState(false);

  // 期待コメント state
  const [expectation, setExpectation] = useState<UserExpectation | null>(null);
  const [expLoaded, setExpLoaded] = useState(false);
  const [expLoading, setExpLoading] = useState(false);
  const [editTl, setEditTl] = useState('');
  const [editCompany, setEditCompany] = useState('');
  const [tlSaving, setTlSaving] = useState(false);
  const [companySaving, setCompanySaving] = useState(false);
  const [tlSaved, setTlSaved] = useState(false);
  const [companySaved, setCompanySaved] = useState(false);
  const [tlSaveError, setTlSaveError] = useState<string | null>(null);
  const [companySaveError, setCompanySaveError] = useState<string | null>(null);

  // ドラッグ中のリスナー管理
  const dragRef = useRef<{ onMove: (e: MouseEvent) => void; onUp: () => void } | null>(null);

  useEffect(() => {
    return () => {
      if (dragRef.current) {
        document.removeEventListener('mousemove', dragRef.current.onMove);
        document.removeEventListener('mouseup', dragRef.current.onUp);
      }
    };
  }, []);

  function attachDrag(handleMove: (e: MouseEvent) => void) {
    if (dragRef.current) {
      document.removeEventListener('mousemove', dragRef.current.onMove);
      document.removeEventListener('mouseup', dragRef.current.onUp);
    }
    const onUp = () => {
      document.removeEventListener('mousemove', handleMove);
      document.removeEventListener('mouseup', onUp);
      dragRef.current = null;
    };
    dragRef.current = { onMove: handleMove, onUp };
    document.addEventListener('mousemove', handleMove);
    document.addEventListener('mouseup', onUp);
  }

  function startMove(e: React.MouseEvent) {
    if ((e.target as HTMLElement).closest('.interview-float-panel__close')) return;
    e.preventDefault();
    const sx = e.clientX, sy = e.clientY;
    const sl = panelRect.left, st = panelRect.top;
    attachDrag((ev) => {
      setPanelRect(prev => ({
        ...prev,
        left: Math.max(0, Math.min(window.innerWidth - prev.width, sl + ev.clientX - sx)),
        top: Math.max(0, Math.min(window.innerHeight - prev.height, st + ev.clientY - sy)),
      }));
    });
  }

  function startResize(e: React.MouseEvent, dir: string) {
    e.preventDefault();
    e.stopPropagation();
    const sx = e.clientX, sy = e.clientY;
    const { left, top, width, height } = panelRect;
    attachDrag((ev) => {
      const dx = ev.clientX - sx, dy = ev.clientY - sy;
      let nl = left, nt = top, nw = width, nh = height;
      if (dir.includes('e')) {
        nw = Math.min(Math.max(240, width + dx), window.innerWidth - nl);
      }
      if (dir.includes('s')) {
        nh = Math.min(Math.max(200, height + dy), window.innerHeight - nt);
      }
      if (dir.includes('w')) {
        nw = Math.max(240, width - dx);
        nl = left + width - nw;
        if (nl < 0) { nl = 0; nw = left + width; }
      }
      if (dir.includes('n')) {
        nh = Math.max(200, height - dy);
        nt = top + height - nh;
        if (nt < 0) { nt = 0; nh = top + height; }
      }
      setPanelRect({ left: nl, top: nt, width: nw, height: nh });
    });
  }

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

    const interviewPromise = isTlOrAdmin
      ? getInterview(selectedId).catch(() => null)
      : Promise.resolve(null);
    const prevYearInterviewPromise = isTlOrAdmin
      ? getPrevYearInterview(selectedId).catch(() => null)
      : Promise.resolve(null);

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
      interviewPromise,
      prevYearInterviewPromise,
    ]).then(([itRes, masterRes, qualRes, semRes, goalRes, compRes, reviewRes, prevGoalsRes, prevReviewRes, interviewRes, prevYearRes]) => {
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

      // Initialize interview memo state
      const memo = interviewRes?.data ?? null;
      setInterview(memo);
      setGeneralNote(memo?.generalNote ?? '');
      const noteMap = new Map<string, string>();
      if (memo) {
        for (const n of memo.detailNotes) {
          noteMap.set(buildNoteKey(n.detailType, n.detailId), n.note);
        }
      }
      setDetailNotes(noteMap);
      setSaveError(null);

      setPrevYearInterview(prevYearRes?.data ?? null);
    }).finally(() => setLoading(false));
  }, [selectedId, inventories, isTlOrAdmin]);

  useEffect(() => {
    if (activeTab !== 'ai-analysis' || aiAnalysisLoaded || !isTlOrAdmin) return;
    getMemberAiAnalyses(userIdNum)
      .then(res => setMemberAiAnalyses(res.data))
      .catch(() => {})
      .finally(() => setAiAnalysisLoaded(true));
  }, [activeTab, aiAnalysisLoaded, isTlOrAdmin, userIdNum]);

  useEffect(() => {
    if (activeTab !== 'expectations' || expLoaded || !isTlOrAdmin) return;
    setExpLoading(true);
    getExpectations(userIdNum)
      .then(res => {
        setExpectation(res.data);
        setEditTl(res.data.tlExpectation ?? '');
        setEditCompany(res.data.companyExpectation ?? '');
      })
      .catch(() => {
        setExpectation({ tlExpectation: null, companyExpectation: null });
      })
      .finally(() => {
        setExpLoaded(true);
        setExpLoading(false);
      });
  }, [activeTab, expLoaded, isTlOrAdmin, userIdNum]);

  async function handleSaveTl() {
    setTlSaving(true);
    setTlSaveError(null);
    setTlSaved(false);
    try {
      const res = await saveTlExpectation(userIdNum, editTl);
      setExpectation(res.data);
      setTlSaved(true);
    } catch {
      setTlSaveError('保存に失敗しました');
    } finally {
      setTlSaving(false);
    }
  }

  async function handleSaveCompany() {
    setCompanySaving(true);
    setCompanySaveError(null);
    setCompanySaved(false);
    try {
      const res = await saveCompanyExpectation(userIdNum, editCompany);
      setExpectation(res.data);
      setCompanySaved(true);
    } catch {
      setCompanySaveError('保存に失敗しました');
    } finally {
      setCompanySaving(false);
    }
  }

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

  const prevYearNoteMap = useMemo(() => {
    const m = new Map<string, string>();
    if (prevYearInterview) {
      for (const n of prevYearInterview.detailNotes) {
        m.set(buildNoteKey(n.detailType, n.detailId), n.note);
      }
    }
    return m;
  }, [prevYearInterview]);

  const hasPrevYear = comparison?.hasPrevYear ?? false;
  const itSkillColCount = 3 + (hasPrevYear ? 2 : 0) + (isTlOrAdmin ? 1 : 0);

  const selectedInventory = inventories.find(i => i.id === selectedId);

  function goalDetailId(g: GoalItem): number {
    if (g.goalCategory === 'IT_SKILL' && g.itSkillId != null) return g.itSkillId;
    if (g.goalCategory === 'QUALIFICATION' && g.qualificationId != null) return g.qualificationId;
    return g.id;
  }

  function setDetailNote(detailType: DetailType, detailId: number, value: string) {
    setDetailNotes(prev => {
      const next = new Map(prev);
      if (value) {
        next.set(buildNoteKey(detailType, detailId), value);
      } else {
        next.delete(buildNoteKey(detailType, detailId));
      }
      return next;
    });
  }

  async function handleSave() {
    if (!selectedId) return;
    setSaving(true);
    setSaveError(null);
    try {
      const notes = Array.from(detailNotes.entries())
        .map(([key, note]) => {
          const sep = key.lastIndexOf('|');
          const type = key.slice(0, sep) as DetailType;
          const id = Number(key.slice(sep + 1));
          return { detailType: type, detailId: id, note };
        });
      const res = await saveInterview(selectedId, {
        generalNote: generalNote || null,
        detailNotes: notes,
      });
      setInterview(res.data);
    } catch {
      setSaveError('保存に失敗しました');
    } finally {
      setSaving(false);
    }
  }

  function renderDetailNoteCell(detailType: DetailType, detailId: number, showPrev = true) {
    const key = buildNoteKey(detailType, detailId);
    const prevNote = showPrev ? prevYearNoteMap.get(key) : undefined;
    return (
      <td className="interview-note-cell">
        <textarea
          className="interview-note-textarea"
          value={detailNotes.get(key) ?? ''}
          onChange={e => setDetailNote(detailType, detailId, e.target.value)}
          placeholder="メモ"
          rows={1}
        />
        {prevNote && (
          <div className="interview-note-prev-inline">前年: {prevNote}</div>
        )}
      </td>
    );
  }

  function renderPrevGoalNoteCell(g: GoalItem) {
    const note = prevYearNoteMap.get(buildNoteKey('GOAL', goalDetailId(g)));
    return (
      <td className="interview-note-cell">
        {note
          ? <div className="interview-note-prev-readonly">{note}</div>
          : <span className="interview-note-empty">—</span>}
      </td>
    );
  }

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
                  {(Object.keys(TAB_LABELS) as TabKey[])
                    .filter(tab => (tab !== 'expectations' && tab !== 'ai-analysis') || isTlOrAdmin)
                    .map(tab => (
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
                    <StickyHorizontalScroll className="comparison-table-wrapper">
                      <table className="comparison-table">
                        <thead>
                          <tr>
                            <th>スキル名</th>
                            {hasPrevYear && <th>前年度</th>}
                            <th>今年度</th>
                            {hasPrevYear && <th>差分</th>}
                            <th>備考</th>
                            {isTlOrAdmin && <th className="interview-note-th">面談メモ</th>}
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
                                        {isTlOrAdmin && renderDetailNoteCell('IT_SKILL', detail.itSkillId ?? detail.id)}
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
                                      {isTlOrAdmin && renderDetailNoteCell('IT_SKILL', detail.id)}
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
                      <StickyHorizontalScroll className="master-table-wrap">
                        <table className="master-table">
                          <thead>
                            <tr>
                              <th>分類</th>
                              <th>資格名</th>
                              <th>取得年月</th>
                              <th>備考</th>
                              {isTlOrAdmin && <th className="interview-note-th">面談メモ</th>}
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
                                {isTlOrAdmin && renderDetailNoteCell('QUALIFICATION', q.qualificationId ?? q.id)}
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </StickyHorizontalScroll>
                    )}
                  </div>
                )}

                {/* ── セミナータブ ── */}
                {activeTab === 'seminars' && (
                  <div className="history-tab-content">
                    {seminarDetails.length === 0 ? (
                      <p className="no-data">セミナーデータがありません</p>
                    ) : (
                      <StickyHorizontalScroll className="master-table-wrap">
                        <table className="master-table">
                          <thead>
                            <tr>
                              <th>区分</th>
                              <th>分類</th>
                              <th>セミナー名</th>
                              <th>受講年月</th>
                              <th>備考</th>
                              {isTlOrAdmin && <th className="interview-note-th">面談メモ</th>}
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
                                {isTlOrAdmin && renderDetailNoteCell('SEMINAR', s.id)}
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </StickyHorizontalScroll>
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
                                    {isTlOrAdmin && <th className="interview-note-th">面談メモ（前年度）</th>}
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
                                        {isTlOrAdmin && renderPrevGoalNoteCell(g)}
                                      </tr>
                                    );
                                  })}
                                </tbody>
                              </table>
                            </StickyHorizontalScroll>
                          </div>
                        )}
                        {goals.length > 0 && (
                          <div className="history-goal-section">
                            <h3 className="history-goal-title">今年度目標</h3>
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
                                    {isTlOrAdmin && <th className="interview-note-th">面談メモ</th>}
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
                                        {isTlOrAdmin && renderDetailNoteCell('GOAL', goalDetailId(g), false)}
                                      </tr>
                                    );
                                  })}
                                </tbody>
                              </table>
                            </StickyHorizontalScroll>
                          </div>
                        )}
                      </>
                    )}
                  </div>
                )}

                {/* ── AI分析タブ ── */}
                {activeTab === 'ai-analysis' && isTlOrAdmin && (
                  <div className="history-tab-content">
                    {!aiAnalysisLoaded ? (
                      <div className="loading">読み込み中...</div>
                    ) : (() => {
                      const analysis = memberAiAnalyses.find(a => {
                        const inv = inventories.find(i => i.id === selectedId);
                        return inv && a.fiscalYearId === inv.fiscalYear.id;
                      });
                      if (!analysis) return <p className="no-data">この年度のAI分析データはありません。</p>;
                      return <AiAnalysisCard analysis={analysis} />;
                    })()}
                  </div>
                )}

                {/* ── 期待タブ ── */}
                {activeTab === 'expectations' && isTlOrAdmin && (
                  <div className="history-tab-content">
                    {expLoading ? (
                      <div className="loading">読み込み中...</div>
                    ) : (
                      <div className="expectation-detail-panel">
                        {/* TLが期待すること */}
                        <div className="expectation-detail-section">
                          <h3 className="expectation-detail-title">TLが期待すること</h3>
                          {user?.role === 'TL' ? (
                            <>
                              <textarea
                                className="expectation-detail-textarea"
                                value={editTl}
                                onChange={e => { setEditTl(e.target.value); setTlSaved(false); }}
                                placeholder="このメンバーへの期待を入力してください"
                                rows={6}
                              />
                              <div className="expectation-detail-save-row">
                                {tlSaved && !tlSaveError && (
                                  <span className="expectation-saved-label">保存しました</span>
                                )}
                                {tlSaveError && <span className="error-text">{tlSaveError}</span>}
                                <button
                                  className="btn btn-submit expectation-save-btn"
                                  onClick={handleSaveTl}
                                  disabled={tlSaving}
                                >
                                  {tlSaving ? '保存中...' : '保存'}
                                </button>
                              </div>
                            </>
                          ) : (
                            <div className="expectation-detail-readonly">
                              {expectation?.tlExpectation || '（未入力）'}
                            </div>
                          )}
                        </div>

                        {/* 会社が期待すること */}
                        <div className="expectation-detail-section">
                          <h3 className="expectation-detail-title">会社が期待すること</h3>
                          {user?.role === 'ADMIN' ? (
                            <>
                              <textarea
                                className="expectation-detail-textarea"
                                value={editCompany}
                                onChange={e => { setEditCompany(e.target.value); setCompanySaved(false); }}
                                placeholder="会社としての期待を入力してください"
                                rows={6}
                              />
                              <div className="expectation-detail-save-row">
                                {companySaved && !companySaveError && (
                                  <span className="expectation-saved-label">保存しました</span>
                                )}
                                {companySaveError && <span className="error-text">{companySaveError}</span>}
                                <button
                                  className="btn btn-submit expectation-save-btn"
                                  onClick={handleSaveCompany}
                                  disabled={companySaving}
                                >
                                  {companySaving ? '保存中...' : '保存'}
                                </button>
                              </div>
                            </>
                          ) : (
                            <div className="expectation-detail-readonly">
                              {expectation?.companyExpectation || '（未入力）'}
                            </div>
                          )}
                        </div>
                      </div>
                    )}
                  </div>
                )}

              </>
            )}
          </>
        )}
      </main>

      {/* ── フローティング面談メモパネル（TL/ADMIN のみ） ── */}
      {isTlOrAdmin && selectedId && (
        <>
          {panelOpen && (
            <div
              className="interview-float-panel"
              style={{ left: panelRect.left, top: panelRect.top, width: panelRect.width, height: panelRect.height }}
            >
              {/* 8方向リサイズハンドル */}
              {(['n','s','e','w','nw','ne','sw','se'] as const).map(dir => (
                <div key={dir} className={`ifp-resize ifp-resize--${dir}`} onMouseDown={e => startResize(e, dir)} />
              ))}
              <div className="interview-float-panel__header" onMouseDown={startMove}>
                <span className="interview-float-panel__title">全体備忘録</span>
                <button
                  className="interview-float-panel__close"
                  onMouseDown={e => e.stopPropagation()}
                  onClick={() => setPanelOpen(false)}
                  aria-label="閉じる"
                >×</button>
              </div>
              <div className="interview-float-panel__body">
                {prevYearInterview && (
                  <div className="interview-float-prev-section">
                    <div className="interview-float-prev-label">前年度備忘録</div>
                    <div className="interview-float-prev-text">
                      {prevYearInterview.generalNote || '（なし）'}
                    </div>
                  </div>
                )}
                <textarea
                  className="interview-float-panel__textarea"
                  value={generalNote}
                  onChange={e => setGeneralNote(e.target.value)}
                  placeholder="面談全体のメモを入力してください"
                />
                {saveError && <p className="error-text">{saveError}</p>}
                <div className="interview-float-panel__actions">
                  {interview && !saving && !saveError && (
                    <span className="interview-panel__saved-label">保存済み</span>
                  )}
                  <button
                    className="btn btn-submit interview-float-save-btn"
                    onClick={handleSave}
                    disabled={saving}
                  >
                    {saving ? '保存中...' : '保存'}
                  </button>
                </div>
              </div>
            </div>
          )}
          <button
            className={`interview-float-btn${panelOpen ? ' interview-float-btn--open' : ''}`}
            onClick={() => setPanelOpen(v => !v)}
          >
            {panelOpen ? '✕ 閉じる' : '📝 備忘録'}
          </button>
        </>
      )}
    </div>
  );
}
