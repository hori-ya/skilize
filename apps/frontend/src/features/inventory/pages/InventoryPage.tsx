import { Fragment, useEffect, useState, useCallback, useMemo } from 'react';
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

export default function InventoryPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const inventoryId = Number(id);

  const [inventory, setInventory] = useState<InventoryDetail | null>(null);
  const [tab, setTab] = useState<Tab>('itSkill');
  const [isSaving, setIsSaving] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
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
        if (d.itSkillId != null) {
          entries[d.itSkillId] = { id: d.id, levelId: d.skillLevelId, remarks: d.remarks ?? '' };
        } else {
          customs.push({ id: d.id, customSkillName: d.customSkillName ?? '', levelId: d.skillLevelId, remarks: d.remarks ?? '' });
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
          const [prevQualRes, prevSemRes] = await Promise.all([
            needQualInherit ? getQualificationDetails(prevId) : Promise.resolve(null),
            needSemInherit ? getSeminarDetails(prevId) : Promise.resolve(null),
          ]);
          if (prevQualRes && prevQualRes.data.items.length > 0) inheritedQuals = prevQualRes.data.items;
          if (prevSemRes && prevSemRes.data.items.length > 0) inheritedSems = prevSemRes.data.items;
        }
      }

      setQualificationRows(inheritedQuals.map(d => ({
        id: needQualInherit ? undefined : d.id,
        qualificationId: d.qualificationId,
        customQualificationName: d.customQualificationName,
        acquiredYearMonth: d.acquiredYearMonth ?? '',
        remarks: d.remarks ?? '',
        isCustom: d.qualificationId == null,
      })));
      setSeminarRows(inheritedSems.map(d => ({
        id: needSemInherit ? undefined : d.id,
        adSeminarId: d.adSeminarId,
        seminarName: d.seminarName ?? '',
        seminarCategoryId: d.seminarCategoryId,
        attendedYearMonth: d.attendedYearMonth ?? '',
        remarks: d.remarks ?? '',
        isAd: d.adSeminarId != null,
      })));

      setQualSaved(!needQualInherit || inheritedQuals.length === 0);
      setSemSaved(!needSemInherit || inheritedSems.length === 0);

      const savedTab = localStorage.getItem(`inventory-tab-${inventoryId}`);
      if (savedTab === 'itSkill' || savedTab === 'qualification' || savedTab === 'seminar') {
        setTab(savedTab);
      }
    });
  }, [inventoryId]);

  const itSkillTree = useMemo(() => {
    const map = new Map<string, Map<string, ItSkill[]>>();
    for (const skill of itSkills) {
      const cat1 = skill.category1Name || '未分類';
      const cat2 = skill.category2Name || '';
      if (!map.has(cat1)) map.set(cat1, new Map());
      const cat2Map = map.get(cat1)!;
      if (!cat2Map.has(cat2)) cat2Map.set(cat2, []);
      cat2Map.get(cat2)!.push(skill);
    }
    return Array.from(map.entries()).map(([cat1, cat2Map]) => ({
      cat1,
      cat2Groups: Array.from(cat2Map.entries()).map(([cat2, skills]) => ({ cat2, skills })),
    }));
  }, [itSkills]);

  const qualsByCategory = useMemo(() => qualifications.reduce<Record<string, Qualification[]>>((acc, q) => {
    const cat = q.categoryName || '未分類';
    if (!acc[cat]) acc[cat] = [];
    acc[cat].push(q);
    return acc;
  }, {}), [qualifications]);

  const adsByCategory = useMemo(() => adSeminars.reduce<Record<string, AdSeminar[]>>((acc, a) => {
    const cat = a.categoryName || '未分類';
    if (!acc[cat]) acc[cat] = [];
    acc[cat].push(a);
    return acc;
  }, {}), [adSeminars]);

  const itSkillScoredCount = useMemo(() =>
    Object.values(itSkillEntries).filter(e => e.levelId !== null).length + customSkillRows.length,
    [itSkillEntries, customSkillRows]);

  const allItSkillsScored = useMemo(() =>
    itSkills.every(skill => {
      const e = itSkillEntries[skill.id];
      return e && e.levelId !== null;
    }),
    [itSkills, itSkillEntries]);

  const missingSkillIds = useMemo(() => {
    if (!validationAttempted) return new Set<number>();
    return new Set(
      itSkills
        .filter(skill => { const e = itSkillEntries[skill.id]; return !e || e.levelId === null; })
        .map(s => s.id)
    );
  }, [validationAttempted, itSkills, itSkillEntries]);

  const customSkillErrors = useMemo(() => {
    if (!validationAttempted) return new Set<number>();
    return new Set(customSkillRows.map((r, i) => !r.customSkillName.trim() ? i : -1).filter(i => i >= 0));
  }, [validationAttempted, customSkillRows]);

  const qualErrors = useMemo(() => {
    if (!qualValidationAttempted) return new Set<number>();
    return new Set(qualificationRows.map((r, i) => {
      if (r.isCustom && !r.customQualificationName?.trim()) return i;
      if (!r.acquiredYearMonth) return i;
      return -1;
    }).filter(i => i >= 0));
  }, [qualValidationAttempted, qualificationRows]);

  const seminarErrors = useMemo(() => {
    if (!semValidationAttempted) return new Set<number>();
    return new Set(seminarRows.map((r, i) => {
      if (!r.isAd && !r.seminarName.trim()) return i;
      if (!r.attendedYearMonth) return i;
      return -1;
    }).filter(i => i >= 0));
  }, [semValidationAttempted, seminarRows]);

  const showMessage = (msg: string) => {
    setSaveMessage(msg);
    setTimeout(() => setSaveMessage(''), 3000);
  };

  const handleSaveItSkills = useCallback(async () => {
    if (isSaving) return;
    setValidationAttempted(true);
    const missingCount = itSkills.filter(skill => {
      const e = itSkillEntries[skill.id];
      return !e || e.levelId === null;
    }).length;
    const emptyCustom = customSkillRows.filter(r => !r.customSkillName.trim()).length;
    if (missingCount > 0 || emptyCustom > 0) {
      const msgs: string[] = [];
      if (missingCount > 0) msgs.push(`レベルが未入力のスキルが ${missingCount} 件`);
      if (emptyCustom > 0) msgs.push(`スキル名が未入力のカスタムスキルが ${emptyCustom} 件`);
      setErrorMessage(msgs.join('、') + 'あります。');
      return;
    }
    setErrorMessage('');
    setIsSaving(true);
    try {
      const items = [
        ...Object.entries(itSkillEntries)
          .filter(([, entry]) => entry.levelId !== null)
          .map(([skillId, entry]) => ({
            id: entry.id,
            itSkillId: Number(skillId),
            customSkillName: null,
            skillLevelId: entry.levelId!,
            remarks: entry.remarks,
          })),
        ...customSkillRows.map(row => ({
          id: row.id,
          itSkillId: null,
          customSkillName: row.customSkillName,
          skillLevelId: row.levelId,
          remarks: row.remarks,
        })),
      ];
      await saveItSkillDetails(inventoryId, items);
      showMessage('ITスキルを保存しました');
    } catch {
      showMessage('保存に失敗しました');
    } finally {
      setIsSaving(false);
    }
  }, [inventoryId, itSkills, itSkillEntries, customSkillRows, isSaving]);

  const handleSaveQualifications = useCallback(async () => {
    if (isSaving) return;
    setQualValidationAttempted(true);
    const hasError = qualificationRows.some(r => {
      if (r.isCustom && !r.customQualificationName?.trim()) return true;
      if (!r.acquiredYearMonth) return true;
      return false;
    });
    if (hasError) { setErrorMessage('必須項目を入力してください。'); return; }
    setErrorMessage('');
    setIsSaving(true);
    try {
      await saveQualificationDetails(inventoryId, qualificationRows.map(r => ({
        id: r.id,
        qualificationId: r.qualificationId,
        customQualificationName: r.customQualificationName,
        acquiredYearMonth: r.acquiredYearMonth || null,
        remarks: r.remarks,
      })));
      showMessage('資格を保存しました');
      setQualSaved(true);
    } catch {
      showMessage('保存に失敗しました');
    } finally {
      setIsSaving(false);
    }
  }, [inventoryId, qualificationRows, isSaving]);

  const handleSaveSeminars = useCallback(async () => {
    if (isSaving) return;
    setSemValidationAttempted(true);
    const hasError = seminarRows.some(r => {
      if (!r.isAd && !r.seminarName.trim()) return true;
      if (!r.attendedYearMonth) return true;
      return false;
    });
    if (hasError) { setErrorMessage('必須項目を入力してください。'); return; }
    setErrorMessage('');
    setIsSaving(true);
    try {
      await saveSeminarDetails(inventoryId, seminarRows.map(r => ({
        id: r.id,
        adSeminarId: r.adSeminarId,
        seminarName: r.isAd ? null : r.seminarName,
        seminarCategoryId: r.isAd ? null : r.seminarCategoryId,
        attendedYearMonth: r.attendedYearMonth || null,
        remarks: r.remarks,
      })));
      showMessage('セミナーを保存しました');
      setSemSaved(true);
    } catch {
      showMessage('保存に失敗しました');
    } finally {
      setIsSaving(false);
    }
  }, [inventoryId, seminarRows, isSaving]);

  const handleSubmit = async () => {
    if (!window.confirm('棚卸を提出しますか？提出後も再編集できます。')) return;
    setIsSubmitting(true);
    try {
      await submitInventory(inventoryId);
      localStorage.removeItem(`inventory-tab-${inventoryId}`);
      navigate(`/inventory/${inventoryId}/comparison`);
    } catch {
      showMessage('提出に失敗しました');
    } finally {
      setIsSubmitting(false);
    }
  };

  const setSkillLevel = (skillId: number, levelId: number) => {
    setItSkillEntries(prev => ({
      ...prev,
      [skillId]: { ...prev[skillId] ?? { id: null, remarks: '' }, levelId },
    }));
  };

  const clearSkillEntry = (skillId: number) => {
    setItSkillEntries(prev => ({
      ...prev,
      [skillId]: { ...prev[skillId], levelId: null },
    }));
  };

  const setSkillRemarks = (skillId: number, remarks: string) => {
    setItSkillEntries(prev => ({
      ...prev,
      [skillId]: { ...prev[skillId] ?? { id: null, levelId: null }, remarks },
    }));
  };

  const addQualificationRow = (qual: Qualification) => {
    if (qualificationRows.some(r => r.qualificationId === qual.id)) return;
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
    setQualificationRows(prev => prev.filter((_, i) => i !== idx));
  };

  const addAdSeminarRow = (ad: AdSeminar) => {
    if (seminarRows.some(r => r.adSeminarId === ad.id)) return;
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
    setSeminarRows(prev => prev.filter((_, i) => i !== idx));
  };

  if (!inventory) return <div className="loading">読み込み中...</div>;

  return (
    <div className="inventory-page">
      <NavBar />

      <main className="inventory-main">
        <div className="page-title-row">
          <h1 className="page-title">棚卸入力 — {inventory.fiscalYear.name}</h1>
          {tab === 'itSkill' && skillLevels.length > 0 && (
            <div className="level-legend">
              <span className="level-legend-label">採点基準</span>
              {skillLevels.map(lv => (
                <span key={lv.id} className="level-legend-item">
                  <span className="level-legend-lv">{lv.levelValue}</span>
                  {lv.description}
                </span>
              ))}
            </div>
          )}
        </div>

        {saveMessage && <div className="save-message">{saveMessage}</div>}
        {errorMessage && <div className="error-message">{errorMessage}</div>}

        <div className="tab-bar">
          <button className={`tab-btn${tab === 'itSkill' ? ' active' : ''}`} onClick={() => changeTab('itSkill')}>
            ITスキル（{itSkillScoredCount}）
          </button>
          <button className={`tab-btn${tab === 'qualification' ? ' active' : ''}`} onClick={() => changeTab('qualification')}>
            資格（{qualificationRows.length}）
          </button>
          <button className={`tab-btn${tab === 'seminar' ? ' active' : ''}`} onClick={() => changeTab('seminar')}>
            セミナー（{seminarRows.length}）
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
                      <th className="col-skill-name">スキル名</th>
                      {skillLevels.map(lv => (
                        <th key={lv.id} className="col-level" title={lv.description}>
                          {lv.levelValue}
                        </th>
                      ))}
                      <th className="col-remarks">備考（採点根拠）</th>
                    </tr>
                  </thead>
                  <tbody>
                    {itSkillTree.map(({ cat1, cat2Groups }) => (
                      <Fragment key={`cat1-${cat1}`}>
                        <tr className="scoring-cat1-row">
                          <td colSpan={skillLevels.length + 2}>{cat1}</td>
                        </tr>
                        {cat2Groups.map(({ cat2, skills }) => (
                          <Fragment key={`cat2-${cat1}-${cat2}`}>
                            {cat2 && (
                              <tr className="scoring-cat2-row">
                                <td colSpan={skillLevels.length + 2}>{cat2}</td>
                              </tr>
                            )}
                            {skills.map(skill => {
                              const entry = itSkillEntries[skill.id] ?? { levelId: null, remarks: '' };
                              return (
                                <tr key={skill.id} className={`scoring-skill-row${entry.levelId !== null ? ' scored' : ''}${missingSkillIds.has(skill.id) ? ' missing' : ''}`}>
                                  <td className="skill-name-cell">{skill.name}</td>
                                  {skillLevels.map(lv => (
                                    <td key={lv.id} className="radio-cell">
                                      <input
                                        type="radio"
                                        name={`skill-${skill.id}`}
                                        checked={entry.levelId === lv.id}
                                        onChange={() => setSkillLevel(skill.id, lv.id)}
                                      />
                                    </td>
                                  ))}
                                  <td>
                                    <div className="remarks-cell">
                                      <textarea
                                        className="remarks-input"
                                        rows={2}
                                        value={entry.remarks}
                                        placeholder="任意"
                                        onChange={e => setSkillRemarks(skill.id, e.target.value)}
                                      />
                                      {entry.levelId !== null && (
                                        <button
                                          className="clear-score-btn"
                                          onClick={() => clearSkillEntry(skill.id)}
                                          title="採点をクリア"
                                        >×</button>
                                      )}
                                    </div>
                                  </td>
                                </tr>
                              );
                            })}
                          </Fragment>
                        ))}
                      </Fragment>
                    ))}
                    {customSkillRows.length > 0 && (
                      <tr className="scoring-cat1-row">
                        <td colSpan={skillLevels.length + 2}>カスタムスキル</td>
                      </tr>
                    )}
                    {customSkillRows.map((row, idx) => (
                      <tr key={`custom-${idx}`} className="scoring-skill-row custom-skill-row">
                        <td>
                          <input
                            type="text"
                            className={`input custom-name-input${customSkillErrors.has(idx) ? ' input--error' : ''}`}
                            placeholder="スキル名を入力"
                            value={row.customSkillName}
                            onChange={e => setCustomSkillRows(prev => prev.map((r, i) =>
                              i === idx ? { ...r, customSkillName: e.target.value } : r))}
                          />
                        </td>
                        {skillLevels.map(lv => (
                          <td key={lv.id} className="radio-cell"></td>
                        ))}
                        <td>
                          <div className="remarks-cell">
                            <textarea
                              className="remarks-input"
                              rows={2}
                              value={row.remarks}
                              placeholder="任意"
                              onChange={e => setCustomSkillRows(prev => prev.map((r, i) =>
                                i === idx ? { ...r, remarks: e.target.value } : r))}
                            />
                            <button
                              className="clear-score-btn"
                              onClick={() => setCustomSkillRows(prev => prev.filter((_, i) => i !== idx))}
                              title="削除"
                            >×</button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="scoring-actions">
                <button
                  className="btn btn-secondary"
                  onClick={() => setCustomSkillRows(prev => [...prev, {
                    customSkillName: '', levelId: skillLevels[0]?.id ?? 1, remarks: '',
                  }])}
                >
                  + カスタムスキルを追加
                </button>
                <button
                  className="btn btn-primary save-btn"
                  onClick={handleSaveItSkills}
                  disabled={isSaving}
                >
                  {isSaving ? '保存中...' : '一時保存'}
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
                <h3>資格一覧から追加</h3>
                {Object.entries(qualsByCategory).map(([cat, quals]) => (
                  <div key={cat} className="skill-category-group">
                    <h4 className="category-label">{cat}</h4>
                    {quals.map(q => (
                      <button
                        key={q.id}
                        className={`skill-add-btn${qualificationRows.some(r => r.qualificationId === q.id) ? ' added' : ''}`}
                        onClick={() => addQualificationRow(q)}
                      >
                        + {q.name}
                      </button>
                    ))}
                  </div>
                ))}
                <button className="btn btn-secondary custom-add-btn" onClick={addCustomQualRow}>
                  + カスタム資格を追加
                </button>
              </div>

              <div className="skill-input-panel">
                <h3>入力済み資格</h3>
                {qualificationRows.length === 0 && (
                  <p className="empty-note">左のリストから資格を選択してください</p>
                )}
                {qualificationRows.map((row, idx) => (
                  <div key={idx} className={`skill-row${qualErrors.has(idx) ? ' skill-row--error' : ''}`}>
                    <div className="skill-row-header">
                      {row.isCustom ? (
                        <input
                          className={`input skill-name-input${qualValidationAttempted && !row.customQualificationName?.trim() ? ' input--error' : ''}`}
                          placeholder="資格名"
                          value={row.customQualificationName ?? ''}
                          onChange={e => setQualificationRows(prev => prev.map((r, i) =>
                            i === idx ? { ...r, customQualificationName: e.target.value } : r))}
                        />
                      ) : (
                        <span className="skill-name">
                          {qualifications.find(q => q.id === row.qualificationId)?.name}
                        </span>
                      )}
                      <button className="remove-btn" onClick={() => removeQualificationRow(idx)}>✕</button>
                    </div>
                    <div className="skill-row-body">
                      <label className="form-label">取得年月</label>
                      <input
                        type="month"
                        className={`input${qualValidationAttempted && !row.acquiredYearMonth ? ' input--error' : ''}`}
                        value={row.acquiredYearMonth ? row.acquiredYearMonth.slice(0, 7) : ''}
                        onChange={e => setQualificationRows(prev => prev.map((r, i) =>
                          i === idx ? { ...r, acquiredYearMonth: e.target.value ? `${e.target.value}-01` : '' } : r))}
                      />
                      <label className="form-label">備考</label>
                      <textarea
                        className="textarea"
                        value={row.remarks}
                        placeholder="任意"
                        onChange={e => setQualificationRows(prev => prev.map((r, i) =>
                          i === idx ? { ...r, remarks: e.target.value } : r))}
                      />
                    </div>
                  </div>
                ))}
                <button
                  className="btn btn-primary save-btn"
                  onClick={handleSaveQualifications}
                  disabled={isSaving}
                >
                  {isSaving ? '保存中...' : '一時保存'}
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
                <h3>AD一覧から追加</h3>
                {Object.entries(adsByCategory).map(([cat, ads]) => (
                  <div key={cat} className="skill-category-group">
                    <h4 className="category-label">{cat}</h4>
                    {ads.map(ad => (
                      <button
                        key={ad.id}
                        className={`skill-add-btn${seminarRows.some(r => r.adSeminarId === ad.id) ? ' added' : ''}`}
                        onClick={() => addAdSeminarRow(ad)}
                      >
                        + {ad.name}
                      </button>
                    ))}
                  </div>
                ))}
                <button className="btn btn-secondary custom-add-btn" onClick={addCustomSeminarRow}>
                  + 他のセミナーを追加
                </button>
              </div>

              <div className="skill-input-panel">
                <h3>入力済みセミナー</h3>
                {seminarRows.length === 0 && (
                  <p className="empty-note">左のリストからADを選択するか、他のセミナーを追加してください</p>
                )}
                {seminarRows.map((row, idx) => (
                  <div key={idx} className={`skill-row${seminarErrors.has(idx) ? ' skill-row--error' : ''}`}>
                    <div className="skill-row-header">
                      {row.isAd ? (
                        <span className="skill-name">
                          【AD】{adSeminars.find(a => a.id === row.adSeminarId)?.name}
                        </span>
                      ) : (
                        <input
                          className={`input skill-name-input${semValidationAttempted && !row.seminarName.trim() ? ' input--error' : ''}`}
                          placeholder="セミナー名"
                          value={row.seminarName}
                          onChange={e => setSeminarRows(prev => prev.map((r, i) =>
                            i === idx ? { ...r, seminarName: e.target.value } : r))}
                        />
                      )}
                      <button className="remove-btn" onClick={() => removeSeminarRow(idx)}>✕</button>
                    </div>
                    <div className="skill-row-body">
                      <label className="form-label">受講年月</label>
                      <input
                        type="month"
                        className={`input${semValidationAttempted && !row.attendedYearMonth ? ' input--error' : ''}`}
                        value={row.attendedYearMonth ? row.attendedYearMonth.slice(0, 7) : ''}
                        onChange={e => setSeminarRows(prev => prev.map((r, i) =>
                          i === idx ? { ...r, attendedYearMonth: e.target.value ? `${e.target.value}-01` : '' } : r))}
                      />
                      <label className="form-label">備考</label>
                      <textarea
                        className="textarea"
                        value={row.remarks}
                        placeholder="任意"
                        onChange={e => setSeminarRows(prev => prev.map((r, i) =>
                          i === idx ? { ...r, remarks: e.target.value } : r))}
                      />
                    </div>
                  </div>
                ))}
                <button
                  className="btn btn-primary save-btn"
                  onClick={handleSaveSeminars}
                  disabled={isSaving}
                >
                  {isSaving ? '保存中...' : '一時保存'}
                </button>
              </div>
            </div>
          </div>
        )}

        <div className="submit-section">
          {!allItSkillsScored && (
            <p className="submit-hint">すべてのITスキルに採点を入力してください</p>
          )}
          {!qualSaved && (
            <p className="submit-hint">資格タブで「一時保存」を実行してください</p>
          )}
          {!semSaved && (
            <p className="submit-hint">セミナータブで「一時保存」を実行してください</p>
          )}
          <button
            className="btn btn-submit"
            onClick={handleSubmit}
            disabled={isSubmitting || !allItSkillsScored || !qualSaved || !semSaved}
          >
            {isSubmitting ? '提出中...' : '棚卸を提出する →'}
          </button>
        </div>
      </main>
    </div>
  );
}
