/*******************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * 棚卸入力ページ。ITスキル・資格・セミナーのタブ切り替えで入力し、
 * 全 ITスキルの評価が完了したら棚卸を提出できる。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import { useEffect, useState, useCallback, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  getInventory, getMyInventories, saveItSkillDetails, saveQualificationDetails,
  saveSeminarDetails, submitInventory,
  getItSkillDetails, getQualificationDetails, getSeminarDetails,
} from '../api/inventoryApi';
import { getItSkills, getSkillLevels, getQualifications, getAdSeminars } from '../../../shared/api/masterApi';
import type { InventoryDetail } from '../types/index';
import type { ItSkill, SkillLevel, Qualification, AdSeminar } from '../../../shared/types/master';
import NavBar from '../../../app/layouts/NavBar';
import ConfirmDialog from '../../../shared/ui/ConfirmDialog';
import { useTranslation } from 'react-i18next';

type Tab = 'itSkill' | 'qualification' | 'seminar';

interface ItSkillEntry {
  id?: number | null;
  levelId: number | null;
  remarks: string;
}

interface CustomSkillRow {
  id?: number | null;
  customSkillName: string;
  levelId: number;
  remarks: string;
}

interface QualificationRow {
  id?: number | null;
  qualificationId?: number | null;
  customQualificationName?: string | null;
  acquiredYearMonth: string;
  remarks: string;
  isCustom: boolean;
}

interface SeminarRow {
  id?: number | null;
  adSeminarId?: number | null;
  seminarName: string;
  seminarCategoryId?: number | null;
  attendedYearMonth: string;
  remarks: string;
  isAd: boolean;
}

/** 配列の指定インデックスの要素だけを新しい値で差し替えた配列を返す。 */
function replaceAt<T>(list: T[], idx: number, updates: Partial<T>): T[] {
  const next: T[] = [];
  for (let i = 0; i < list.length; i++) {
    if (i === idx) {
      next.push({ ...list[i], ...updates });
    } else {
      next.push(list[i]);
    }
  }
  return next;
}

/** 配列から指定インデックスの要素を取り除いた配列を返す。 */
function removeAt<T>(list: T[], idx: number): T[] {
  const next: T[] = [];
  for (let i = 0; i < list.length; i++) {
    if (i !== idx) {
      next.push(list[i]);
    }
  }
  return next;
}

/** ITスキルを大分類の並び順 → 中分類名 → 表示順の優先順位で比較する。 */
function compareSkills(a: ItSkill, b: ItSkill): number {
  if (a.category1SortOrder !== b.category1SortOrder) {
    return a.category1SortOrder - b.category1SortOrder;
  }
  let aCat2 = '';
  if (a.category2Name != null) aCat2 = a.category2Name;
  let bCat2 = '';
  if (b.category2Name != null) bCat2 = b.category2Name;
  const cat2Compare = aCat2.localeCompare(bCat2);
  if (cat2Compare !== 0) {
    return cat2Compare;
  }
  return a.sortOrder - b.sortOrder;
}

/** 指定した資格IDを持つ行が既に一覧に存在するかを判定する。 */
function hasQualification(rows: QualificationRow[], qualificationId: number): boolean {
  let found = false;
  for (const r of rows) {
    if (r.qualificationId === qualificationId) {
      found = true;
      break;
    }
  }
  return found;
}

/** 指定した推奨セミナーIDを持つ行が既に一覧に存在するかを判定する。 */
function hasAdSeminar(rows: SeminarRow[], adSeminarId: number): boolean {
  let found = false;
  for (const r of rows) {
    if (r.adSeminarId === adSeminarId) {
      found = true;
      break;
    }
  }
  return found;
}

/** 資格マスタから指定IDの名称を検索する。見つからない場合は空文字を返す。 */
function findQualificationName(list: Qualification[], id: number | null | undefined): string {
  let name = '';
  for (const q of list) {
    if (q.id === id) {
      name = q.name;
      break;
    }
  }
  return name;
}

/** 推奨セミナーマスタから指定IDの名称を検索する。見つからない場合は空文字を返す。 */
function findAdSeminarName(list: AdSeminar[], id: number | null | undefined): string {
  let name = '';
  for (const a of list) {
    if (a.id === id) {
      name = a.name;
      break;
    }
  }
  return name;
}

/**
 * 棚卸入力ページ。
 *
 * ITスキル・資格・セミナーのタブを切り替えて棚卸データを入力する。
 * 全 ITスキルの評価が完了したら棚卸を提出でき、比較ページへ遷移する。
 */
