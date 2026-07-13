/*******************************************************************************
 * 機能ID      ：USR
 * 機能名      ：ユーザー管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * メンバー詳細ページ。TL/ADMIN がメンバーの棚卸データを年度別に参照する。
 * ITスキル・資格・セミナー・目標・AI分析・期待のタブで閲覧でき、
 * フローティングパネルで面談メモを入力・保存できる。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import { useEffect, useRef, useState, useMemo } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import type { AxiosResponse } from 'axios';
import { getMemberInventories } from '../api/userApi';
import { getExpectations, saveTlExpectation, saveCompanyExpectation } from '../../expectation/api/expectationApi';
import { getMemberAiAnalyses } from '../../ai/api/aiAnalysisApi';
import {
  getItSkillDetails, getQualificationDetails,
  getSeminarDetails, getGoals, getComparison, getGoalReview,
} from '../../inventory/api/inventoryApi';
import { downloadInventoryReport } from '../../report/api/reportApi';
import { getItSkills, getFiscalYears } from '../../../shared/api/masterApi';
import type { FiscalYear } from '../../../shared/types/master';
import { getInterview, saveInterview, getPrevYearInterview } from '../../interview/api/interviewApi';
import type {
  InventorySummary, ItSkillDetailItem, QualificationDetailItem,
  SeminarDetailItem, GoalItem, ComparisonResponse, GoalReviewItem, GoalReviewResponse,
} from '../../inventory/types/index';
import type { AiAnalysis } from '../../ai/types/index';
import type { ItSkill } from '../../../shared/types/master';
import type { InterviewMemo, DetailType } from '../../interview/types';
import type { UserExpectation } from '../../expectation/types/index';
import { useAuth } from '../../../app/providers/AuthProvider';
import NavBar from '../../../app/layouts/NavBar';
import AiAnalysisCard from '../../ai/components/AiAnalysisCard';
import StickyHorizontalScroll from '../../../shared/ui/StickyHorizontalScroll';
import { useTranslation } from 'react-i18next';

type TabKey = 'it-skills' | 'qualifications' | 'seminars' | 'goals' | 'expectations' | 'ai-analysis';

const GOAL_CATEGORY_KEY: Record<string, string> = {
  IT_SKILL: 'goalCategory.itSkill',
  QUALIFICATION: 'goalCategory.qualification',
  AD: 'goalCategory.ad',
};

const ACHIEVEMENT_KEY: Record<string, string> = {
  ACHIEVED: 'achievement.achieved',
  PARTIAL: 'achievement.partial',
  NOT_ACHIEVED: 'achievement.notAchieved',
};

const STATUS_KEY: Record<string, string> = {
  DRAFT: 'status.draft',
  PENDING_GOAL: 'status.pendingGoal',
  COMPLETED: 'status.completed',
};

/**
 * ITスキルの前年比較差分を表示するセルコンポーネント。
 *
 * 前年データがない場合は null を返し、差分に応じて上昇・下降・新規のスタイルで表示する。
 */
function DiffCell({ diff, hasPrevYear }: { diff: number | null | undefined; hasPrevYear: boolean }) {
  const { t } = useTranslation('user');
  if (!hasPrevYear) return null;
  if (diff === null || diff === undefined) return <span className="diff-new">{t('diffNew')}</span>;
  if (diff > 0) return <span className="diff-up">↑ +{diff}</span>;
  if (diff < 0) return <span className="diff-down">↓ {diff}</span>;
  return <span>—</span>;
}

/**
 * 面談メモのキーを生成する。detailType と detailId を組み合わせて一意のキーを返す。
 *
 * @param detailType 明細種別（IT_SKILL / QUALIFICATION / SEMINAR / GOAL）
 * @param detailId 明細 ID
 * @returns Map のキーとして使用する文字列
 */
function buildNoteKey(detailType: DetailType, detailId: number): string {
  return `${detailType}|${detailId}`;
}

/** 文字列配列を昇順（localeCompare）にソートする（配列の内容は変更しない）。 */
function sortStringsAsc(values: string[]): string[] {
  const result = [...values];
  for (let i = 0; i < result.length; i++) {
    for (let j = 0; j < result.length - i - 1; j++) {
      if (result[j].localeCompare(result[j + 1]) > 0) {
        const temp = result[j];
        result[j] = result[j + 1];
        result[j + 1] = temp;
      }
    }
  }
  return result;
}

/** 年度IDの文字列表現が一致する年度を検索する。 */
function findFiscalYearById(fiscalYears: FiscalYear[], idStr: string): FiscalYear | undefined {
  for (const f of fiscalYears) {
    if (String(f.id) === idStr) {
      return f;
    }
  }
  return undefined;
}

/** 目標リストを検索語・カテゴリで絞り込む（目標一覧・前年度目標一覧の両方で使用）。 */
function filterGoalsList(goalsList: GoalItem[], searchLower: string, categoryFilter: string): GoalItem[] {
  const result: GoalItem[] = [];
  for (const g of goalsList) {
    if (categoryFilter && g.goalCategory !== categoryFilter) continue;
    if (searchLower) {
      let name = '';
      if (g.itSkillName != null) {
        name = g.itSkillName;
      } else if (g.qualificationName != null) {
        name = g.qualificationName;
      } else if (g.adSeminarName != null) {
        name = g.adSeminarName;
      } else if (g.customName != null) {
        name = g.customName;
      }
      if (!name.toLowerCase().includes(searchLower)) continue;
    }
    result.push(g);
  }
  return result;
}

/** ITスキル明細に対応するマスタ情報を解決する（未紐付けの場合は undefined）。 */
function resolveItSkillMaster(detail: ItSkillDetailItem, skillMap: Map<number, ItSkill>): ItSkill | undefined {
  let itSkillId = -1;
  if (detail.itSkillId != null) {
    itSkillId = detail.itSkillId;
  }
  return skillMap.get(itSkillId);
}

