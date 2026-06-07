import { useEffect, useRef, useState, useMemo, Fragment } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
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
  SeminarDetailItem, GoalItem, ComparisonResponse, GoalReviewItem,
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

function DiffCell({ diff, hasPrevYear }: { diff: number | null | undefined; hasPrevYear: boolean }) {
  const { t } = useTranslation('user');
  if (!hasPrevYear) return null;
  if (diff === null || diff === undefined) return <span className="diff-new">{t('diffNew')}</span>;
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
  const { t } = useTranslation('user');
  const userIdNum = Number(userId);
  const backPath: string = (location.state as { from?: string } | null)?.from ?? '/team';
  const backLabel: string = (location.state as { fromLabel?: string } | null)?.fromLabel ?? t('memberDetail.defaultBackLabel');

  const isTlOrAdmin = user?.role === 'TL' || user?.role === 'ADMIN';

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
    getMemberInventories(userIdNum).then(res => {
      setInventories(res.data);
      if (res.data.length > 0) setSelectedId(res.data[0].id);
    });
    getFiscalYears().then(res => setFiscalYears(res.data)).catch(() => {});
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
      setSaveError(t('memberDetail.interview.saveFailed'));
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
                value={selectedId ?? ''}
                onChange={e => setSelectedId(Number(e.target.value))}
              >
                {inventories.map(inv => (
                  <option key={inv.id} value={inv.id}>
                    {inv.fiscalYear.name}（{t(STATUS_KEY[inv.status] ?? inv.status)}）
                  </option>
                ))}
              </select>
            </div>

            {loading ? (
              <div className="loading">{t('loading')}</div>
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
                          {itSkillTree.groups.map(({ cat1 }) => (
                            <option key={cat1} value={cat1}>{cat1}</option>
                          ))}
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
                          {itSkillDetails.length === 0 ? (
                            <tr>
                              <td colSpan={itSkillColCount} className="no-data-cell">
                                {t('memberDetail.noDataCell.itSkills')}
                              </td>
                            </tr>
                          ) : filteredItSkillCount === 0 ? (
                            <tr>
                              <td colSpan={itSkillColCount} className="no-data-cell">
                                {t('memberDetail.noDataCell.noMatchSkills')}
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
                                            <td>{detail.remarks || '—'}</td>
                                            {isTlOrAdmin && renderDetailNoteCell('IT_SKILL', detail.itSkillId ?? detail.id)}
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
                                    <td colSpan={itSkillColCount}>{t('customSkillLabel')}</td>
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
                      <p className="no-data">{t('memberDetail.noDataCell.qualifications')}</p>
                    ) : (
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
                            {qualCategories.map(cat => (
                              <option key={cat} value={cat}>{cat}</option>
                            ))}
                            {qualificationDetails.some(q => q.qualificationId === null) && (
                              <option value="__custom__">{t('memberDetail.filter.custom')}</option>
                            )}
                          </select>
                          <select
                            className="history-filter-bar__select"
                            value={qualFiscalYearFilter}
                            onChange={e => setQualFiscalYearFilter(e.target.value)}
                          >
                            <option value="">{t('memberDetail.filter.qualFiscalYearAll')}</option>
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
                                <th>{t('memberDetail.table.category')}</th>
                                <th>{t('memberDetail.table.qualName')}</th>
                                <th>{t('memberDetail.table.acquiredYearMonth')}</th>
                                <th>{t('memberDetail.table.remarks')}</th>
                                {isTlOrAdmin && <th className="interview-note-th">{t('memberDetail.table.interviewNote')}</th>}
                              </tr>
                            </thead>
                            <tbody>
                              {filteredQualifications.length === 0 ? (
                                <tr><td colSpan={isTlOrAdmin ? 5 : 4} className="no-data-cell">{t('memberDetail.noDataCell.noMatchQuals')}</td></tr>
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
                                    {isTlOrAdmin && renderDetailNoteCell('QUALIFICATION', q.qualificationId ?? q.id)}
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
                      <p className="no-data">{t('memberDetail.noDataCell.seminars')}</p>
                    ) : (
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
                            <option value="">{t('memberDetail.filter.seminarFiscalYearAll')}</option>
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
                                <th>{t('memberDetail.table.seminarType')}</th>
                                <th>{t('memberDetail.table.category')}</th>
                                <th>{t('memberDetail.table.seminarName')}</th>
                                <th>{t('memberDetail.table.attendedYearMonth')}</th>
                                <th>{t('memberDetail.table.remarks')}</th>
                                {isTlOrAdmin && <th className="interview-note-th">{t('memberDetail.table.interviewNote')}</th>}
                              </tr>
                            </thead>
                            <tbody>
                              {filteredSeminars.length === 0 ? (
                                <tr><td colSpan={isTlOrAdmin ? 6 : 5} className="no-data-cell">{t('memberDetail.noDataCell.noMatchSeminars')}</td></tr>
                              ) : (
                                filteredSeminars.map(s => (
                                  <tr key={s.id}>
                                    <td>{s.adSeminarId !== null ? 'AD' : t('memberDetail.filter.seminarTypeFree')}</td>
                                    <td>{s.adSeminarId !== null ? (s.adSeminarCategoryName ?? '—') : '—'}</td>
                                    <td>{s.adSeminarName ?? s.seminarName ?? '—'}</td>
                                    <td>{s.attendedYearMonth?.slice(0, 7) ?? '—'}</td>
                                    <td>{s.remarks || '—'}</td>
                                    {isTlOrAdmin && renderDetailNoteCell('SEMINAR', s.id)}
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

                {/* ── 目標タブ ── */}
                {activeTab === 'goals' && (
                  <div className="history-tab-content">
                    {prevGoals.length === 0 && goals.length === 0 ? (
                      <p className="no-data">{t('memberDetail.noDataCell.goals')}</p>
                    ) : (
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
                        {prevGoals.length > 0 && (
                          <div className="history-goal-section">
                            <h3 className="history-goal-title">{t('memberDetail.goalSection.prevYear')}</h3>
                            {filteredPrevGoals.length === 0 ? (
                              <p className="no-data">{t('memberDetail.noDataCell.noMatchGoals')}</p>
                            ) : (
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
                                          {isTlOrAdmin && renderPrevGoalNoteCell(g)}
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
                            <h3 className="history-goal-title">{t('memberDetail.goalSection.currentYear')}</h3>
                            {filteredGoals.length === 0 ? (
                              <p className="no-data">{t('memberDetail.noDataCell.noMatchGoals')}</p>
                            ) : (
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
                                          {isTlOrAdmin && renderDetailNoteCell('GOAL', goalDetailId(g), false)}
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

                {/* ── AI分析タブ ── */}
                {activeTab === 'ai-analysis' && isTlOrAdmin && (
                  <div className="history-tab-content">
                    {!aiAnalysisLoaded ? (
                      <div className="loading">{t('loading')}</div>
                    ) : (() => {
                      const analysis = memberAiAnalyses.find(a => {
                        const inv = inventories.find(i => i.id === selectedId);
                        return inv && a.fiscalYearId === inv.fiscalYear.id;
                      });
                      if (!analysis) return <p className="no-data">{t('memberDetail.noDataCell.aiAnalysis')}</p>;
                      return <AiAnalysisCard analysis={analysis} />;
                    })()}
                  </div>
                )}

                {/* ── 期待タブ ── */}
                {activeTab === 'expectations' && isTlOrAdmin && (
                  <div className="history-tab-content">
                    {expLoading ? (
                      <div className="loading">{t('loading')}</div>
                    ) : (
                      <div className="expectation-detail-panel">
                        <div className="expectation-detail-section">
                          <h3 className="expectation-detail-title">{t('memberDetail.expectation.tlTitle')}</h3>
                          {user?.role === 'TL' ? (
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
                                  {tlSaving ? t('memberDetail.expectation.savingButton') : t('memberDetail.expectation.saveButton')}
                                </button>
                              </div>
                            </>
                          ) : (
                            <div className="expectation-detail-readonly">
                              {expectation?.tlExpectation || t('memberDetail.expectation.noInput')}
                            </div>
                          )}
                        </div>

                        <div className="expectation-detail-section">
                          <h3 className="expectation-detail-title">{t('memberDetail.expectation.companyTitle')}</h3>
                          {user?.role === 'ADMIN' ? (
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
                                  {companySaving ? t('memberDetail.expectation.savingButton') : t('memberDetail.expectation.saveButton')}
                                </button>
                              </div>
                            </>
                          ) : (
                            <div className="expectation-detail-readonly">
                              {expectation?.companyExpectation || t('memberDetail.expectation.noInput')}
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

      {/* ── 印刷ボタン ── */}
      {selectedId && (
        <button
          className="report-download-btn"
          onClick={handleDownloadReport}
          disabled={downloading}
        >
          {downloading ? t('memberDetail.report.downloading') : t('memberDetail.report.button')}
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
              {(['n','s','e','w','nw','ne','sw','se'] as const).map(dir => (
                <div key={dir} className={`ifp-resize ifp-resize--${dir}`} onMouseDown={e => startResize(e, dir)} />
              ))}
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
                    {saving ? t('memberDetail.interview.savingButton') : t('memberDetail.interview.saveButton')}
                  </button>
                </div>
              </div>
            </div>
          )}
          <button
            className={`interview-float-btn${panelOpen ? ' interview-float-btn--open' : ''}`}
            onClick={() => setPanelOpen(v => !v)}
          >
            {panelOpen ? t('memberDetail.interview.closeButton') : t('memberDetail.interview.openButton')}
          </button>
        </>
      )}
    </div>
  );
}