export default function InventoryPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation('inventory');
  const inventoryId = Number(id);

  const [inventory, setInventory] = useState<InventoryDetail | null>(null);
  const [tab, setTab] = useState<Tab>('itSkill');
  const [isSaving, setIsSaving] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showSubmitConfirm, setShowSubmitConfirm] = useState(false);
  const [saveMessage, setSaveMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [validationAttempted, setValidationAttempted] = useState(false);
  const [qualValidationAttempted, setQualValidationAttempted] = useState(false);
  const [semValidationAttempted, setSemValidationAttempted] = useState(false);
  const [qualSaved, setQualSaved] = useState(true);
  const [semSaved, setSemSaved] = useState(true);

  const changeTab = (newTab: Tab) => {
    setTab(newTab);
    setErrorMessage('');
    localStorage.setItem(`inventory-tab-${inventoryId}`, newTab);
  };

  const [itSkills, setItSkills] = useState<ItSkill[]>([]);
  const [skillLevels, setSkillLevels] = useState<SkillLevel[]>([]);
  const [qualifications, setQualifications] = useState<Qualification[]>([]);
  const [adSeminars, setAdSeminars] = useState<AdSeminar[]>([]);

  const [itSkillEntries, setItSkillEntries] = useState<Record<number, ItSkillEntry>>({});
  const [customSkillRows, setCustomSkillRows] = useState<CustomSkillRow[]>([]);
  const [qualificationRows, setQualificationRows] = useState<QualificationRow[]>([]);
  const [seminarRows, setSeminarRows] = useState<SeminarRow[]>([]);

  const [openQualCats, setOpenQualCats] = useState<Set<string>>(new Set());
  const [openSemCats, setOpenSemCats] = useState<Set<string>>(new Set());
  const [qualSearch, setQualSearch] = useState('');
  const [semSearch, setSemSearch] = useState('');

  // 初期表示時に棚卸データとマスタ情報を一括取得し、前年度からのデータ引き継ぎも行う
  useEffect(() => {
    Promise.all([
      getInventory(inventoryId),
      getItSkills(true),
      getSkillLevels(true),
      getQualifications(true),
      getAdSeminars(true),
      getItSkillDetails(inventoryId),
      getQualificationDetails(inventoryId),
      getSeminarDetails(inventoryId),
    ]).then(async ([invRes, skillsRes, levelsRes, qualsRes, adsRes, itDetailsRes, qualDetailsRes, semDetailsRes]) => {
      setInventory(invRes.data);
      setItSkills(skillsRes.data);
      setSkillLevels(levelsRes.data);
      setQualifications(qualsRes.data);
      setAdSeminars(adsRes.data);

      const entries: Record<number, ItSkillEntry> = {};
      const customs: CustomSkillRow[] = [];
      for (const d of itDetailsRes.data.items) {
        let remarks = '';
        if (d.remarks != null) remarks = d.remarks;
        if (d.itSkillId != null) {
          entries[d.itSkillId] = { id: d.id, levelId: d.skillLevelId, remarks };
        } else {
          let customSkillName = '';
          if (d.customSkillName != null) customSkillName = d.customSkillName;
          customs.push({ id: d.id, customSkillName, levelId: d.skillLevelId, remarks });
        }
      }
      setItSkillEntries(entries);
      setCustomSkillRows(customs);

      const currentQuals = qualDetailsRes.data.items;
      const currentSems = semDetailsRes.data.items;

      const needQualInherit = currentQuals.length === 0;
      const needSemInherit = currentSems.length === 0;

      let inheritedQuals = currentQuals;
      let inheritedSems = currentSems;

      if (needQualInherit || needSemInherit) {
        const allInvRes = await getMyInventories();
        const allInvs = allInvRes.data;
        const currentIndex = allInvs.findIndex(i => i.id === inventoryId);
        if (currentIndex >= 0 && currentIndex + 1 < allInvs.length) {
          const prevId = allInvs[currentIndex + 1].id;

          type QualDetailsResult = Awaited<ReturnType<typeof getQualificationDetails>> | null;
          let qualPromise: Promise<QualDetailsResult> = Promise.resolve(null);
          if (needQualInherit) qualPromise = getQualificationDetails(prevId);

          type SemDetailsResult = Awaited<ReturnType<typeof getSeminarDetails>> | null;
          let semPromise: Promise<SemDetailsResult> = Promise.resolve(null);
          if (needSemInherit) semPromise = getSeminarDetails(prevId);

          const [prevQualRes, prevSemRes] = await Promise.all([qualPromise, semPromise]);
          if (prevQualRes && prevQualRes.data.items.length > 0) inheritedQuals = prevQualRes.data.items;
          if (prevSemRes && prevSemRes.data.items.length > 0) inheritedSems = prevSemRes.data.items;
        }
      }

      const qualRows: QualificationRow[] = [];
      for (const d of inheritedQuals) {
        let rowId: number | undefined = d.id;
        if (needQualInherit) rowId = undefined;
        let acquiredYearMonth = '';
        if (d.acquiredYearMonth != null) acquiredYearMonth = d.acquiredYearMonth;
        let remarks = '';
        if (d.remarks != null) remarks = d.remarks;
        qualRows.push({
          id: rowId,
          qualificationId: d.qualificationId,
          customQualificationName: d.customQualificationName,
          acquiredYearMonth,
          remarks,
          isCustom: d.qualificationId == null,
        });
      }
      setQualificationRows(qualRows);

      const semRows: SeminarRow[] = [];
      for (const d of inheritedSems) {
        let rowId: number | undefined = d.id;
        if (needSemInherit) rowId = undefined;
        let seminarName = '';
        if (d.seminarName != null) seminarName = d.seminarName;
        let attendedYearMonth = '';
        if (d.attendedYearMonth != null) attendedYearMonth = d.attendedYearMonth;
        let remarks = '';
        if (d.remarks != null) remarks = d.remarks;
        semRows.push({
          id: rowId,
          adSeminarId: d.adSeminarId,
          seminarName,
          seminarCategoryId: d.seminarCategoryId,
          attendedYearMonth,
          remarks,
          isAd: d.adSeminarId != null,
        });
      }
      setSeminarRows(semRows);

      setQualSaved(!needQualInherit || inheritedQuals.length === 0);
      setSemSaved(!needSemInherit || inheritedSems.length === 0);

      const savedTab = localStorage.getItem(`inventory-tab-${inventoryId}`);
      if (savedTab === 'itSkill' || savedTab === 'qualification' || savedTab === 'seminar') {
        setTab(savedTab);
      }
    });
  }, [inventoryId]);

  const itSkillTree = useMemo(() => {
    const sorted: ItSkill[] = [];
    for (const skill of itSkills) {
      sorted.push(skill);
    }
    // category1SortOrder → category2Name → sortOrder の順で並び替える業務ルール
    for (let i = 1; i < sorted.length; i++) {
      const current = sorted[i];
      let j = i - 1;
      while (j >= 0 && compareSkills(sorted[j], current) > 0) {
        sorted[j + 1] = sorted[j];
        j = j - 1;
      }
      sorted[j + 1] = current;
    }
    const map = new Map<string, Map<string, ItSkill[]>>();
    for (const skill of sorted) {
      let cat1 = skill.category1Name;
      if (!cat1) cat1 = '未分類';
      let cat2 = skill.category2Name;
      if (!cat2) cat2 = '';
      if (!map.has(cat1)) map.set(cat1, new Map());
      const cat2Map = map.get(cat1)!;
      if (!cat2Map.has(cat2)) cat2Map.set(cat2, []);
      cat2Map.get(cat2)!.push(skill);
    }
    const tree: { cat1: string; cat2Groups: { cat2: string; skills: ItSkill[] }[] }[] = [];
    for (const [cat1, cat2Map] of map) {
      const cat2Groups: { cat2: string; skills: ItSkill[] }[] = [];
      for (const [cat2, skills] of cat2Map) {
        cat2Groups.push({ cat2, skills });
      }
      tree.push({ cat1, cat2Groups });
    }
    return tree;
  }, [itSkills]);

  const qualsByCategory = useMemo(() => {
    const acc: Record<string, Qualification[]> = {};
    for (const q of qualifications) {
      let cat = q.categoryName;
      if (!cat) cat = '未分類';
      if (!acc[cat]) acc[cat] = [];
      acc[cat].push(q);
    }
    return acc;
  }, [qualifications]);

  const adsByCategory = useMemo(() => {
    const acc: Record<string, AdSeminar[]> = {};
    for (const a of adSeminars) {
      let cat = a.categoryName;
      if (!cat) cat = '未分類';
      if (!acc[cat]) acc[cat] = [];
      acc[cat].push(a);
    }
    return acc;
  }, [adSeminars]);

  const filteredQualsByCategory = useMemo(() => {
    const term = qualSearch.trim().toLowerCase();
    if (!term) return qualsByCategory;
    const result: Record<string, Qualification[]> = {};
    for (const [cat, quals] of Object.entries(qualsByCategory)) {
      const filtered: Qualification[] = [];
      for (const q of quals) {
        if (q.name.toLowerCase().includes(term)) filtered.push(q);
      }
      if (filtered.length > 0) result[cat] = filtered;
    }
    return result;
  }, [qualsByCategory, qualSearch]);

  const filteredAdsByCategory = useMemo(() => {
    const term = semSearch.trim().toLowerCase();
    if (!term) return adsByCategory;
    const result: Record<string, AdSeminar[]> = {};
    for (const [cat, ads] of Object.entries(adsByCategory)) {
      const filtered: AdSeminar[] = [];
      for (const a of ads) {
        if (a.name.toLowerCase().includes(term)) filtered.push(a);
      }
      if (filtered.length > 0) result[cat] = filtered;
    }
    return result;
  }, [adsByCategory, semSearch]);

  const itSkillScoredCount = useMemo(() => {
    let count = 0;
    for (const e of Object.values(itSkillEntries)) {
      if (e.levelId !== null) count = count + 1;
    }
    return count + customSkillRows.length;
  }, [itSkillEntries, customSkillRows]);

  const allItSkillsScored = useMemo(() => {
    let allScored = true;
    for (const skill of itSkills) {
      const e = itSkillEntries[skill.id];
      if (!e || e.levelId === null) {
        allScored = false;
        break;
      }
    }
    return allScored;
  }, [itSkills, itSkillEntries]);

  const missingSkillIds = useMemo(() => {
    const ids = new Set<number>();
    if (!validationAttempted) return ids;
    for (const skill of itSkills) {
      const e = itSkillEntries[skill.id];
      if (!e || e.levelId === null) ids.add(skill.id);
    }
    return ids;
  }, [validationAttempted, itSkills, itSkillEntries]);

  const customSkillErrors = useMemo(() => {
    const errors = new Set<number>();
    if (!validationAttempted) return errors;
    for (let i = 0; i < customSkillRows.length; i++) {
      if (!customSkillRows[i].customSkillName.trim()) errors.add(i);
    }
    return errors;
  }, [validationAttempted, customSkillRows]);

  const qualErrors = useMemo(() => {
    const errors = new Set<number>();
    if (!qualValidationAttempted) return errors;
    for (let i = 0; i < qualificationRows.length; i++) {
      const r = qualificationRows[i];
      let invalid = false;
      let customName = '';
      if (r.customQualificationName != null) customName = r.customQualificationName;
      if (r.isCustom && !customName.trim()) invalid = true;
      if (!r.acquiredYearMonth) invalid = true;
      if (invalid) errors.add(i);
    }
    return errors;
  }, [qualValidationAttempted, qualificationRows]);

  const seminarErrors = useMemo(() => {
    const errors = new Set<number>();
    if (!semValidationAttempted) return errors;
    for (let i = 0; i < seminarRows.length; i++) {
      const r = seminarRows[i];
      let invalid = false;
      if (!r.isAd && !r.seminarName.trim()) invalid = true;
      if (!r.attendedYearMonth) invalid = true;
      if (invalid) errors.add(i);
    }
    return errors;
  }, [semValidationAttempted, seminarRows]);

  const showMessage = (msg: string) => {
    setSaveMessage(msg);
    setTimeout(() => setSaveMessage(''), 3000);
  };

  const handleSaveItSkills = useCallback(async () => {
    if (isSaving) return;
    setValidationAttempted(true);
    let missingCount = 0;
    for (const skill of itSkills) {
      const e = itSkillEntries[skill.id];
      if (!e || e.levelId === null) missingCount = missingCount + 1;
    }
    let emptyCustom = 0;
    for (const r of customSkillRows) {
      if (!r.customSkillName.trim()) emptyCustom = emptyCustom + 1;
    }
    if (missingCount > 0 || emptyCustom > 0) {
      const msgs: string[] = [];
      if (missingCount > 0) msgs.push(t('inventoryPage.validation.missingLevel', { count: missingCount }));
      if (emptyCustom > 0) msgs.push(t('inventoryPage.validation.missingCustomName', { count: emptyCustom }));
      setErrorMessage(msgs.join('、') + t('inventoryPage.validation.validationSuffix'));
      return;
    }
    setErrorMessage('');
    setIsSaving(true);
    try {
      const items: { id?: number | null; itSkillId: number | null; customSkillName: string | null; skillLevelId: number; remarks: string }[] = [];
      for (const [skillId, entry] of Object.entries(itSkillEntries)) {
        if (entry.levelId !== null) {
          items.push({
            id: entry.id,
            itSkillId: Number(skillId),
            customSkillName: null,
            skillLevelId: entry.levelId,
            remarks: entry.remarks,
          });
        }
      }
      for (const row of customSkillRows) {
        items.push({
          id: row.id,
          itSkillId: null,
          customSkillName: row.customSkillName,
          skillLevelId: row.levelId,
          remarks: row.remarks,
        });
      }
      await saveItSkillDetails(inventoryId, items);
      showMessage(t('inventoryPage.message.itSkillSaved'));
    } catch {
      showMessage(t('inventoryPage.message.saveFailed'));
    } finally {
      setIsSaving(false);
    }
  }, [inventoryId, itSkills, itSkillEntries, customSkillRows, isSaving, t]);

  const handleSaveQualifications = useCallback(async () => {
    if (isSaving) return;
    setQualValidationAttempted(true);
    let hasError = false;
    for (const r of qualificationRows) {
      let customName = '';
      if (r.customQualificationName != null) customName = r.customQualificationName;
      if (r.isCustom && !customName.trim()) {
        hasError = true;
        break;
      }
      if (!r.acquiredYearMonth) {
        hasError = true;
        break;
      }
    }
    if (hasError) { setErrorMessage(t('inventoryPage.validation.requiredFields')); return; }
    setErrorMessage('');
    setIsSaving(true);
    try {
      const items: { id?: number | null; qualificationId?: number | null; customQualificationName?: string | null; acquiredYearMonth: string | null; remarks: string }[] = [];
      for (const r of qualificationRows) {
        let acquiredYearMonth: string | null = r.acquiredYearMonth;
        if (!r.acquiredYearMonth) acquiredYearMonth = null;
        items.push({
          id: r.id,
          qualificationId: r.qualificationId,
          customQualificationName: r.customQualificationName,
          acquiredYearMonth,
          remarks: r.remarks,
        });
      }
      await saveQualificationDetails(inventoryId, items);
      showMessage(t('inventoryPage.message.qualSaved'));
      setQualSaved(true);
    } catch {
      showMessage(t('inventoryPage.message.saveFailed'));
    } finally {
      setIsSaving(false);
    }
  }, [inventoryId, qualificationRows, isSaving, t]);

  const handleSaveSeminars = useCallback(async () => {
    if (isSaving) return;
    setSemValidationAttempted(true);
    let hasError = false;
    for (const r of seminarRows) {
      if (!r.isAd && !r.seminarName.trim()) {
        hasError = true;
        break;
      }
      if (!r.attendedYearMonth) {
        hasError = true;
        break;
      }
    }
    if (hasError) { setErrorMessage(t('inventoryPage.validation.requiredFields')); return; }
    setErrorMessage('');
    setIsSaving(true);
    try {
      const items: { id?: number | null; adSeminarId?: number | null; seminarName: string | null; seminarCategoryId?: number | null; attendedYearMonth: string | null; remarks: string }[] = [];
      for (const r of seminarRows) {
        let seminarName: string | null = r.seminarName;
        let seminarCategoryId = r.seminarCategoryId;
        if (r.isAd) {
          seminarName = null;
          seminarCategoryId = null;
        }
        let attendedYearMonth: string | null = r.attendedYearMonth;
        if (!r.attendedYearMonth) attendedYearMonth = null;
        items.push({
          id: r.id,
          adSeminarId: r.adSeminarId,
          seminarName,
          seminarCategoryId,
          attendedYearMonth,
          remarks: r.remarks,
        });
      }
      await saveSeminarDetails(inventoryId, items);
      showMessage(t('inventoryPage.message.seminarSaved'));
      setSemSaved(true);
    } catch {
      showMessage(t('inventoryPage.message.saveFailed'));
    } finally {
      setIsSaving(false);
    }
  }, [inventoryId, seminarRows, isSaving, t]);

  const handleSubmit = () => setShowSubmitConfirm(true);

  const doSubmit = async () => {
    setShowSubmitConfirm(false);
    setIsSubmitting(true);
    try {
      await submitInventory(inventoryId);
      localStorage.removeItem(`inventory-tab-${inventoryId}`);
      navigate(`/inventory/${inventoryId}/comparison`);
    } catch {
      showMessage(t('inventoryPage.message.submitFailed'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const setSkillLevel = (skillId: number, levelId: number) => {
    setItSkillEntries(prev => {
      let base = prev[skillId];
      if (base == null) base = { id: null, remarks: '' };
      return {
        ...prev,
        [skillId]: { ...base, levelId },
      };
    });
  };

  const clearSkillEntry = (skillId: number) => {
    setItSkillEntries(prev => ({
      ...prev,
      [skillId]: { ...prev[skillId], levelId: null },
    }));
  };

  const setSkillRemarks = (skillId: number, remarks: string) => {
    setItSkillEntries(prev => {
      let base = prev[skillId];
      if (base == null) base = { id: null, levelId: null };
      return {
        ...prev,
        [skillId]: { ...base, remarks },
      };
    });
  };

  const addCustomSkillRow = () => {
    let defaultLevelId = 1;
    if (skillLevels.length > 0) defaultLevelId = skillLevels[0].id;
    setCustomSkillRows(prev => [...prev, {
      customSkillName: '', levelId: defaultLevelId, remarks: '',
    }]);
  };

  const addQualificationRow = (qual: Qualification) => {
    if (hasQualification(qualificationRows, qual.id)) return;
    setQualificationRows(prev => [...prev, {
      qualificationId: qual.id, acquiredYearMonth: '', remarks: '', isCustom: false,
    }]);
  };
  const addCustomQualRow = () => {
    setQualificationRows(prev => [...prev, {
      customQualificationName: '', acquiredYearMonth: '', remarks: '', isCustom: true,
    }]);
  };
  const removeQualificationRow = (idx: number) => {
    setQualificationRows(prev => removeAt(prev, idx));
  };

  const addAdSeminarRow = (ad: AdSeminar) => {
    if (hasAdSeminar(seminarRows, ad.id)) return;
    setSeminarRows(prev => [...prev, {
      adSeminarId: ad.id, seminarName: '', attendedYearMonth: '', remarks: '', isAd: true,
    }]);
  };
  const addCustomSeminarRow = () => {
    setSeminarRows(prev => [...prev, {
      seminarName: '', attendedYearMonth: '', remarks: '', isAd: false,
    }]);
  };
  const removeSeminarRow = (idx: number) => {
    setSeminarRows(prev => removeAt(prev, idx));
  };

  const toggleQualCat = (cat: string) => {
    setOpenQualCats(prev => {
      const next = new Set(prev);
      if (next.has(cat)) next.delete(cat); else next.add(cat);
      return next;
    });
  };

  const toggleSemCat = (cat: string) => {
    setOpenSemCats(prev => {
      const next = new Set(prev);
      if (next.has(cat)) next.delete(cat); else next.add(cat);
      return next;
    });
  };

  if (!inventory) return <div className="loading">{t('loading')}</div>;

  // レベル凡例（IT スキルタブのヘッダー右側に表示する各レベルの説明）
  const levelLegendItems: React.ReactNode[] = [];
  for (const lv of skillLevels) {
    levelLegendItems.push(
      <span key={lv.id} className="level-legend-item">
        <span className="level-legend-lv">{lv.levelValue}</span>
        {lv.description}
      </span>,
    );
  }

  let itSkillTabClass = 'tab-btn';
  if (tab === 'itSkill') itSkillTabClass = 'tab-btn active';
  let qualTabClass = 'tab-btn';
  if (tab === 'qualification') qualTabClass = 'tab-btn active';
  let semTabClass = 'tab-btn';
  if (tab === 'seminar') semTabClass = 'tab-btn active';

  let saveButtonLabel = t('inventoryPage.saveButton');
  if (isSaving) saveButtonLabel = t('inventoryPage.savingButton');

  let submitButtonLabel = t('inventoryPage.submitButton');
  if (isSubmitting) submitButtonLabel = t('inventoryPage.submittingButton');

  const skillLevelHeaderCells: React.ReactNode[] = [];
  for (const lv of skillLevels) {
    skillLevelHeaderCells.push(
      <th key={lv.id} className="col-level" title={lv.description}>
        {lv.levelValue}
      </th>,
    );
  }

  // ITスキル採点テーブルの本体行（大分類・中分類の見出し行とスキル行）を構築する
  const itSkillBodyRows: React.ReactNode[] = [];
  for (const group of itSkillTree) {
    itSkillBodyRows.push(
      <tr key={`cat1-${group.cat1}`} className="scoring-cat1-row">
        <td colSpan={skillLevels.length + 2}>{group.cat1}</td>
      </tr>,
    );
    for (const cat2Group of group.cat2Groups) {
      if (cat2Group.cat2) {
        itSkillBodyRows.push(
          <tr key={`cat2-${group.cat1}-${cat2Group.cat2}`} className="scoring-cat2-row">
            <td colSpan={skillLevels.length + 2}>{cat2Group.cat2}</td>
          </tr>,
        );
      }
      for (const skill of cat2Group.skills) {
        let entry = itSkillEntries[skill.id];
        if (entry == null) entry = { levelId: null, remarks: '' };
        const currentEntry = entry;

        let skillRowClass = 'scoring-skill-row';
        if (currentEntry.levelId !== null) skillRowClass = skillRowClass + ' scored';
        if (missingSkillIds.has(skill.id)) skillRowClass = skillRowClass + ' missing';

        const radioCells: React.ReactNode[] = [];
        for (const lv of skillLevels) {
          radioCells.push(
            <td key={lv.id} className="radio-cell">
              <input
                type="radio"
                name={`skill-${skill.id}`}
                checked={currentEntry.levelId === lv.id}
                onChange={() => setSkillLevel(skill.id, lv.id)}
              />
            </td>,
          );
        }

        itSkillBodyRows.push(
          <tr key={skill.id} className={skillRowClass}>
            <td className="skill-name-cell">{skill.name}</td>
            {radioCells}
            <td>
              <div className="remarks-cell">
                <textarea
                  className="remarks-input"
                  rows={2}
                  value={currentEntry.remarks}
                  placeholder={t('inventoryPage.table.optional')}
                  onChange={e => setSkillRemarks(skill.id, e.target.value)}
                />
                {currentEntry.levelId !== null && (
                  <button
                    className="clear-score-btn"
                    onClick={() => clearSkillEntry(skill.id)}
                    title={t('inventoryPage.clearScoreTitle')}
                  >×</button>
                )}
              </div>
            </td>
          </tr>,
        );
      }
    }
  }
  if (customSkillRows.length > 0) {
    itSkillBodyRows.push(
      <tr key="custom-header" className="scoring-cat1-row">
        <td colSpan={skillLevels.length + 2}>{t('inventoryPage.customSkillCategory')}</td>
      </tr>,
    );
  }
  for (let idx = 0; idx < customSkillRows.length; idx++) {
    const row = customSkillRows[idx];
    const rowIdx = idx;

    let customNameClass = 'input custom-name-input';
    if (customSkillErrors.has(rowIdx)) customNameClass = customNameClass + ' input--error';

    const emptyRadioCells: React.ReactNode[] = [];
    for (const lv of skillLevels) {
      emptyRadioCells.push(<td key={lv.id} className="radio-cell"></td>);
    }

    itSkillBodyRows.push(
      <tr key={`custom-${rowIdx}`} className="scoring-skill-row custom-skill-row">
        <td>
          <input
            type="text"
            className={customNameClass}
            placeholder={t('inventoryPage.customSkillNamePlaceholder')}
            value={row.customSkillName}
            onChange={e => setCustomSkillRows(prev => replaceAt(prev, rowIdx, { customSkillName: e.target.value }))}
          />
        </td>
        {emptyRadioCells}
        <td>
          <div className="remarks-cell">
            <textarea
              className="remarks-input"
              rows={2}
              value={row.remarks}
              placeholder={t('inventoryPage.table.optional')}
              onChange={e => setCustomSkillRows(prev => replaceAt(prev, rowIdx, { remarks: e.target.value }))}
            />
            <button
              className="clear-score-btn"
              onClick={() => setCustomSkillRows(prev => removeAt(prev, rowIdx))}
              title={t('common:button.delete')}
            >×</button>
          </div>
        </td>
      </tr>,
    );
  }

  // 資格候補一覧（カテゴリごとの折りたたみグループ）を構築する
  const qualCategoryGroups: React.ReactNode[] = [];
  for (const [cat, quals] of Object.entries(filteredQualsByCategory)) {
    let isOpen = openQualCats.has(cat);
    if (qualSearch.trim()) isOpen = true;

    let arrowClass = 'category-arrow';
    if (!isOpen) arrowClass = arrowClass + ' collapsed';

    const qualButtons: React.ReactNode[] = [];
    if (isOpen) {
      for (const q of quals) {
        let addBtnClass = 'skill-add-btn';
        if (hasQualification(qualificationRows, q.id)) addBtnClass = addBtnClass + ' added';
        qualButtons.push(
          <button key={q.id} className={addBtnClass} onClick={() => addQualificationRow(q)}>
            + {q.name}
          </button>,
        );
      }
    }

    qualCategoryGroups.push(
      <div key={cat} className="skill-category-group">
        <button className="category-toggle" onClick={() => toggleQualCat(cat)}>
          <span className="category-toggle-text">{cat}</span>
          <span className={arrowClass}>▾</span>
        </button>
        {qualButtons}
      </div>,
    );
  }

  // 資格入力欄（登録済みの各行）を構築する
  const qualificationRowElements: React.ReactNode[] = [];
  for (let idx = 0; idx < qualificationRows.length; idx++) {
    const row = qualificationRows[idx];
    const rowIdx = idx;

    let rowClass = 'skill-row';
    if (qualErrors.has(rowIdx)) rowClass = rowClass + ' skill-row--error';

    let customNameValue = '';
    if (row.customQualificationName != null) customNameValue = row.customQualificationName;

    let customNameInputClass = 'input skill-name-input';
    if (qualValidationAttempted && !customNameValue.trim()) customNameInputClass = customNameInputClass + ' input--error';

    let rowHeaderContent: React.ReactNode = (
      <span className="skill-name">
        {findQualificationName(qualifications, row.qualificationId)}
      </span>
    );
    if (row.isCustom) {
      rowHeaderContent = (
        <input
          className={customNameInputClass}
          placeholder={t('inventoryPage.qualSection.qualNamePlaceholder')}
          value={customNameValue}
          onChange={e => setQualificationRows(prev => replaceAt(prev, rowIdx, { customQualificationName: e.target.value }))}
        />
      );
    }

    let acquiredMonthValue = '';
    if (row.acquiredYearMonth) acquiredMonthValue = row.acquiredYearMonth.slice(0, 7);

    let acquiredMonthClass = 'input';
    if (qualValidationAttempted && !row.acquiredYearMonth) acquiredMonthClass = acquiredMonthClass + ' input--error';

    qualificationRowElements.push(
      <div key={rowIdx} className={rowClass}>
        <div className="skill-row-header">
          {rowHeaderContent}
          <button className="remove-btn" onClick={() => removeQualificationRow(rowIdx)}>✕</button>
        </div>
        <div className="skill-row-body">
          <label className="form-label">{t('inventoryPage.table.acquiredYearMonth')}</label>
          <input
            type="month"
            className={acquiredMonthClass}
            value={acquiredMonthValue}
            onChange={e => {
              let newValue = '';
              if (e.target.value) newValue = `${e.target.value}-01`;
              setQualificationRows(prev => replaceAt(prev, rowIdx, { acquiredYearMonth: newValue }));
            }}
          />
          <label className="form-label">{t('inventoryPage.table.remarksShort')}</label>
          <textarea
            className="textarea"
            value={row.remarks}
            placeholder={t('inventoryPage.table.optional')}
            onChange={e => setQualificationRows(prev => replaceAt(prev, rowIdx, { remarks: e.target.value }))}
          />
        </div>
      </div>,
    );
  }

  // 推奨セミナー候補一覧（カテゴリごとの折りたたみグループ）を構築する
  const semCategoryGroups: React.ReactNode[] = [];
  for (const [cat, ads] of Object.entries(filteredAdsByCategory)) {
    let isOpen = openSemCats.has(cat);
    if (semSearch.trim()) isOpen = true;

    let arrowClass = 'category-arrow';
    if (!isOpen) arrowClass = arrowClass + ' collapsed';

    const adButtons: React.ReactNode[] = [];
    if (isOpen) {
      for (const ad of ads) {
        let addBtnClass = 'skill-add-btn';
        if (hasAdSeminar(seminarRows, ad.id)) addBtnClass = addBtnClass + ' added';
        adButtons.push(
          <button key={ad.id} className={addBtnClass} onClick={() => addAdSeminarRow(ad)}>
            + {ad.name}
          </button>,
        );
      }
    }

    semCategoryGroups.push(
      <div key={cat} className="skill-category-group">
        <button className="category-toggle" onClick={() => toggleSemCat(cat)}>
          <span className="category-toggle-text">{cat}</span>
          <span className={arrowClass}>▾</span>
        </button>
        {adButtons}
      </div>,
    );
  }

  // セミナー入力欄（登録済みの各行）を構築する
  const seminarRowElements: React.ReactNode[] = [];
  for (let idx = 0; idx < seminarRows.length; idx++) {
    const row = seminarRows[idx];
    const rowIdx = idx;

    let rowClass = 'skill-row';
    if (seminarErrors.has(rowIdx)) rowClass = rowClass + ' skill-row--error';

    let seminarNameInputClass = 'input skill-name-input';
    if (semValidationAttempted && !row.seminarName.trim()) seminarNameInputClass = seminarNameInputClass + ' input--error';

    let rowHeaderContent: React.ReactNode = (
      <input
        className={seminarNameInputClass}
        placeholder={t('inventoryPage.seminarSection.seminarNamePlaceholder')}
        value={row.seminarName}
        onChange={e => setSeminarRows(prev => replaceAt(prev, rowIdx, { seminarName: e.target.value }))}
      />
    );
    if (row.isAd) {
      rowHeaderContent = (
        <span className="skill-name">
          {t('inventoryPage.seminarSection.adPrefix')}{findAdSeminarName(adSeminars, row.adSeminarId)}
        </span>
      );
    }

    let attendedMonthValue = '';
    if (row.attendedYearMonth) attendedMonthValue = row.attendedYearMonth.slice(0, 7);

    let attendedMonthClass = 'input';
    if (semValidationAttempted && !row.attendedYearMonth) attendedMonthClass = attendedMonthClass + ' input--error';

    seminarRowElements.push(
      <div key={rowIdx} className={rowClass}>
        <div className="skill-row-header">
          {rowHeaderContent}
          <button className="remove-btn" onClick={() => removeSeminarRow(rowIdx)}>✕</button>
        </div>
        <div className="skill-row-body">
          <label className="form-label">{t('inventoryPage.table.attendedYearMonth')}</label>
          <input
            type="month"
            className={attendedMonthClass}
            value={attendedMonthValue}
            onChange={e => {
              let newValue = '';
              if (e.target.value) newValue = `${e.target.value}-01`;
              setSeminarRows(prev => replaceAt(prev, rowIdx, { attendedYearMonth: newValue }));
            }}
          />
          <label className="form-label">{t('inventoryPage.table.remarksShort')}</label>
          <textarea
            className="textarea"
            value={row.remarks}
            placeholder={t('inventoryPage.table.optional')}
            onChange={e => setSeminarRows(prev => replaceAt(prev, rowIdx, { remarks: e.target.value }))}
          />
        </div>
      </div>,
    );
  }

  return (
    <div className="inventory-page">
      <NavBar />

      <main className="inventory-main">
        <div className="page-title-row">
          <h1 className="page-title">{t('inventoryPage.title', { fiscalYear: inventory.fiscalYear.name })}</h1>
          {tab === 'itSkill' && skillLevels.length > 0 && (
            <div className="level-legend">
              <span className="level-legend-label">{t('inventoryPage.levelLegendLabel')}</span>
              {levelLegendItems}
            </div>
          )}
        </div>

        {saveMessage && <div className="save-message">{saveMessage}</div>}
        {errorMessage && <div className="error-message">{errorMessage}</div>}

        <div className="tab-bar">
          <button className={itSkillTabClass} onClick={() => changeTab('itSkill')}>
            {t('inventoryPage.tab.itSkill', { count: itSkillScoredCount })}
          </button>
          <button className={qualTabClass} onClick={() => changeTab('qualification')}>
            {t('inventoryPage.tab.qualification', { count: qualificationRows.length })}
          </button>
          <button className={semTabClass} onClick={() => changeTab('seminar')}>
            {t('inventoryPage.tab.seminar', { count: seminarRows.length })}
          </button>
        </div>

        {/* IT Skills Tab — Scoring Sheet */}
        {tab === 'itSkill' && (
          <div className="tab-content">
            <div className="scoring-sheet">
              <div className="scoring-scroll">
                <table className="scoring-table">
                  <thead>
                    <tr>
                      <th className="col-skill-name">{t('inventoryPage.table.skillName')}</th>
                      {skillLevelHeaderCells}
                      <th className="col-remarks">{t('inventoryPage.table.remarks')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {itSkillBodyRows}
                  </tbody>
                </table>
              </div>
              <div className="scoring-actions">
                <button
                  className="btn btn-secondary"
                  onClick={addCustomSkillRow}
                >
                  {t('inventoryPage.addCustomSkill')}
                </button>
                <button
                  className="btn btn-primary save-btn"
                  onClick={handleSaveItSkills}
                  disabled={isSaving}
                >
                  {saveButtonLabel}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Qualifications Tab */}
        {tab === 'qualification' && (
          <div className="tab-content">
            <div className="skill-layout">
              <div className="skill-list-panel">
                <h3>{t('inventoryPage.qualSection.listTitle')}</h3>
                <input
                  type="text"
                  className="skill-search"
                  placeholder={t('inventoryPage.qualSection.searchPlaceholder')}
                  value={qualSearch}
                  onChange={e => setQualSearch(e.target.value)}
                />
                <div className="skill-list-scroll">
                  {qualCategoryGroups}
                </div>
                <div className="skill-list-footer">
                  <button className="btn btn-secondary custom-add-btn" onClick={addCustomQualRow}>
                    {t('inventoryPage.qualSection.addCustom')}
                  </button>
                </div>
              </div>

              <div className="skill-input-panel">
                <h3>{t('inventoryPage.qualSection.inputTitle')}</h3>
                {qualificationRows.length === 0 && (
                  <p className="empty-note">{t('inventoryPage.qualSection.emptyNote')}</p>
                )}
                {qualificationRowElements}
                <button
                  className="btn btn-primary save-btn"
                  onClick={handleSaveQualifications}
                  disabled={isSaving}
                >
                  {saveButtonLabel}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Seminars Tab */}
        {tab === 'seminar' && (
          <div className="tab-content">
            <div className="skill-layout">
              <div className="skill-list-panel">
                <h3>{t('inventoryPage.seminarSection.listTitle')}</h3>
                <input
                  type="text"
                  className="skill-search"
                  placeholder={t('inventoryPage.seminarSection.searchPlaceholder')}
                  value={semSearch}
                  onChange={e => setSemSearch(e.target.value)}
                />
                <div className="skill-list-scroll">
                  {semCategoryGroups}
                </div>
                <div className="skill-list-footer">
                  <button className="btn btn-secondary custom-add-btn" onClick={addCustomSeminarRow}>
                    {t('inventoryPage.seminarSection.addOther')}
                  </button>
                </div>
              </div>

              <div className="skill-input-panel">
                <h3>{t('inventoryPage.seminarSection.inputTitle')}</h3>
                {seminarRows.length === 0 && (
                  <p className="empty-note">{t('inventoryPage.seminarSection.emptyNote')}</p>
                )}
                {seminarRowElements}
                <button
                  className="btn btn-primary save-btn"
                  onClick={handleSaveSeminars}
                  disabled={isSaving}
                >
                  {saveButtonLabel}
                </button>
              </div>
            </div>
          </div>
        )}

        <div className="submit-section">
          {!allItSkillsScored && (
            <p className="submit-hint">{t('inventoryPage.submitHint.itSkills')}</p>
          )}
          {!qualSaved && (
            <p className="submit-hint">{t('inventoryPage.submitHint.qualification')}</p>
          )}
          {!semSaved && (
            <p className="submit-hint">{t('inventoryPage.submitHint.seminar')}</p>
          )}
          <button
            className="btn btn-submit"
            onClick={handleSubmit}
            disabled={isSubmitting || !allItSkillsScored || !qualSaved || !semSaved}
          >
            {submitButtonLabel}
          </button>
        </div>
      </main>

      {showSubmitConfirm && (
        <ConfirmDialog
          title={t('inventoryPage.submitConfirm.title')}
          message={t('inventoryPage.submitConfirm.message')}
          confirmLabel={t('inventoryPage.submitConfirm.confirmLabel')}
          onConfirm={doSubmit}
          onCancel={() => setShowSubmitConfirm(false)}
        />
      )}
    </div>
  );
}
