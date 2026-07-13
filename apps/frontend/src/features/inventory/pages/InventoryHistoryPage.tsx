/*******************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * 棚卸履歴ページ。年度を選択してITスキル・資格・セミナー・目標・AI分析を参照できる。
 * 現在年度の ITスキル備考は編集が可能。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  getMyInventories, getItSkillDetails, getQualificationDetails,
  getSeminarDetails, getGoals, getComparison, patchItSkillRemarks, getGoalReview,
} from '../api/inventoryApi';
import { getMyAiAnalyses } from '../../ai/api/aiAnalysisApi';
import { getItSkills, getFiscalYears } from '../../../shared/api/masterApi';
import type { FiscalYear } from '../../../shared/types/master';
import type {
  InventorySummary, ItSkillDetailItem, QualificationDetailItem,
  SeminarDetailItem, GoalItem, ComparisonResponse, ComparisonItem, GoalReviewItem,
} from '../types/index';
import type { AiAnalysis } from '../../ai/types/index';
import type { ItSkill } from '../../../shared/types/master';
import NavBar from '../../../app/layouts/NavBar';
import AiAnalysisCard from '../../ai/components/AiAnalysisCard';
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

/** ITスキル明細を大分類・中分類にグルーピングした際の中分類単位のグループ。 */
interface ItSkillCat2Group {
  cat2: string;
  items: ItSkillDetailItem[];
}

/** ITスキル明細を大分類・中分類にグルーピングした際の大分類単位のグループ。 */
interface ItSkillCat1Group {
  cat1: string;
  cat2Groups: ItSkillCat2Group[];
}

/**
 * 比較関数を使って配列を昇順に並び替える（挿入ソート）。
 *
 * Array.prototype.sort の代替として使用する。元の配列は変更せず新しい配列を返す。
 */
function sortByComparator<T>(items: T[], compare: (a: T, b: T) => number): T[] {
  const result: T[] = [...items];
  for (let i = 1; i < result.length; i++) {
    const current = result[i];
    let j = i - 1;
    while (j >= 0 && compare(result[j], current) > 0) {
      result[j + 1] = result[j];
      j -= 1;
    }
    result[j + 1] = current;
  }
  return result;
}

/** 文字列配列を辞書順に並び替える（Array.prototype.sort の代替）。 */
function sortStrings(items: string[]): string[] {
  return sortByComparator(items, (a, b) => {
    if (a < b) return -1;
    if (a > b) return 1;
    return 0;
  });
}

/**
 * 目標のスキル種別（ITスキル・資格・セミナー・カスタム）に応じた表示名を取得する。
 *
 * どの名前も設定されていない場合は fallback を返す。
 */
function getGoalDisplayName(goal: GoalItem, fallback: string): string {
  if (goal.itSkillName != null) return goal.itSkillName;
  if (goal.qualificationName != null) return goal.qualificationName;
  if (goal.adSeminarName != null) return goal.adSeminarName;
  if (goal.customName != null) return goal.customName;
  return fallback;
}

/** 目標カテゴリコードを表示用の翻訳キーに変換する。未定義のコードはそのまま返す。 */
function getGoalCategoryKey(goalCategory: string): string {
  if (GOAL_CATEGORY_KEY[goalCategory] != null) return GOAL_CATEGORY_KEY[goalCategory];
  return goalCategory;
}

/** 達成状況コードを表示用の翻訳キーに変換する。未定義のコードはそのまま返す。 */
function getAchievementKey(achievementStatus: string): string {
  if (ACHIEVEMENT_KEY[achievementStatus] != null) return ACHIEVEMENT_KEY[achievementStatus];
  return achievementStatus;
}

/**
 * ITスキルの前年比較差分を表示するセルコンポーネント。
 *
 * 前年データがない場合は null を返し、差分に応じて上昇・下降・新規のスタイルで表示する。
 */
function DiffCell({ diff, hasPrevYear }: { diff: number | null | undefined; hasPrevYear: boolean }) {
  const { t } = useTranslation('inventory');
  if (!hasPrevYear) return null;
  if (diff === null || diff === undefined) return <span className="diff-new">{t('historyPage.diffNew')}</span>;
  if (diff > 0) return <span className="diff-up">↑ +{diff}</span>;
  if (diff < 0) return <span className="diff-down">↓ {diff}</span>;
  return <span>—</span>;
}