/** ITスキル明細の並び順を比較する（大分類表示順 → 中分類名 → スキル表示順）。 */
function compareItSkillDetails(a: ItSkillDetailItem, b: ItSkillDetailItem, skillMap: Map<number, ItSkill>): number {
  const ma = resolveItSkillMaster(a, skillMap);
  const mb = resolveItSkillMaster(b, skillMap);

  let aSortOrder1 = 0;
  if (ma != null) {
    aSortOrder1 = ma.category1SortOrder;
  }
  let bSortOrder1 = 0;
  if (mb != null) {
    bSortOrder1 = mb.category1SortOrder;
  }
  if (aSortOrder1 !== bSortOrder1) {
    return aSortOrder1 - bSortOrder1;
  }

  let aCat2 = '';
  if (ma != null && ma.category2Name != null) {
    aCat2 = ma.category2Name;
  }
  let bCat2 = '';
  if (mb != null && mb.category2Name != null) {
    bCat2 = mb.category2Name;
  }
  const cat2Compare = aCat2.localeCompare(bCat2);
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
 * メンバー詳細ページ。
 *
 * TL/ADMIN がメンバーの棚卸データを年度別に参照する。
 * ITスキル・資格・セミナー・目標・AI分析・期待のタブで閲覧でき、
 * フローティングパネルで面談メモを入力・保存できる。
 */
export default function MemberDetailPage() {
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();
  const { t } = useTranslation('user');
  const userIdNum = Number(userId);

  const locationState = location.state as { from?: string; fromLabel?: string } | null;
  let backPath = '/team';
  if (locationState != null && locationState.from != null) {
    backPath = locationState.from;
  }
  let backLabel = t('memberDetail.defaultBackLabel');
  if (locationState != null && locationState.fromLabel != null) {
    backLabel = locationState.fromLabel;
  }

  let isTlOrAdmin = false;
  if (user != null && (user.role === 'TL' || user.role === 'ADMIN')) {
    isTlOrAdmin = true;
  }

  const TAB_LABELS: Record<TabKey, string> = {
    'it-skills': t('memberDetail.tab.itSkills'),
    qualifications: t('memberDetail.tab.qualifications'),
    seminars: t('memberDetail.tab.seminars'),
    goals: t('memberDetail.tab.goals'),
    expectations: t('memberDetail.tab.expectations'),
    'ai-analysis': t('memberDetail.tab.aiAnalysis'),
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

  const [memberAiAnalyses, setMemberAiAnalyses] = useState<AiAnalysis[]>([]);
  const [aiAnalysisLoaded, setAiAnalysisLoaded] = useState(false);

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

  const [fiscalYears, setFiscalYears] = useState<FiscalYear[]>([]);
  const [downloading, setDownloading] = useState(false);

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

  const dragRef = useRef<{ onMove: (e: MouseEvent) => void; onUp: () => void } | null>(null);

  // アンマウント時にドラッグ操作のイベントリスナーをクリーンアップする
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

  // 選択年度が変わったときにすべてのフィルター状態をリセットする
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

  // 初期表示時にメンバーの棚卸一覧と会計年度一覧を取得する
  useEffect(() => {
    getMemberInventories(userIdNum).then(res => {
      setInventories(res.data);
      if (res.data.length > 0) setSelectedId(res.data[0].id);
    });
    getFiscalYears().then(res => setFiscalYears(res.data)).catch(() => {});
  }, [userIdNum]);

  // 選択年度が変わったときに明細・目標・比較・面談メモなどの詳細データを一括取得する
  useEffect(() => {
    if (!selectedId) return;
    setLoading(true);

    let selectedIndex = -1;
    for (let i = 0; i < inventories.length; i++) {
      if (inventories[i].id === selectedId) {
        selectedIndex = i;
        break;
      }
    }
    let nextInventoryId: number | null = null;
    if (selectedIndex > 0) {
      nextInventoryId = inventories[selectedIndex - 1].id;
    }
    let prevInventoryId: number | null = null;
    if (selectedIndex < inventories.length - 1) {
      prevInventoryId = inventories[selectedIndex + 1].id;
    }

    let interviewPromise: Promise<AxiosResponse<InterviewMemo> | null>;
    if (isTlOrAdmin) {
      interviewPromise = getInterview(selectedId).catch(() => null);
    } else {
      interviewPromise = Promise.resolve(null);
    }
    let prevYearInterviewPromise: Promise<AxiosResponse<InterviewMemo> | null>;
    if (isTlOrAdmin) {
      prevYearInterviewPromise = getPrevYearInterview(selectedId).catch(() => null);
    } else {
      prevYearInterviewPromise = Promise.resolve(null);
    }

    let goalReviewPromise: Promise<AxiosResponse<GoalReviewResponse> | null> = Promise.resolve(null);
    if (nextInventoryId) {
      goalReviewPromise = getGoalReview(nextInventoryId).catch(() => null);
    }
    let prevGoalsPromise: Promise<AxiosResponse<{ items: GoalItem[] }> | null> = Promise.resolve(null);
    if (prevInventoryId) {
      prevGoalsPromise = getGoals(prevInventoryId).catch(() => null);
    }
    let prevReviewPromise: Promise<AxiosResponse<GoalReviewResponse> | null> = Promise.resolve(null);
    if (prevInventoryId) {
      prevReviewPromise = getGoalReview(selectedId).catch(() => null);
    }

    Promise.all([
      getItSkillDetails(selectedId),
      getItSkills(),
      getQualificationDetails(selectedId),
      getSeminarDetails(selectedId),
      getGoals(selectedId),
      getComparison(selectedId).catch(() => null),
      goalReviewPromise,
      prevGoalsPromise,
      prevReviewPromise,
      interviewPromise,
      prevYearInterviewPromise,
    ]).then(([itRes, masterRes, qualRes, semRes, goalRes, compRes, reviewRes, prevGoalsRes, prevReviewRes, interviewRes, prevYearRes]) => {
      setItSkillDetails(itRes.data.items);
      setItSkillMaster(masterRes.data);
      setQualificationDetails(qualRes.data.items);
      setSeminarDetails(semRes.data.items);
      setGoals(goalRes.data.items);
      let comparisonValue: ComparisonResponse | null = null;
      if (compRes != null) {
        comparisonValue = compRes.data;
      }
      setComparison(comparisonValue);

      const reviewMap = new Map<number, GoalReviewItem>();
      if (reviewRes != null) {
        for (const item of reviewRes.data.items) {
          reviewMap.set(item.prevGoalId, item);
        }
      }
      setGoalReviewMap(reviewMap);

      let prevGoalsValue: GoalItem[] = [];
      if (prevGoalsRes != null) {
        prevGoalsValue = prevGoalsRes.data.items;
      }
      setPrevGoals(prevGoalsValue);

      const prevRevMap = new Map<number, GoalReviewItem>();
      if (prevReviewRes != null) {
        for (const item of prevReviewRes.data.items) {
          prevRevMap.set(item.prevGoalId, item);
        }
      }
      setPrevGoalReviewMap(prevRevMap);

      let memo: InterviewMemo | null = null;
      if (interviewRes != null) {
        memo = interviewRes.data;
      }
      setInterview(memo);
      let generalNoteValue = '';
      if (memo != null && memo.generalNote != null) {
        generalNoteValue = memo.generalNote;
      }
      setGeneralNote(generalNoteValue);
      const noteMap = new Map<string, string>();
      if (memo) {
        for (const n of memo.detailNotes) {
          noteMap.set(buildNoteKey(n.detailType, n.detailId), n.note);
        }
      }
      setDetailNotes(noteMap);
      setSaveError(null);

      let prevYearMemo: InterviewMemo | null = null;
      if (prevYearRes != null) {
        prevYearMemo = prevYearRes.data;
      }
      setPrevYearInterview(prevYearMemo);
    }).finally(() => setLoading(false));
  }, [selectedId, inventories, isTlOrAdmin]);

  // AI分析タブが初めて表示されたときにメンバーの AI分析データを取得する
  useEffect(() => {
    if (activeTab !== 'ai-analysis' || aiAnalysisLoaded || !isTlOrAdmin) return;
    getMemberAiAnalyses(userIdNum)
      .then(res => setMemberAiAnalyses(res.data))
      .catch(() => {})
      .finally(() => setAiAnalysisLoaded(true));
  }, [activeTab, aiAnalysisLoaded, isTlOrAdmin, userIdNum]);

  // 期待タブが初めて表示されたときにメンバーへの期待データを取得する
  useEffect(() => {
    if (activeTab !== 'expectations' || expLoaded || !isTlOrAdmin) return;
    setExpLoading(true);
    getExpectations(userIdNum)
      .then(res => {
        setExpectation(res.data);
        let tlExp = '';
        if (res.data.tlExpectation != null) {
          tlExp = res.data.tlExpectation;
        }
        setEditTl(tlExp);
        let companyExp = '';
        if (res.data.companyExpectation != null) {
          companyExp = res.data.companyExpectation;
        }
        setEditCompany(companyExp);
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
      setTlSaveError(t('memberDetail.expectation.saveFailed'));
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
      setCompanySaveError(t('memberDetail.expectation.saveFailed'));
    } finally {
      setCompanySaving(false);
    }
  }

  async function handleDownloadReport() {
    if (!selectedId) return;
    setDownloading(true);
    try {
      const res = await downloadInventoryReport(selectedId);
      const url = URL.createObjectURL(new Blob([res.data as BlobPart], { type: 'application/pdf' }));
      const a = document.createElement('a');
      a.href = url;
      a.download = `inventory_report_${selectedId}.pdf`;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      // ダウンロード失敗は静かに無視（ネットワークエラー等）
    } finally {
      setDownloading(false);
    }
  }

  const itSkillTree = useMemo(() => {
    const skillMap = new Map<number, ItSkill>();
    for (const s of itSkillMaster) {
      skillMap.set(s.id, s);
    }
    const map = new Map<string, Map<string, ItSkillDetailItem[]>>();
    const customItems: ItSkillDetailItem[] = [];

    const sortedDetails = [...itSkillDetails];
    for (let i = 0; i < sortedDetails.length; i++) {
      for (let j = 0; j < sortedDetails.length - i - 1; j++) {
        if (compareItSkillDetails(sortedDetails[j], sortedDetails[j + 1], skillMap) > 0) {
          const temp = sortedDetails[j];
          sortedDetails[j] = sortedDetails[j + 1];
          sortedDetails[j + 1] = temp;
        }
      }
    }

    for (const detail of sortedDetails) {
      if (detail.itSkillId === null) {
        customItems.push(detail);
      } else {
        const master = skillMap.get(detail.itSkillId);
        let cat1 = '未分類';
        let cat2 = '';
        if (master != null) {
          if (master.category1Name != null) {
            cat1 = master.category1Name;
          }
          if (master.category2Name != null) {
            cat2 = master.category2Name;
          }
        }
        if (!map.has(cat1)) map.set(cat1, new Map());
        const cat2Map = map.get(cat1)!;
        if (!cat2Map.has(cat2)) cat2Map.set(cat2, []);
        cat2Map.get(cat2)!.push(detail);
      }
    }

    const groups: { cat1: string; cat2Groups: { cat2: string; items: ItSkillDetailItem[] }[] }[] = [];
    for (const [cat1, cat2Map] of map.entries()) {
      const cat2Groups: { cat2: string; items: ItSkillDetailItem[] }[] = [];
      for (const [cat2, items] of cat2Map.entries()) {
        cat2Groups.push({ cat2, items });
      }
      groups.push({ cat1, cat2Groups });
    }

    return { groups, customItems };
  }, [itSkillDetails, itSkillMaster]);

  const comparisonMap = useMemo(() => {
    const map = new Map<number, { prevLevelValue: number | null; diff: number | null }>();
    if (comparison != null) {
      for (const item of comparison.items) {
        map.set(item.currentDetailId, item);
      }
    }
    return map;
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

  let hasPrevYear = false;
  if (comparison != null) {
    hasPrevYear = comparison.hasPrevYear;
  }
  let itSkillColCount = 3;
  if (hasPrevYear) {
    itSkillColCount += 2;
  }
  if (isTlOrAdmin) {
    itSkillColCount += 1;
  }

  const itSkillCat2Options = useMemo(() => {
    let sourceGroups = itSkillTree.groups;
    if (itSkillCategory1Filter) {
      sourceGroups = [];
      for (const g of itSkillTree.groups) {
        if (g.cat1 === itSkillCategory1Filter) {
          sourceGroups.push(g);
        }
      }
    }
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

    const filteredGroups: { cat1: string; cat2Groups: { cat2: string; items: ItSkillDetailItem[] }[] }[] = [];
    for (const g of itSkillTree.groups) {
      if (itSkillCategory1Filter && g.cat1 !== itSkillCategory1Filter) continue;

      const cat2Groups: { cat2: string; items: ItSkillDetailItem[] }[] = [];
      for (const cg of g.cat2Groups) {
        if (itSkillCategory2Filter && cg.cat2 !== itSkillCategory2Filter) continue;

        const items: ItSkillDetailItem[] = [];
        for (const item of cg.items) {
          let itSkillName = '';
          if (item.itSkillName != null) {
            itSkillName = item.itSkillName;
          }
          if (searchLower && !itSkillName.toLowerCase().includes(searchLower)) continue;
          if (itSkillDiffFilter) {
            const comp = comparisonMap.get(item.id);
            if (itSkillDiffFilter === 'new' && comp !== undefined) continue;
            let compDiff = 0;
            if (comp != null && comp.diff != null) {
              compDiff = comp.diff;
            }
            if (itSkillDiffFilter === 'up' && (comp === undefined || compDiff <= 0)) continue;
            if (itSkillDiffFilter === 'down' && (comp === undefined || compDiff >= 0)) continue;
          }
          items.push(item);
        }
        if (items.length > 0) {
          cat2Groups.push({ cat2: cg.cat2, items });
        }
      }
      if (cat2Groups.length > 0) {
        filteredGroups.push({ cat1: g.cat1, cat2Groups });
      }
    }

    let showCustom = itSkillCategory1Filter === '__custom__';
    if (!showCustom && !itSkillCategory1Filter && !itSkillCategory2Filter
        && itSkillDiffFilter !== 'up' && itSkillDiffFilter !== 'down') {
      showCustom = true;
    }
    const filteredCustom: ItSkillDetailItem[] = [];
    if (showCustom) {
      for (const item of itSkillTree.customItems) {
        let customSkillName = '';
        if (item.customSkillName != null) {
          customSkillName = item.customSkillName;
        }
        if (!searchLower || customSkillName.toLowerCase().includes(searchLower)) {
          filteredCustom.push(item);
        }
      }
    }

    return { groups: filteredGroups, customItems: filteredCustom };
  }, [itSkillTree, itSkillSearch, itSkillCategory1Filter, itSkillCategory2Filter, itSkillDiffFilter, comparisonMap]);

  const filteredItSkillCount = useMemo(() => {
    let count = 0;
    for (const { cat2Groups } of filteredItSkillTree.groups) {
      for (const { items } of cat2Groups) count += items.length;
    }
    return count + filteredItSkillTree.customItems.length;
  }, [filteredItSkillTree]);

  const qualCategories = useMemo(() => {
    const set = new Set<string>();
    for (const q of qualificationDetails) {
      if (q.qualificationCategoryName !== null) {
        set.add(q.qualificationCategoryName);
      }
    }
    return sortStringsAsc(Array.from(set));
  }, [qualificationDetails]);

  const seminarAdCategories = useMemo(() => {
    const set = new Set<string>();
    for (const s of seminarDetails) {
      if (s.adSeminarId !== null && s.adSeminarCategoryName !== null) {
        set.add(s.adSeminarCategoryName);
      }
    }
    return sortStringsAsc(Array.from(set));
  }, [seminarDetails]);

  const filteredQualifications = useMemo(() => {
    const searchLower = qualNameSearch.toLowerCase();
    const result: QualificationDetailItem[] = [];
    for (const q of qualificationDetails) {
      if (qualCategoryFilter) {
        if (qualCategoryFilter === '__custom__') {
          if (q.qualificationId !== null) continue;
        } else {
          if (q.qualificationCategoryName !== qualCategoryFilter) continue;
        }
      }
      if (searchLower) {
        let name = '';
        if (q.qualificationName != null) {
          name = q.qualificationName;
        } else if (q.customQualificationName != null) {
          name = q.customQualificationName;
        }
        if (!name.toLowerCase().includes(searchLower)) continue;
      }
      if (qualFiscalYearFilter) {
        const fy = findFiscalYearById(fiscalYears, qualFiscalYearFilter);
        if (fy) {
          if (!q.acquiredYearMonth) continue;
          const ym = q.acquiredYearMonth.slice(0, 7);
          if (ym < fy.startDate.slice(0, 7) || ym > fy.endDate.slice(0, 7)) continue;
        }
      }
      result.push(q);
    }
    return result;
  }, [qualificationDetails, qualNameSearch, qualCategoryFilter, qualFiscalYearFilter, fiscalYears]);

  const filteredSeminars = useMemo(() => {
    const searchLower = seminarNameSearch.toLowerCase();
    const result: SeminarDetailItem[] = [];
    for (const s of seminarDetails) {
      if (seminarTypeFilter === 'AD' && s.adSeminarId === null) continue;
      if (seminarTypeFilter === 'FREE' && s.adSeminarId !== null) continue;
      if (seminarCategoryFilter && s.adSeminarCategoryName !== seminarCategoryFilter) continue;
      if (searchLower) {
        let name = '';
        if (s.adSeminarName != null) {
          name = s.adSeminarName;
        } else if (s.seminarName != null) {
          name = s.seminarName;
        }
        if (!name.toLowerCase().includes(searchLower)) continue;
      }
      if (seminarFiscalYearFilter) {
        const fy = findFiscalYearById(fiscalYears, seminarFiscalYearFilter);
        if (fy) {
          if (!s.attendedYearMonth) continue;
          const ym = s.attendedYearMonth.slice(0, 7);
          if (ym < fy.startDate.slice(0, 7) || ym > fy.endDate.slice(0, 7)) continue;
        }
      }
      result.push(s);
    }
    return result;
  }, [seminarDetails, seminarNameSearch, seminarTypeFilter, seminarCategoryFilter, seminarFiscalYearFilter, fiscalYears]);

  const filteredGoals = useMemo(
    () => filterGoalsList(goals, goalSearch.toLowerCase(), goalCategoryFilter),
    [goals, goalSearch, goalCategoryFilter],
  );

  const filteredPrevGoals = useMemo(
    () => filterGoalsList(prevGoals, goalSearch.toLowerCase(), goalCategoryFilter),
    [prevGoals, goalSearch, goalCategoryFilter],
  );

  let selectedInventory: InventorySummary | undefined;
  for (const inv of inventories) {
    if (inv.id === selectedId) {
      selectedInventory = inv;
      break;
    }
  }

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
      const notes: { detailType: DetailType; detailId: number; note: string }[] = [];
      for (const [key, note] of detailNotes.entries()) {
        const sep = key.lastIndexOf('|');
        const type = key.slice(0, sep) as DetailType;
        const id = Number(key.slice(sep + 1));
        notes.push({ detailType: type, detailId: id, note });
      }
      const res = await saveInterview(selectedId, {
        generalNote: generalNote || null,
        detailNotes: notes,
      });
      setInterview(res.data);
    } catch {
      setSaveError(t('memberDetail.interview.saveFailed'));
    } finally {
      setSaving(false);
    }
  }

  function renderDetailNoteCell(detailType: DetailType, detailId: number, showPrev = true) {
    const key = buildNoteKey(detailType, detailId);
    let prevNote: string | undefined;
    if (showPrev) {
      prevNote = prevYearNoteMap.get(key);
    }
    let noteValue = '';
    const existingNote = detailNotes.get(key);
    if (existingNote != null) {
      noteValue = existingNote;
    }
    return (
      <td className="interview-note-cell">
        <textarea
          className="interview-note-textarea"
          value={noteValue}
          onChange={e => setDetailNote(detailType, detailId, e.target.value)}
          placeholder={t('memberDetail.interview.noteInputPlaceholder')}
          rows={1}
        />
        {prevNote && (
          <div className="interview-note-prev-inline">{t('memberDetail.interview.prevNotePrefix')}{prevNote}</div>
        )}
      </td>
    );
  }

  function renderPrevGoalNoteCell(g: GoalItem) {
    const note = prevYearNoteMap.get(buildNoteKey('GOAL', goalDetailId(g)));
    let content: React.ReactNode;
    if (note) {
      content = <div className="interview-note-prev-readonly">{note}</div>;
    } else {
      content = <span className="interview-note-empty">—</span>;
    }
    return (
      <td className="interview-note-cell">
        {content}
      </td>
    );
  }

  let selectedIdValue: number | string = '';
  if (selectedId != null) {
    selectedIdValue = selectedId;
  }

  const yearOptions: React.ReactNode[] = [];
  for (const inv of inventories) {
    let statusLabelKey: string = inv.status;
    if (STATUS_KEY[inv.status] != null) {
      statusLabelKey = STATUS_KEY[inv.status];
    }
    yearOptions.push(
      <option key={inv.id} value={inv.id}>
        {inv.fiscalYear.name}（{t(statusLabelKey)}）
      </option>,
    );
  }

  const tabButtons: React.ReactNode[] = [];
  for (const tab of Object.keys(TAB_LABELS) as TabKey[]) {
    const isRestricted = tab === 'expectations' || tab === 'ai-analysis';
    if (isRestricted && !isTlOrAdmin) continue;
    let tabClassName = 'tab-btn';
    if (activeTab === tab) {
      tabClassName += ' active';
    }
    tabButtons.push(
      <button
        key={tab}
        className={tabClassName}
        onClick={() => setActiveTab(tab)}
      >
        {TAB_LABELS[tab]}
      </button>,
    );
  }

  // ── ITスキルタブ ──
  const cat1FilterOptions: React.ReactNode[] = [];
  for (const { cat1 } of itSkillTree.groups) {
    cat1FilterOptions.push(<option key={cat1} value={cat1}>{cat1}</option>);
  }
  const cat2FilterOptions: React.ReactNode[] = [];
  for (const cat2 of itSkillCat2Options) {
    cat2FilterOptions.push(<option key={cat2} value={cat2}>{cat2}</option>);
  }

  let itSkillTableBody: React.ReactNode;
  if (itSkillDetails.length === 0) {
    itSkillTableBody = (
      <tr>
        <td colSpan={itSkillColCount} className="no-data-cell">
          {t('memberDetail.noDataCell.itSkills')}
        </td>
      </tr>
    );
  } else if (filteredItSkillCount === 0) {
    itSkillTableBody = (
      <tr>
        <td colSpan={itSkillColCount} className="no-data-cell">
          {t('memberDetail.noDataCell.noMatchSkills')}
        </td>
      </tr>
    );
  } else {
    const rows: React.ReactNode[] = [];
    for (const { cat1, cat2Groups } of filteredItSkillTree.groups) {
      rows.push(
        <tr key={cat1} className="scoring-cat1-row">
          <td colSpan={itSkillColCount}>{cat1}</td>
        </tr>,
      );
      for (const { cat2, items } of cat2Groups) {
        if (cat2) {
          rows.push(
            <tr key={`${cat1}-${cat2}-header`} className="scoring-cat2-row">
              <td colSpan={itSkillColCount}>{cat2}</td>
            </tr>,
          );
        }
        for (const detail of items) {
          const comp = comparisonMap.get(detail.id);
          let prevLevelLabel: React.ReactNode = '—';
          if (comp != null && comp.prevLevelValue != null) {
            prevLevelLabel = comp.prevLevelValue;
          }
          let diffValue: number | null | undefined;
          if (comp != null) {
            diffValue = comp.diff;
          }
          let noteDetailId = detail.id;
          if (detail.itSkillId != null) {
            noteDetailId = detail.itSkillId;
          }
          let remarksLabel = '—';
          if (detail.remarks) {
            remarksLabel = detail.remarks;
          }
          rows.push(
            <tr key={detail.id}>
              <td>{detail.itSkillName}</td>
              {hasPrevYear && <td>{prevLevelLabel}</td>}
              <td>{detail.levelValue}</td>
              {hasPrevYear && (
                <td className="diff-cell">
                  <DiffCell diff={diffValue} hasPrevYear={hasPrevYear} />
                </td>
              )}
              <td>{remarksLabel}</td>
              {isTlOrAdmin && renderDetailNoteCell('IT_SKILL', noteDetailId)}
            </tr>,
          );
        }
      }
    }
    if (filteredItSkillTree.customItems.length > 0) {
      rows.push(
        <tr key="__custom__" className="scoring-cat1-row">
          <td colSpan={itSkillColCount}>{t('customSkillLabel')}</td>
        </tr>,
      );
      for (const detail of filteredItSkillTree.customItems) {
        let remarksLabel = '—';
        if (detail.remarks) {
          remarksLabel = detail.remarks;
        }
        rows.push(
          <tr key={detail.id}>
            <td>{detail.customSkillName} ※</td>
            {hasPrevYear && <td>—</td>}
            <td>—</td>
            {hasPrevYear && (
              <td className="diff-cell">
                <DiffCell diff={null} hasPrevYear={hasPrevYear} />
              </td>
            )}
            <td>{remarksLabel}</td>
            {isTlOrAdmin && renderDetailNoteCell('IT_SKILL', detail.id)}
          </tr>,
        );
      }
    }
    itSkillTableBody = rows;
  }

  // ── 資格タブ ──
  let qualificationsTabContent: React.ReactNode;
  if (qualificationDetails.length === 0) {
    qualificationsTabContent = <p className="no-data">{t('memberDetail.noDataCell.qualifications')}</p>;
  } else {
    const qualCategoryOptions: React.ReactNode[] = [];
    for (const cat of qualCategories) {
      qualCategoryOptions.push(<option key={cat} value={cat}>{cat}</option>);
    }
    let hasCustomQualification = false;
    for (const q of qualificationDetails) {
      if (q.qualificationId === null) {
        hasCustomQualification = true;
        break;
      }
    }
    const qualFiscalYearOptions: React.ReactNode[] = [];
    for (const fy of fiscalYears) {
      qualFiscalYearOptions.push(<option key={fy.id} value={String(fy.id)}>{fy.name}</option>);
    }

    let qualColSpan = 4;
    if (isTlOrAdmin) {
      qualColSpan = 5;
    }

    let tableBody: React.ReactNode;
    if (filteredQualifications.length === 0) {
      tableBody = <tr><td colSpan={qualColSpan} className="no-data-cell">{t('memberDetail.noDataCell.noMatchQuals')}</td></tr>;
    } else {
      const rows: React.ReactNode[] = [];
      for (const q of filteredQualifications) {
        let categoryLabel = '—';
        if (q.qualificationCategoryName != null) {
          categoryLabel = q.qualificationCategoryName;
        }
        let nameLabel = '—';
        if (q.qualificationName != null) {
          nameLabel = q.qualificationName;
        } else if (q.customQualificationName != null) {
          nameLabel = q.customQualificationName;
        }
        let acquiredLabel = '—';
        if (q.acquiredYearMonth != null) {
          acquiredLabel = q.acquiredYearMonth.slice(0, 7);
        }
        let remarksLabel = '—';
        if (q.remarks) {
          remarksLabel = q.remarks;
        }
        let noteDetailId = q.id;
        if (q.qualificationId != null) {
          noteDetailId = q.qualificationId;
        }
        rows.push(
          <tr key={q.id}>
            <td>{categoryLabel}</td>
            <td>
              {nameLabel}
              {q.qualificationId === null && ' ※'}
            </td>
            <td>{acquiredLabel}</td>
            <td>{remarksLabel}</td>
            {isTlOrAdmin && renderDetailNoteCell('QUALIFICATION', noteDetailId)}
          </tr>,
        );
      }
      tableBody = rows;
    }

    qualificationsTabContent = (
      <>
        <div className="history-filter-bar">
          <input
            className="history-filter-bar__input"
            placeholder={t('memberDetail.filter.qualNameSearch')}
            value={qualNameSearch}
            onChange={e => setQualNameSearch(e.target.value)}
          />
          <select
            className="history-filter-bar__select"
            value={qualCategoryFilter}
            onChange={e => setQualCategoryFilter(e.target.value)}
          >
            <option value="">{t('memberDetail.filter.qualCategoryAll')}</option>
            {qualCategoryOptions}
            {hasCustomQualification && (
              <option value="__custom__">{t('memberDetail.filter.custom')}</option>
            )}
          </select>
          <select
            className="history-filter-bar__select"
            value={qualFiscalYearFilter}
            onChange={e => setQualFiscalYearFilter(e.target.value)}
          >
            <option value="">{t('memberDetail.filter.qualFiscalYearAll')}</option>
            {qualFiscalYearOptions}
          </select>
          <span className="history-result-count">{filteredQualifications.length}件</span>
        </div>
        <StickyHorizontalScroll className="master-table-wrap">
          <table className="master-table">
            <thead>
              <tr>
                <th>{t('memberDetail.table.category')}</th>
                <th>{t('memberDetail.table.qualName')}</th>
                <th>{t('memberDetail.table.acquiredYearMonth')}</th>
                <th>{t('memberDetail.table.remarks')}</th>
                {isTlOrAdmin && <th className="interview-note-th">{t('memberDetail.table.interviewNote')}</th>}
              </tr>
            </thead>
            <tbody>
              {tableBody}
            </tbody>
          </table>
        </StickyHorizontalScroll>
      </>
    );
  }

  // ── セミナータブ ──
  let seminarsTabContent: React.ReactNode;
  if (seminarDetails.length === 0) {
    seminarsTabContent = <p className="no-data">{t('memberDetail.noDataCell.seminars')}</p>;
  } else {
    const seminarCategoryOptions: React.ReactNode[] = [];
    for (const cat of seminarAdCategories) {
      seminarCategoryOptions.push(<option key={cat} value={cat}>{cat}</option>);
    }
    const seminarFiscalYearOptions: React.ReactNode[] = [];
    for (const fy of fiscalYears) {
      seminarFiscalYearOptions.push(<option key={fy.id} value={String(fy.id)}>{fy.name}</option>);
    }

    let seminarColSpan = 5;
    if (isTlOrAdmin) {
      seminarColSpan = 6;
    }

    let tableBody: React.ReactNode;
    if (filteredSeminars.length === 0) {
      tableBody = <tr><td colSpan={seminarColSpan} className="no-data-cell">{t('memberDetail.noDataCell.noMatchSeminars')}</td></tr>;
    } else {
      const rows: React.ReactNode[] = [];
      for (const s of filteredSeminars) {
        let typeLabel: string = t('memberDetail.filter.seminarTypeFree');
        if (s.adSeminarId !== null) {
          typeLabel = 'AD';
        }
        let categoryLabel = '—';
        if (s.adSeminarId !== null && s.adSeminarCategoryName != null) {
          categoryLabel = s.adSeminarCategoryName;
        }
        let nameLabel = '—';
        if (s.adSeminarName != null) {
          nameLabel = s.adSeminarName;
        } else if (s.seminarName != null) {
          nameLabel = s.seminarName;
        }
        let attendedLabel = '—';
        if (s.attendedYearMonth != null) {
          attendedLabel = s.attendedYearMonth.slice(0, 7);
        }
        let remarksLabel = '—';
        if (s.remarks) {
          remarksLabel = s.remarks;
        }
        rows.push(
          <tr key={s.id}>
            <td>{typeLabel}</td>
            <td>{categoryLabel}</td>
            <td>{nameLabel}</td>
            <td>{attendedLabel}</td>
            <td>{remarksLabel}</td>
            {isTlOrAdmin && renderDetailNoteCell('SEMINAR', s.id)}
          </tr>,
        );
      }
      tableBody = rows;
    }

    seminarsTabContent = (
      <>
        <div className="history-filter-bar">
          <input
            className="history-filter-bar__input"
            placeholder={t('memberDetail.filter.seminarNameSearch')}
            value={seminarNameSearch}
            onChange={e => setSeminarNameSearch(e.target.value)}
          />
          <select
            className="history-filter-bar__select"
            value={seminarTypeFilter}
            onChange={e => { setSeminarTypeFilter(e.target.value as '' | 'AD' | 'FREE'); setSeminarCategoryFilter(''); }}
          >
            <option value="">{t('memberDetail.filter.seminarTypeAll')}</option>
            <option value="AD">AD</option>
            <option value="FREE">{t('memberDetail.filter.seminarTypeFree')}</option>
          </select>
          {seminarTypeFilter !== 'FREE' && seminarAdCategories.length > 0 && (
            <select
              className="history-filter-bar__select"
              value={seminarCategoryFilter}
              onChange={e => setSeminarCategoryFilter(e.target.value)}
            >
              <option value="">{t('memberDetail.filter.seminarCategoryAll')}</option>
              {seminarCategoryOptions}
            </select>
          )}
          <select
            className="history-filter-bar__select"
            value={seminarFiscalYearFilter}
            onChange={e => setSeminarFiscalYearFilter(e.target.value)}
          >
            <option value="">{t('memberDetail.filter.seminarFiscalYearAll')}</option>
            {seminarFiscalYearOptions}
          </select>
          <span className="history-result-count">{filteredSeminars.length}件</span>
        </div>
        <StickyHorizontalScroll className="master-table-wrap">
          <table className="master-table">
            <thead>
              <tr>
                <th>{t('memberDetail.table.seminarType')}</th>
                <th>{t('memberDetail.table.category')}</th>
                <th>{t('memberDetail.table.seminarName')}</th>
                <th>{t('memberDetail.table.attendedYearMonth')}</th>
                <th>{t('memberDetail.table.remarks')}</th>
                {isTlOrAdmin && <th className="interview-note-th">{t('memberDetail.table.interviewNote')}</th>}
              </tr>
            </thead>
            <tbody>
              {tableBody}
            </tbody>
          </table>
        </StickyHorizontalScroll>
      </>
    );
  }

  // ── 目標タブ ──
  let goalsTabContent: React.ReactNode;
  if (prevGoals.length === 0 && goals.length === 0) {
    goalsTabContent = <p className="no-data">{t('memberDetail.noDataCell.goals')}</p>;
  } else {
    let prevGoalsSection: React.ReactNode = null;
    if (prevGoals.length > 0) {
      let prevGoalsBody: React.ReactNode;
      if (filteredPrevGoals.length === 0) {
        prevGoalsBody = <p className="no-data">{t('memberDetail.noDataCell.noMatchGoals')}</p>;
      } else {
        const rows: React.ReactNode[] = [];
        for (const g of filteredPrevGoals) {
          const review = prevGoalReviewMap.get(g.id);
          let categoryLabelKey: string = g.goalCategory;
          if (GOAL_CATEGORY_KEY[g.goalCategory] != null) {
            categoryLabelKey = GOAL_CATEGORY_KEY[g.goalCategory];
          }
          let nameLabel = '—';
          if (g.itSkillName != null) {
            nameLabel = g.itSkillName;
          } else if (g.qualificationName != null) {
            nameLabel = g.qualificationName;
          } else if (g.adSeminarName != null) {
            nameLabel = g.adSeminarName;
          } else if (g.customName != null) {
            nameLabel = g.customName;
          }
          let targetPeriodLabel = '—';
          if (g.targetPeriod != null) {
            targetPeriodLabel = g.targetPeriod.slice(0, 7);
          }
          let reasonLabel = '—';
          if (g.reason) {
            reasonLabel = g.reason;
          }
          let achievementLabel = '—';
          if (review != null && review.achievementStatus) {
            let achievementKey: string = review.achievementStatus;
            if (ACHIEVEMENT_KEY[review.achievementStatus] != null) {
              achievementKey = ACHIEVEMENT_KEY[review.achievementStatus];
            }
            achievementLabel = t(achievementKey);
          }
          let reviewNoteLabel = '—';
          if (review != null && review.reviewNote) {
            reviewNoteLabel = review.reviewNote;
          }
          rows.push(
            <tr key={g.id}>
              <td><span className="goal-category-badge">{t(categoryLabelKey)}</span></td>
              <td>{nameLabel}</td>
              <td>{targetPeriodLabel}</td>
              <td>{reasonLabel}</td>
              <td>{achievementLabel}</td>
              <td>{reviewNoteLabel}</td>
              {isTlOrAdmin && renderPrevGoalNoteCell(g)}
            </tr>,
          );
        }
        prevGoalsBody = (
          <StickyHorizontalScroll className="master-table-wrap">
            <table className="master-table">
              <thead>
                <tr>
                  <th style={{ width: 80 }}>{t('memberDetail.table.goalCategory')}</th>
                  <th>{t('memberDetail.table.goalName')}</th>
                  <th style={{ width: 120 }}>{t('memberDetail.table.targetPeriod')}</th>
                  <th>{t('memberDetail.table.reasonPlan')}</th>
                  <th style={{ width: 90 }}>{t('memberDetail.table.achievementStatus')}</th>
                  <th>{t('memberDetail.table.reviewNote')}</th>
                  {isTlOrAdmin && <th className="interview-note-th">{t('memberDetail.table.prevYearInterviewNote')}</th>}
                </tr>
              </thead>
              <tbody>
                {rows}
              </tbody>
            </table>
          </StickyHorizontalScroll>
        );
      }
      prevGoalsSection = (
        <div className="history-goal-section">
          <h3 className="history-goal-title">{t('memberDetail.goalSection.prevYear')}</h3>
          {prevGoalsBody}
        </div>
      );
    }

    let currentGoalsSection: React.ReactNode = null;
    if (goals.length > 0) {
      let currentGoalsBody: React.ReactNode;
      if (filteredGoals.length === 0) {
        currentGoalsBody = <p className="no-data">{t('memberDetail.noDataCell.noMatchGoals')}</p>;
      } else {
        const rows: React.ReactNode[] = [];
        for (const g of filteredGoals) {
          const review = goalReviewMap.get(g.id);
          let categoryLabelKey: string = g.goalCategory;
          if (GOAL_CATEGORY_KEY[g.goalCategory] != null) {
            categoryLabelKey = GOAL_CATEGORY_KEY[g.goalCategory];
          }
          let nameLabel = '—';
          if (g.itSkillName != null) {
            nameLabel = g.itSkillName;
          } else if (g.qualificationName != null) {
            nameLabel = g.qualificationName;
          } else if (g.adSeminarName != null) {
            nameLabel = g.adSeminarName;
          } else if (g.customName != null) {
            nameLabel = g.customName;
          }
          let targetPeriodLabel = '—';
          if (g.targetPeriod != null) {
            targetPeriodLabel = g.targetPeriod.slice(0, 7);
          }
          let reasonLabel = '—';
          if (g.reason) {
            reasonLabel = g.reason;
          }
          let achievementLabel = '—';
          if (review != null && review.achievementStatus) {
            let achievementKey: string = review.achievementStatus;
            if (ACHIEVEMENT_KEY[review.achievementStatus] != null) {
              achievementKey = ACHIEVEMENT_KEY[review.achievementStatus];
            }
            achievementLabel = t(achievementKey);
          }
          let reviewNoteLabel = '—';
          if (review != null && review.reviewNote) {
            reviewNoteLabel = review.reviewNote;
          }
          rows.push(
            <tr key={g.id}>
              <td><span className="goal-category-badge">{t(categoryLabelKey)}</span></td>
              <td>{nameLabel}</td>
              <td>{targetPeriodLabel}</td>
              <td>{reasonLabel}</td>
              <td>{achievementLabel}</td>
              <td>{reviewNoteLabel}</td>
              {isTlOrAdmin && renderDetailNoteCell('GOAL', goalDetailId(g), false)}
            </tr>,
          );
        }
        currentGoalsBody = (
          <StickyHorizontalScroll className="master-table-wrap">
            <table className="master-table">
              <thead>
                <tr>
                  <th style={{ width: 80 }}>{t('memberDetail.table.goalCategory')}</th>
                  <th>{t('memberDetail.table.goalName')}</th>
                  <th style={{ width: 120 }}>{t('memberDetail.table.targetPeriod')}</th>
                  <th>{t('memberDetail.table.reasonPlan')}</th>
                  <th style={{ width: 90 }}>{t('memberDetail.table.achievementStatus')}</th>
                  <th>{t('memberDetail.table.reviewNote')}</th>
                  {isTlOrAdmin && <th className="interview-note-th">{t('memberDetail.table.interviewNote')}</th>}
                </tr>
              </thead>
              <tbody>
                {rows}
              </tbody>
            </table>
          </StickyHorizontalScroll>
        );
      }
      currentGoalsSection = (
        <div className="history-goal-section">
          <h3 className="history-goal-title">{t('memberDetail.goalSection.currentYear')}</h3>
          {currentGoalsBody}
        </div>
      );
    }

    goalsTabContent = (
      <>
        <div className="history-filter-bar">
          <input
            className="history-filter-bar__input"
            placeholder={t('memberDetail.filter.goalSearch')}
            value={goalSearch}
            onChange={e => setGoalSearch(e.target.value)}
          />
          <select
            className="history-filter-bar__select"
            value={goalCategoryFilter}
            onChange={e => setGoalCategoryFilter(e.target.value as '' | 'IT_SKILL' | 'QUALIFICATION' | 'AD')}
          >
            <option value="">{t('memberDetail.filter.goalCategoryAll')}</option>
            <option value="IT_SKILL">{t('goalCategory.itSkill')}</option>
            <option value="QUALIFICATION">{t('goalCategory.qualification')}</option>
            <option value="AD">AD</option>
          </select>
          <span className="history-result-count">
            {filteredPrevGoals.length + filteredGoals.length}件
          </span>
        </div>
        {prevGoalsSection}
        {currentGoalsSection}
      </>
    );
  }

  // ── AI分析タブ ──
  let aiAnalysisTabContent: React.ReactNode;
  if (!aiAnalysisLoaded) {
    aiAnalysisTabContent = <div className="loading">{t('loading')}</div>;
  } else {
    let matchedInventory: InventorySummary | undefined;
    for (const inv of inventories) {
      if (inv.id === selectedId) {
        matchedInventory = inv;
        break;
      }
    }
    let analysis: AiAnalysis | undefined;
    if (matchedInventory != null) {
      for (const a of memberAiAnalyses) {
        if (a.fiscalYearId === matchedInventory.fiscalYear.id) {
          analysis = a;
          break;
        }
      }
    }
    if (analysis == null) {
      aiAnalysisTabContent = <p className="no-data">{t('memberDetail.noDataCell.aiAnalysis')}</p>;
    } else {
      aiAnalysisTabContent = <AiAnalysisCard analysis={analysis} />;
    }
  }

  // ── 期待タブ ──
  let expectationsTabContent: React.ReactNode;
  if (expLoading) {
    expectationsTabContent = <div className="loading">{t('loading')}</div>;
  } else {
    let tlSaveButtonLabel = t('memberDetail.expectation.saveButton');
    if (tlSaving) {
      tlSaveButtonLabel = t('memberDetail.expectation.savingButton');
    }
    let companySaveButtonLabel = t('memberDetail.expectation.saveButton');
    if (companySaving) {
      companySaveButtonLabel = t('memberDetail.expectation.savingButton');
    }

    let tlSection: React.ReactNode;
    if (user != null && user.role === 'TL') {
      tlSection = (
        <>
          <textarea
            className="expectation-detail-textarea"
            value={editTl}
            onChange={e => { setEditTl(e.target.value); setTlSaved(false); }}
            placeholder={t('memberDetail.expectation.tlPlaceholder')}
            rows={6}
          />
          <div className="expectation-detail-save-row">
            {tlSaved && !tlSaveError && (
              <span className="expectation-saved-label">{t('memberDetail.expectation.savedLabel')}</span>
            )}
            {tlSaveError && <span className="error-text">{tlSaveError}</span>}
            <button
              className="btn btn-submit expectation-save-btn"
              onClick={handleSaveTl}
              disabled={tlSaving}
            >
              {tlSaveButtonLabel}
            </button>
          </div>
        </>
      );
    } else {
      let tlReadonlyText = t('memberDetail.expectation.noInput');
      if (expectation != null && expectation.tlExpectation) {
        tlReadonlyText = expectation.tlExpectation;
      }
      tlSection = (
        <div className="expectation-detail-readonly">
          {tlReadonlyText}
        </div>
      );
    }

    let companySection: React.ReactNode;
    if (user != null && user.role === 'ADMIN') {
      companySection = (
        <>
          <textarea
            className="expectation-detail-textarea"
            value={editCompany}
            onChange={e => { setEditCompany(e.target.value); setCompanySaved(false); }}
            placeholder={t('memberDetail.expectation.companyPlaceholder')}
            rows={6}
          />
          <div className="expectation-detail-save-row">
            {companySaved && !companySaveError && (
              <span className="expectation-saved-label">{t('memberDetail.expectation.savedLabel')}</span>
            )}
            {companySaveError && <span className="error-text">{companySaveError}</span>}
            <button
              className="btn btn-submit expectation-save-btn"
              onClick={handleSaveCompany}
              disabled={companySaving}
            >
              {companySaveButtonLabel}
            </button>
          </div>
        </>
      );
    } else {
      let companyReadonlyText = t('memberDetail.expectation.noInput');
      if (expectation != null && expectation.companyExpectation) {
        companyReadonlyText = expectation.companyExpectation;
      }
      companySection = (
        <div className="expectation-detail-readonly">
          {companyReadonlyText}
        </div>
      );
    }

    expectationsTabContent = (
      <div className="expectation-detail-panel">
        <div className="expectation-detail-section">
          <h3 className="expectation-detail-title">{t('memberDetail.expectation.tlTitle')}</h3>
          {tlSection}
        </div>
        <div className="expectation-detail-section">
          <h3 className="expectation-detail-title">{t('memberDetail.expectation.companyTitle')}</h3>
          {companySection}
        </div>
      </div>
    );
  }

  let downloadButtonLabel = t('memberDetail.report.button');
  if (downloading) {
    downloadButtonLabel = t('memberDetail.report.downloading');
  }

  const resizeHandles: React.ReactNode[] = [];
  for (const dir of ['n', 's', 'e', 'w', 'nw', 'ne', 'sw', 'se'] as const) {
    resizeHandles.push(
      <div key={dir} className={`ifp-resize ifp-resize--${dir}`} onMouseDown={e => startResize(e, dir)} />,
    );
  }

  let interviewSaveButtonLabel = t('memberDetail.interview.saveButton');
  if (saving) {
    interviewSaveButtonLabel = t('memberDetail.interview.savingButton');
  }

  let panelToggleClassName = 'interview-float-btn';
  if (panelOpen) {
    panelToggleClassName += ' interview-float-btn--open';
  }
  let panelToggleLabel = t('memberDetail.interview.openButton');
  if (panelOpen) {
    panelToggleLabel = t('memberDetail.interview.closeButton');
  }

  return (
    <div className="team-page">
      <NavBar />
      <main className="team-main">
        <button className="page-back-btn" onClick={() => navigate(backPath)}>{t('memberDetail.backButton', { label: backLabel })}</button>
        <h1 className="page-title">{t('memberDetail.title')}</h1>
        {selectedInventory && (
          <p className="page-subtitle">{t('memberDetail.readOnly')}</p>
        )}

        {inventories.length === 0 ? (
          <div className="info-card"><p>{t('memberDetail.noData')}</p></div>
        ) : (
          <>
            <div className="history-selector-row">
              <label className="form-label">{t('memberDetail.yearLabel')}</label>
              <select
                className="select history-year-select"
                value={selectedIdValue}
                onChange={e => setSelectedId(Number(e.target.value))}
              >
                {yearOptions}
              </select>
            </div>

            {loading ? (
              <div className="loading">{t('loading')}</div>
            ) : (
              <>
                <div className="tab-bar">
                  {tabButtons}
                </div>

                {/* ── ITスキルタブ ── */}
                {activeTab === 'it-skills' && (
                  <div className="history-tab-content">
                    {itSkillDetails.length > 0 && (
                      <div className="history-filter-bar">
                        <input
                          className="history-filter-bar__input"
                          placeholder={t('memberDetail.filter.skillSearch')}
                          value={itSkillSearch}
                          onChange={e => setItSkillSearch(e.target.value)}
                        />
                        <select
                          className="history-filter-bar__select"
                          value={itSkillCategory1Filter}
                          onChange={e => { setItSkillCategory1Filter(e.target.value); setItSkillCategory2Filter(''); }}
                        >
                          <option value="">{t('memberDetail.filter.category1All')}</option>
                          {cat1FilterOptions}
                          {itSkillTree.customItems.length > 0 && (
                            <option value="__custom__">{t('memberDetail.filter.custom')}</option>
                          )}
                        </select>
                        {itSkillCat2Options.length > 0 && (
                          <select
                            className="history-filter-bar__select"
                            value={itSkillCategory2Filter}
                            onChange={e => setItSkillCategory2Filter(e.target.value)}
                          >
                            <option value="">{t('memberDetail.filter.category2All')}</option>
                            {cat2FilterOptions}
                          </select>
                        )}
                        {hasPrevYear && (
                          <select
                            className="history-filter-bar__select"
                            value={itSkillDiffFilter}
                            onChange={e => setItSkillDiffFilter(e.target.value as '' | 'up' | 'down' | 'new')}
                          >
                            <option value="">{t('memberDetail.filter.diffAll')}</option>
                            <option value="up">{t('memberDetail.filter.diffUp')}</option>
                            <option value="down">{t('memberDetail.filter.diffDown')}</option>
                            <option value="new">{t('memberDetail.filter.diffNew')}</option>
                          </select>
                        )}
                        <span className="history-result-count">{filteredItSkillCount}件</span>
                      </div>
                    )}
                    <StickyHorizontalScroll className="comparison-table-wrapper">
                      <table className="comparison-table">
                        <thead>
                          <tr>
                            <th>{t('memberDetail.table.skillName')}</th>
                            {hasPrevYear && <th>{t('memberDetail.table.prevYear')}</th>}
                            <th>{t('memberDetail.table.currentYear')}</th>
                            {hasPrevYear && <th>{t('memberDetail.table.diff')}</th>}
                            <th>{t('memberDetail.table.remarks')}</th>
                            {isTlOrAdmin && <th className="interview-note-th">{t('memberDetail.table.interviewNote')}</th>}
                          </tr>
                        </thead>
                        <tbody>
                          {itSkillTableBody}
                        </tbody>
                      </table>
                    </StickyHorizontalScroll>
                  </div>
                )}

                {/* ── 資格タブ ── */}
                {activeTab === 'qualifications' && (
                  <div className="history-tab-content">
                    {qualificationsTabContent}
                  </div>
                )}

                {/* ── セミナータブ ── */}
                {activeTab === 'seminars' && (
                  <div className="history-tab-content">
                    {seminarsTabContent}
                  </div>
                )}

                {/* ── 目標タブ ── */}
                {activeTab === 'goals' && (
                  <div className="history-tab-content">
                    {goalsTabContent}
                  </div>
                )}

                {/* ── AI分析タブ ── */}
                {activeTab === 'ai-analysis' && isTlOrAdmin && (
                  <div className="history-tab-content">
                    {aiAnalysisTabContent}
                  </div>
                )}

                {/* ── 期待タブ ── */}
                {activeTab === 'expectations' && isTlOrAdmin && (
                  <div className="history-tab-content">
                    {expectationsTabContent}
                  </div>
                )}

              </>
            )}
          </>
        )}
      </main>

      {/* ── 印刷ボタン ── */}
      {selectedId && (
        <button
          className="report-download-btn"
          onClick={handleDownloadReport}
          disabled={downloading}
        >
          {downloadButtonLabel}
        </button>
      )}

      {/* ── フローティング面談メモパネル（TL/ADMIN のみ） ── */}
      {isTlOrAdmin && selectedId && (
        <>
          {panelOpen && (
            <div
              className="interview-float-panel"
              style={{ left: panelRect.left, top: panelRect.top, width: panelRect.width, height: panelRect.height }}
            >
              {resizeHandles}
              <div className="interview-float-panel__header" onMouseDown={startMove}>
                <span className="interview-float-panel__title">{t('memberDetail.interview.panelTitle')}</span>
                <button
                  className="interview-float-panel__close"
                  onMouseDown={e => e.stopPropagation()}
                  onClick={() => setPanelOpen(false)}
                  aria-label={t('memberDetail.interview.closeLabel')}
                >×</button>
              </div>
              <div className="interview-float-panel__body">
                {prevYearInterview && (
                  <div className="interview-float-prev-section">
                    <div className="interview-float-prev-label">{t('memberDetail.interview.prevYearLabel')}</div>
                    <div className="interview-float-prev-text">
                      {prevYearInterview.generalNote || t('memberDetail.interview.noNote')}
                    </div>
                  </div>
                )}
                <textarea
                  className="interview-float-panel__textarea"
                  value={generalNote}
                  onChange={e => setGeneralNote(e.target.value)}
                  placeholder={t('memberDetail.interview.memoPlaceholder')}
                />
                {saveError && <p className="error-text">{saveError}</p>}
                <div className="interview-float-panel__actions">
                  {interview && !saving && !saveError && (
                    <span className="interview-panel__saved-label">{t('memberDetail.interview.savedLabel')}</span>
                  )}
                  <button
                    className="btn btn-submit interview-float-save-btn"
                    onClick={handleSave}
                    disabled={saving}
                  >
                    {interviewSaveButtonLabel}
                  </button>
                </div>
              </div>
            </div>
          )}
          <button
            className={panelToggleClassName}
            onClick={() => setPanelOpen(v => !v)}
          >
            {panelToggleLabel}
          </button>
        </>
      )}
    </div>
  );
}