/**
 * 棚卸履歴ページ。
 *
 * 年度セレクトボックスで表示年度を切り替え、ITスキル・資格・セミナー・目標・AI分析の
 * 各タブで棚卸履歴を参照できる。
 */
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
    let label = status;
    if (map[status] != null) {
      label = map[status];
    }
    return label;
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

  // 初期表示時に棚卸一覧・AI分析一覧・会計年度一覧を取得する
  useEffect(() => {
    getMyInventories().then(res => {
      setInventories(res.data);
      if (res.data.length > 0) setSelectedId(res.data[0].id);
    });
    getMyAiAnalyses().then(res => setAiAnalyses(res.data)).catch(() => {});
    getFiscalYears().then(res => setFiscalYears(res.data)).catch(() => {});
  }, []);

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

  // 選択年度が変わったときに明細・目標・比較・AI分析などの詳細データを一括取得する
  useEffect(() => {
    if (!selectedId) return;
    setLoading(true);

    const selectedIndex = inventories.findIndex(inv => inv.id === selectedId);
    let nextInventoryId: number | null = null;
    if (selectedIndex > 0) {
      nextInventoryId = inventories[selectedIndex - 1].id;
    }
    let prevInventoryId: number | null = null;
    if (selectedIndex < inventories.length - 1) {
      prevInventoryId = inventories[selectedIndex + 1].id;
    }

    type GoalReviewResult = Awaited<ReturnType<typeof getGoalReview>> | null;
    type GoalsResult = Awaited<ReturnType<typeof getGoals>> | null;

    // 翌年度の棚卸から前年目標の振り返り、前年度の棚卸から前年目標一覧を取得する（存在しない場合は取得しない）
    let goalReviewPromise: Promise<GoalReviewResult> = Promise.resolve(null);
    if (nextInventoryId != null) {
      goalReviewPromise = getGoalReview(nextInventoryId).catch(() => null);
    }
    let prevGoalsPromise: Promise<GoalsResult> = Promise.resolve(null);
    if (prevInventoryId != null) {
      prevGoalsPromise = getGoals(prevInventoryId).catch(() => null);
    }
    let prevGoalReviewPromise: Promise<GoalReviewResult> = Promise.resolve(null);
    if (prevInventoryId != null) {
      prevGoalReviewPromise = getGoalReview(selectedId).catch(() => null);
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
      prevGoalReviewPromise,
    ]).then(([itRes, masterRes, qualRes, semRes, goalRes, compRes, reviewRes, prevGoalsRes, prevReviewRes]) => {
      const itItems = itRes.data.items;
      setItSkillDetails(itItems);
      setItSkillMaster(masterRes.data);
      setQualificationDetails(qualRes.data.items);
      setSeminarDetails(semRes.data.items);
      setGoals(goalRes.data.items);

      let comparisonData = null;
      if (compRes != null) {
        comparisonData = compRes.data;
      }
      setComparison(comparisonData);

      const remarks: Record<number, string> = {};
      for (const item of itItems) {
        let remarksValue = '';
        if (item.remarks != null) {
          remarksValue = item.remarks;
        }
        remarks[item.id] = remarksValue;
      }
      setEditingRemarks(remarks);

      const reviewMap = new Map<number, GoalReviewItem>();
      if (reviewRes != null) {
        for (const item of reviewRes.data.items) {
          reviewMap.set(item.prevGoalId, item);
        }
      }
      setGoalReviewMap(reviewMap);

      let prevGoalsItems: GoalItem[] = [];
      if (prevGoalsRes != null) {
        prevGoalsItems = prevGoalsRes.data.items;
      }
      setPrevGoals(prevGoalsItems);

      const prevRevMap = new Map<number, GoalReviewItem>();
      if (prevReviewRes != null) {
        for (const item of prevReviewRes.data.items) {
          prevRevMap.set(item.prevGoalId, item);
        }
      }
      setPrevGoalReviewMap(prevRevMap);
    }).finally(() => setLoading(false));
  }, [selectedId, inventories]);

  const itSkillTree = useMemo(() => {
    const skillMapEntries: [number, ItSkill][] = [];
    for (const s of itSkillMaster) {
      skillMapEntries.push([s.id, s]);
    }
    const skillMap = new Map(skillMapEntries);
    const map = new Map<string, Map<string, ItSkillDetailItem[]>>();
    const customItems: ItSkillDetailItem[] = [];

    // 大分類の並び順・中分類名・スキル自体の並び順の優先度でソートする
    const sortedDetails = sortByComparator([...itSkillDetails], (a, b) => {
      let idA = a.itSkillId;
      if (idA == null) idA = -1;
      let idB = b.itSkillId;
      if (idB == null) idB = -1;
      const ma = skillMap.get(idA);
      const mb = skillMap.get(idB);

      let sortOrderA = 0;
      if (ma != null) sortOrderA = ma.category1SortOrder;
      let sortOrderB = 0;
      if (mb != null) sortOrderB = mb.category1SortOrder;
      if (sortOrderA !== sortOrderB) return sortOrderA - sortOrderB;

      let cat2A = '';
      if (ma != null && ma.category2Name != null) cat2A = ma.category2Name;
      let cat2B = '';
      if (mb != null && mb.category2Name != null) cat2B = mb.category2Name;
      const cat2Compare = cat2A.localeCompare(cat2B);
      if (cat2Compare !== 0) return cat2Compare;

      let orderA = 0;
      if (ma != null) orderA = ma.sortOrder;
      let orderB = 0;
      if (mb != null) orderB = mb.sortOrder;
      return orderA - orderB;
    });

    for (const detail of sortedDetails) {
      if (detail.itSkillId === null) {
        customItems.push(detail);
      } else {
        const master = skillMap.get(detail.itSkillId);
        let cat1 = '未分類';
        if (master != null && master.category1Name != null) cat1 = master.category1Name;
        let cat2 = '';
        if (master != null && master.category2Name != null) cat2 = master.category2Name;
        if (!map.has(cat1)) map.set(cat1, new Map());
        const cat2Map = map.get(cat1)!;
        if (!cat2Map.has(cat2)) cat2Map.set(cat2, []);
        cat2Map.get(cat2)!.push(detail);
      }
    }

    const groups: ItSkillCat1Group[] = [];
    for (const [cat1, cat2Map] of map.entries()) {
      const cat2Groups: ItSkillCat2Group[] = [];
      for (const [cat2, items] of cat2Map.entries()) {
        cat2Groups.push({ cat2, items });
      }
      groups.push({ cat1, cat2Groups });
    }

    return { groups, customItems };
  }, [itSkillDetails, itSkillMaster]);

  const comparisonMap = useMemo(() => {
    if (!comparison) return new Map<number, ComparisonItem>();
    const entries: [number, ComparisonItem][] = [];
    for (const item of comparison.items) {
      entries.push([item.currentDetailId, item]);
    }
    return new Map(entries);
  }, [comparison]);

  let hasPrevYear = false;
  if (comparison != null) {
    hasPrevYear = comparison.hasPrevYear;
  }

  const itSkillCat2Options = useMemo(() => {
    let sourceGroups = itSkillTree.groups;
    if (itSkillCategory1Filter) {
      const matched: ItSkillCat1Group[] = [];
      for (const g of itSkillTree.groups) {
        if (g.cat1 === itSkillCategory1Filter) {
          matched.push(g);
        }
      }
      sourceGroups = matched;
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

    // 検索語・カテゴリ・前年比フィルターに一致する明細だけを残しつつツリー構造を組み直す
    const filteredGroups: ItSkillCat1Group[] = [];
    for (const g of itSkillTree.groups) {
      if (itSkillCategory1Filter && g.cat1 !== itSkillCategory1Filter) continue;

      const filteredCat2Groups: ItSkillCat2Group[] = [];
      for (const cg of g.cat2Groups) {
        if (itSkillCategory2Filter && cg.cat2 !== itSkillCategory2Filter) continue;

        const filteredItems: ItSkillDetailItem[] = [];
        for (const item of cg.items) {
          let matchesSearch = true;
          if (searchLower) {
            matchesSearch = false;
            if (item.itSkillName != null && item.itSkillName.toLowerCase().includes(searchLower)) {
              matchesSearch = true;
            }
          }
          if (!matchesSearch) continue;

          let matchesDiff = true;
          if (itSkillDiffFilter) {
            const comp = comparisonMap.get(item.id);
            if (itSkillDiffFilter === 'new' && comp !== undefined) matchesDiff = false;
            if (itSkillDiffFilter === 'up') {
              let diff = 0;
              if (comp != null && comp.diff != null) diff = comp.diff;
              if (comp === undefined || diff <= 0) matchesDiff = false;
            }
            if (itSkillDiffFilter === 'down') {
              let diff = 0;
              if (comp != null && comp.diff != null) diff = comp.diff;
              if (comp === undefined || diff >= 0) matchesDiff = false;
            }
          }
          if (!matchesDiff) continue;

          filteredItems.push(item);
        }

        if (filteredItems.length > 0) {
          filteredCat2Groups.push({ cat2: cg.cat2, items: filteredItems });
        }
      }

      if (filteredCat2Groups.length > 0) {
        filteredGroups.push({ cat1: g.cat1, cat2Groups: filteredCat2Groups });
      }
    }

    // カスタムスキルは「カスタム」カテゴリ選択時のみ、または他の絞込（カテゴリ・上昇/下降）が
    // 行われていないときだけ表示する業務ルール
    let showCustom = false;
    if (itSkillCategory1Filter === '__custom__') {
      showCustom = true;
    } else if (!itSkillCategory1Filter && !itSkillCategory2Filter && itSkillDiffFilter !== 'up' && itSkillDiffFilter !== 'down') {
      showCustom = true;
    }

    const filteredCustom: ItSkillDetailItem[] = [];
    if (showCustom) {
      for (const item of itSkillTree.customItems) {
        let matchesSearch = true;
        if (searchLower) {
          matchesSearch = false;
          if (item.customSkillName != null && item.customSkillName.toLowerCase().includes(searchLower)) {
            matchesSearch = true;
          }
        }
        if (matchesSearch) filteredCustom.push(item);
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
    const names = new Set<string>();
    for (const q of qualificationDetails) {
      if (q.qualificationCategoryName !== null) {
        names.add(q.qualificationCategoryName);
      }
    }
    return sortStrings(Array.from(names));
  }, [qualificationDetails]);

  const seminarAdCategories = useMemo(() => {
    const names = new Set<string>();
    for (const s of seminarDetails) {
      if (s.adSeminarId !== null && s.adSeminarCategoryName !== null) {
        names.add(s.adSeminarCategoryName);
      }
    }
    return sortStrings(Array.from(names));
  }, [seminarDetails]);

  const filteredQualifications = useMemo(() => {
    const searchLower = qualNameSearch.toLowerCase();
    const result: QualificationDetailItem[] = [];
    for (const q of qualificationDetails) {
      if (qualCategoryFilter) {
        if (qualCategoryFilter === '__custom__') {
          if (q.qualificationId !== null) continue;
        } else if (q.qualificationCategoryName !== qualCategoryFilter) {
          continue;
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
        let fy: FiscalYear | null = null;
        for (const f of fiscalYears) {
          if (String(f.id) === qualFiscalYearFilter) {
            fy = f;
            break;
          }
        }
        if (fy != null) {
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
        let fy: FiscalYear | null = null;
        for (const f of fiscalYears) {
          if (String(f.id) === seminarFiscalYearFilter) {
            fy = f;
            break;
          }
        }
        if (fy != null) {
          if (!s.attendedYearMonth) continue;
          const ym = s.attendedYearMonth.slice(0, 7);
          if (ym < fy.startDate.slice(0, 7) || ym > fy.endDate.slice(0, 7)) continue;
        }
      }
      result.push(s);
    }
    return result;
  }, [seminarDetails, seminarNameSearch, seminarTypeFilter, seminarCategoryFilter, seminarFiscalYearFilter, fiscalYears]);

  const filteredGoals = useMemo(() => {
    const searchLower = goalSearch.toLowerCase();
    const result: GoalItem[] = [];
    for (const g of goals) {
      if (goalCategoryFilter && g.goalCategory !== goalCategoryFilter) continue;
      if (searchLower) {
        const name = getGoalDisplayName(g, '').toLowerCase();
        if (!name.includes(searchLower)) continue;
      }
      result.push(g);
    }
    return result;
  }, [goals, goalSearch, goalCategoryFilter]);

  const filteredPrevGoals = useMemo(() => {
    const searchLower = goalSearch.toLowerCase();
    const result: GoalItem[] = [];
    for (const g of prevGoals) {
      if (goalCategoryFilter && g.goalCategory !== goalCategoryFilter) continue;
      if (searchLower) {
        const name = getGoalDisplayName(g, '').toLowerCase();
        if (!name.includes(searchLower)) continue;
      }
      result.push(g);
    }
    return result;
  }, [prevGoals, goalSearch, goalCategoryFilter]);

  // The first inventory in the list is the most recent (current year's)
  const isCurrentYear = inventories.length > 0 && selectedId === inventories[0].id;

  let itSkillColCount = 3;
  if (hasPrevYear) itSkillColCount += 2;
  if (isCurrentYear) itSkillColCount += 1;

  const handleSaveRemarks = async (detailId: number) => {
    if (!selectedId) return;
    setSavingId(detailId);
    try {
      let remarksValue = '';
      if (editingRemarks[detailId] != null) {
        remarksValue = editingRemarks[detailId];
      }
      await patchItSkillRemarks(selectedId, detailId, remarksValue);
    } finally {
      setSavingId(null);
    }
  };

  /** 通常のITスキル明細・カスタムITスキル明細の1行分を描画する（前年比較・備考編集を含む）。 */
  function renderItSkillRow(detail: ItSkillDetailItem, isCustom: boolean) {
    let comp: ComparisonItem | undefined;
    if (!isCustom) {
      comp = comparisonMap.get(detail.id);
    }

    let prevLevelCell = '—';
    if (comp != null && comp.prevLevelValue != null) {
      prevLevelCell = String(comp.prevLevelValue);
    }

    let diffValue: number | null | undefined = null;
    if (comp != null) {
      diffValue = comp.diff;
    }

    let nameCell: React.ReactNode = detail.itSkillName;
    if (isCustom) {
      nameCell = <>{detail.customSkillName} ※</>;
    }

    let currentLevelCell: React.ReactNode = '—';
    if (!isCustom) {
      currentLevelCell = detail.levelValue;
    }

    let remarksCell: React.ReactNode;
    if (isCurrentYear) {
      let remarksValue = '';
      if (editingRemarks[detail.id] != null) {
        remarksValue = editingRemarks[detail.id];
      }
      remarksCell = (
        <textarea
          className="remarks-input"
          rows={2}
          value={remarksValue}
          onChange={e => setEditingRemarks(prev => ({
            ...prev, [detail.id]: e.target.value,
          }))}
        />
      );
    } else {
      let displayRemarks = '—';
      if (detail.remarks) displayRemarks = detail.remarks;
      remarksCell = displayRemarks;
    }

    let saveButtonLabel = t('inventoryPage.saveButton');
    if (savingId === detail.id) saveButtonLabel = '...';

    return (
      <tr key={detail.id}>
        <td>{nameCell}</td>
        {hasPrevYear && <td>{prevLevelCell}</td>}
        <td>{currentLevelCell}</td>
        {hasPrevYear && (
          <td className="diff-cell">
            <DiffCell diff={diffValue} hasPrevYear={hasPrevYear} />
          </td>
        )}
        <td>{remarksCell}</td>
        {isCurrentYear && (
          <td>
            <button
              className="btn btn-sm"
              onClick={() => handleSaveRemarks(detail.id)}
              disabled={savingId === detail.id}
            >
              {saveButtonLabel}
            </button>
          </td>
        )}
      </tr>
    );
  }

  if (inventories.length === 0) {
    return (
      <div className="history-page">
        <NavBar />
        <main className="history-main">
          <button className="page-back-btn" onClick={() => navigate('/')}>{t('historyPage.backButton')}</button>
          <h1 className="page-title">{t('historyPage.title')}</h1>
          <div className="info-card"><p>{t('historyPage.noData')}</p></div>
        </main>
      </div>
    );
  }

  const yearOptionElements: React.ReactNode[] = [];
  for (const inv of inventories) {
    yearOptionElements.push(
      <option key={inv.id} value={inv.id}>
        {inv.fiscalYear.name}（{getStatusLabel(inv.status)}）
      </option>
    );
  }

  let selectedIdValue: number | string = '';
  if (selectedId != null) {
    selectedIdValue = selectedId;
  }

  let tabSection: React.ReactNode = <div className="loading">{t('loading')}</div>;
  if (!loading) {
    const tabKeys = Object.keys(TAB_LABELS) as TabKey[];
    const tabButtonElements: React.ReactNode[] = [];
    for (const tab of tabKeys) {
      let tabClassName = 'tab-btn';
      if (activeTab === tab) tabClassName = 'tab-btn active';
      tabButtonElements.push(
        <button
          key={tab}
          className={tabClassName}
          onClick={() => setActiveTab(tab)}
        >
          {TAB_LABELS[tab]}
        </button>
      );
    }

    const itSkillCategory1OptionElements: React.ReactNode[] = [];
    for (const g of itSkillTree.groups) {
      itSkillCategory1OptionElements.push(<option key={g.cat1} value={g.cat1}>{g.cat1}</option>);
    }

    const itSkillCategory2OptionElements: React.ReactNode[] = [];
    for (const cat2 of itSkillCat2Options) {
      itSkillCategory2OptionElements.push(<option key={cat2} value={cat2}>{cat2}</option>);
    }

    const itSkillTableRows: React.ReactNode[] = [];
    if (itSkillDetails.length === 0) {
      itSkillTableRows.push(
        <tr key="no-data">
          <td colSpan={itSkillColCount} className="no-data-cell">
            {t('historyPage.noDataCell.itSkills')}
          </td>
        </tr>
      );
    } else if (filteredItSkillCount === 0) {
      itSkillTableRows.push(
        <tr key="no-match">
          <td colSpan={itSkillColCount} className="no-data-cell">
            {t('historyPage.noDataCell.noMatchSkills')}
          </td>
        </tr>
      );
    } else {
      for (const group of filteredItSkillTree.groups) {
        itSkillTableRows.push(
          <tr key={`cat1-${group.cat1}`} className="scoring-cat1-row">
            <td colSpan={itSkillColCount}>{group.cat1}</td>
          </tr>
        );
        for (const cat2Group of group.cat2Groups) {
          if (cat2Group.cat2) {
            itSkillTableRows.push(
              <tr key={`cat2-${group.cat1}-${cat2Group.cat2}`} className="scoring-cat2-row">
                <td colSpan={itSkillColCount}>{cat2Group.cat2}</td>
              </tr>
            );
          }
          for (const detail of cat2Group.items) {
            itSkillTableRows.push(renderItSkillRow(detail, false));
          }
        }
      }
      if (filteredItSkillTree.customItems.length > 0) {
        itSkillTableRows.push(
          <tr key="custom-header" className="scoring-cat1-row">
            <td colSpan={itSkillColCount}>{t('historyPage.customSkillLabel')}</td>
          </tr>
        );
        for (const detail of filteredItSkillTree.customItems) {
          itSkillTableRows.push(renderItSkillRow(detail, true));
        }
      }
    }

    const qualCategoryOptionElements: React.ReactNode[] = [];
    for (const cat of qualCategories) {
      qualCategoryOptionElements.push(<option key={cat} value={cat}>{cat}</option>);
    }

    let hasCustomQualification = false;
    for (const q of qualificationDetails) {
      if (q.qualificationId === null) {
        hasCustomQualification = true;
        break;
      }
    }

    const qualFiscalYearOptionElements: React.ReactNode[] = [];
    for (const fy of fiscalYears) {
      qualFiscalYearOptionElements.push(<option key={fy.id} value={String(fy.id)}>{fy.name}</option>);
    }

    const qualificationTableRows: React.ReactNode[] = [];
    if (filteredQualifications.length === 0) {
      qualificationTableRows.push(
        <tr key="no-match"><td colSpan={4} className="no-data-cell">{t('historyPage.noDataCell.noMatchQuals')}</td></tr>
      );
    } else {
      for (const q of filteredQualifications) {
        let categoryCell = '—';
        if (q.qualificationCategoryName != null) categoryCell = q.qualificationCategoryName;

        let nameCell = '—';
        if (q.qualificationName != null) {
          nameCell = q.qualificationName;
        } else if (q.customQualificationName != null) {
          nameCell = q.customQualificationName;
        }

        let acquiredCell = '—';
        if (q.acquiredYearMonth != null) acquiredCell = q.acquiredYearMonth.slice(0, 7);

        let remarksCell = '—';
        if (q.remarks) remarksCell = q.remarks;

        qualificationTableRows.push(
          <tr key={q.id}>
            <td>{categoryCell}</td>
            <td>
              {nameCell}
              {q.qualificationId === null && ' ※'}
            </td>
            <td>{acquiredCell}</td>
            <td>{remarksCell}</td>
          </tr>
        );
      }
    }

    const seminarAdCategoryOptionElements: React.ReactNode[] = [];
    for (const cat of seminarAdCategories) {
      seminarAdCategoryOptionElements.push(<option key={cat} value={cat}>{cat}</option>);
    }

    const seminarFiscalYearOptionElements: React.ReactNode[] = [];
    for (const fy of fiscalYears) {
      seminarFiscalYearOptionElements.push(<option key={fy.id} value={String(fy.id)}>{fy.name}</option>);
    }

    const seminarTableRows: React.ReactNode[] = [];
    if (filteredSeminars.length === 0) {
      seminarTableRows.push(
        <tr key="no-match"><td colSpan={5} className="no-data-cell">{t('historyPage.noDataCell.noMatchSeminars')}</td></tr>
      );
    } else {
      for (const s of filteredSeminars) {
        let typeCell = t('historyPage.filter.seminarTypeFree');
        if (s.adSeminarId !== null) typeCell = 'AD';

        let categoryCell = '—';
        if (s.adSeminarId !== null && s.adSeminarCategoryName != null) categoryCell = s.adSeminarCategoryName;

        let nameCell = '—';
        if (s.adSeminarName != null) {
          nameCell = s.adSeminarName;
        } else if (s.seminarName != null) {
          nameCell = s.seminarName;
        }

        let attendedCell = '—';
        if (s.attendedYearMonth != null) attendedCell = s.attendedYearMonth.slice(0, 7);

        let remarksCell = '—';
        if (s.remarks) remarksCell = s.remarks;

        seminarTableRows.push(
          <tr key={s.id}>
            <td>{typeCell}</td>
            <td>{categoryCell}</td>
            <td>{nameCell}</td>
            <td>{attendedCell}</td>
            <td>{remarksCell}</td>
          </tr>
        );
      }
    }

    // 選択中年度の会計年度IDに一致するAI分析結果を探す（存在しなければ未実施として扱う）
    let currentAiAnalysis: AiAnalysis | null = null;
    for (const a of aiAnalyses) {
      let matchesSelectedYear = false;
      for (const inv of inventories) {
        if (inv.id === selectedId && a.fiscalYearId === inv.fiscalYear.id) {
          matchesSelectedYear = true;
          break;
        }
      }
      if (matchesSelectedYear) {
        currentAiAnalysis = a;
        break;
      }
    }

    let aiAnalysisContent: React.ReactNode = <p className="no-data">{t('historyPage.noDataCell.aiAnalysis')}</p>;
    if (currentAiAnalysis != null) {
      aiAnalysisContent = <AiAnalysisCard analysis={currentAiAnalysis} />;
    }

    const prevGoalTableRows: React.ReactNode[] = [];
    for (const g of filteredPrevGoals) {
      const review = prevGoalReviewMap.get(g.id);

      let achievementCell = '—';
      if (review != null && review.achievementStatus) {
        achievementCell = t(getAchievementKey(review.achievementStatus));
      }

      let reviewNoteCell = '—';
      if (review != null && review.reviewNote) reviewNoteCell = review.reviewNote;

      let reasonCell = '—';
      if (g.reason) reasonCell = g.reason;

      prevGoalTableRows.push(
        <tr key={g.id}>
          <td><span className="goal-category-badge">{t(getGoalCategoryKey(g.goalCategory))}</span></td>
          <td>{getGoalDisplayName(g, '—')}</td>
          <td>{g.targetPeriod.slice(0, 7)}</td>
          <td>{reasonCell}</td>
          <td>{achievementCell}</td>
          <td>{reviewNoteCell}</td>
        </tr>
      );
    }

    const goalTableRows: React.ReactNode[] = [];
    for (const g of filteredGoals) {
      const review = goalReviewMap.get(g.id);

      let achievementCell = '—';
      if (review != null && review.achievementStatus) {
        achievementCell = t(getAchievementKey(review.achievementStatus));
      }

      let reviewNoteCell = '—';
      if (review != null && review.reviewNote) reviewNoteCell = review.reviewNote;

      let reasonCell = '—';
      if (g.reason) reasonCell = g.reason;

      goalTableRows.push(
        <tr key={g.id}>
          <td><span className="goal-category-badge">{t(getGoalCategoryKey(g.goalCategory))}</span></td>
          <td>{getGoalDisplayName(g, '—')}</td>
          <td>{g.targetPeriod.slice(0, 7)}</td>
          <td>{reasonCell}</td>
          <td>{achievementCell}</td>
          <td>{reviewNoteCell}</td>
        </tr>
      );
    }

    // 資格タブ本体（データが1件もない場合は案内文のみを表示する）
    let qualificationsTabBody: React.ReactNode = <p className="no-data">{t('historyPage.noDataCell.qualifications')}</p>;
    if (qualificationDetails.length > 0) {
      qualificationsTabBody = (
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
              {qualCategoryOptionElements}
              {hasCustomQualification && (
                <option value="__custom__">{t('historyPage.filter.custom')}</option>
              )}
            </select>
            <select
              className="history-filter-bar__select"
              value={qualFiscalYearFilter}
              onChange={e => setQualFiscalYearFilter(e.target.value)}
            >
              <option value="">{t('historyPage.filter.qualFiscalYearAll')}</option>
              {qualFiscalYearOptionElements}
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
                {qualificationTableRows}
              </tbody>
            </table>
          </StickyHorizontalScroll>
        </>
      );
    }

    // セミナータブ本体（データが1件もない場合は案内文のみを表示する）
    let seminarsTabBody: React.ReactNode = <p className="no-data">{t('historyPage.noDataCell.seminars')}</p>;
    if (seminarDetails.length > 0) {
      seminarsTabBody = (
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
                {seminarAdCategoryOptionElements}
              </select>
            )}
            <select
              className="history-filter-bar__select"
              value={seminarFiscalYearFilter}
              onChange={e => setSeminarFiscalYearFilter(e.target.value)}
            >
              <option value="">{t('historyPage.filter.seminarFiscalYearAll')}</option>
              {seminarFiscalYearOptionElements}
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
                {seminarTableRows}
              </tbody>
            </table>
          </StickyHorizontalScroll>
        </>
      );
    }

    // 前年度目標セクションの本体（絞込結果が0件の場合は案内文のみを表示する）
    let prevGoalSectionBody: React.ReactNode = <p className="no-data">{t('historyPage.noDataCell.noMatchGoals')}</p>;
    if (filteredPrevGoals.length > 0) {
      prevGoalSectionBody = (
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
              {prevGoalTableRows}
            </tbody>
          </table>
        </StickyHorizontalScroll>
      );
    }

    // 当年度目標セクションの本体（絞込結果が0件の場合は案内文のみを表示する）
    let currentGoalSectionBody: React.ReactNode = <p className="no-data">{t('historyPage.noDataCell.noMatchGoals')}</p>;
    if (filteredGoals.length > 0) {
      currentGoalSectionBody = (
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
              {goalTableRows}
            </tbody>
          </table>
        </StickyHorizontalScroll>
      );
    }

    // 目標タブ本体（前年度・当年度とも目標が1件もない場合は案内文のみを表示する）
    let goalsTabBody: React.ReactNode = <p className="no-data">{t('historyPage.noDataCell.goals')}</p>;
    if (prevGoals.length > 0 || goals.length > 0) {
      goalsTabBody = (
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
              {prevGoalSectionBody}
            </div>
          )}
          {goals.length > 0 && (
            <div className="history-goal-section">
              <h3 className="history-goal-title">{t('historyPage.goalSection.currentYear')}</h3>
              {currentGoalSectionBody}
            </div>
          )}
        </>
      );
    }

    tabSection = (
      <>
        <div className="tab-bar">
          {tabButtonElements}
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
                  {itSkillCategory1OptionElements}
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
                    {itSkillCategory2OptionElements}
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
                  {itSkillTableRows}
                </tbody>
              </table>
            </StickyHorizontalScroll>
          </div>
        )}

        {/* ── 資格タブ ── */}
        {activeTab === 'qualifications' && (
          <div className="history-tab-content">
            {qualificationsTabBody}
          </div>
        )}

        {/* ── セミナータブ ── */}
        {activeTab === 'seminars' && (
          <div className="history-tab-content">
            {seminarsTabBody}
          </div>
        )}

        {/* ── AI分析タブ ── */}
        {activeTab === 'ai-analysis' && (
          <div className="history-tab-content">
            {aiAnalysisContent}
          </div>
        )}

        {/* ── 目標タブ ── */}
        {activeTab === 'goals' && (
          <div className="history-tab-content">
            {goalsTabBody}
          </div>
        )}
      </>
    );
  }

  return (
    <div className="history-page">
      <NavBar />
      <main className="history-main">
        <button className="page-back-btn" onClick={() => navigate('/')}>{t('historyPage.backButton')}</button>
        <h1 className="page-title">{t('historyPage.title')}</h1>

        <div className="history-selector-row">
          <label className="form-label">{t('historyPage.yearLabel')}</label>
          <select
            className="select history-year-select"
            value={selectedIdValue}
            onChange={e => setSelectedId(Number(e.target.value))}
          >
            {yearOptionElements}
          </select>
        </div>

        {tabSection}
      </main>
    </div>
  );
}
