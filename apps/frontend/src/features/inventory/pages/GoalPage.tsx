/*******************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * 目標設定ページ。ITスキル・資格・AD セミナーの目標を設定する。
 * ITスキル/資格 1件以上・AD 2件以上の条件を満たして完了すると棚卸が COMPLETED になる。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getGoals, saveGoals, completeGoal } from '../api/inventoryApi';
import { getItSkills, getQualifications, getAdSeminars } from '../../../shared/api/masterApi';
import type { ItSkill, Qualification, AdSeminar } from '../../../shared/types/master';
import NavBar from '../../../app/layouts/NavBar';
import { IconSave, IconCheck, IconPlus } from '../../../shared/ui/Icons';
import ConfirmDialog from '../../../shared/ui/ConfirmDialog';
import { useTranslation } from 'react-i18next';

type GoalCategory = 'IT_SKILL' | 'QUALIFICATION' | 'AD';

interface GoalRow {
  id?: number | null;
  goalCategory: GoalCategory;
  itSkillId?: number | null;
  qualificationId?: number | null;
  adSeminarId?: number | null;
  customName: string;
  targetPeriod: string;
  reason: string;
  isCustom: boolean;
}

const CATEGORY_KEY: Record<GoalCategory, string> = {
  IT_SKILL: 'goalPage.goalCategory.itSkill',
  QUALIFICATION: 'goalPage.goalCategory.qualification',
  AD: 'goalPage.goalCategory.ad',
};

/**
 * 目標設定ページ。
 *
 * ITスキル・資格・AD セミナーの目標を設定する。
 * ITスキル/資格 1件以上・AD 2件以上の条件を満たして完了すると棚卸が COMPLETED になる。
 */
export default function GoalPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation('inventory');
  const inventoryId = Number(id);

  const [goalRows, setGoalRows] = useState<GoalRow[]>([]);
  const [itSkills, setItSkills] = useState<ItSkill[]>([]);
  const [qualifications, setQualifications] = useState<Qualification[]>([]);
  const [adSeminars, setAdSeminars] = useState<AdSeminar[]>([]);
  const [isSaving, setIsSaving] = useState(false);
  const [isCompleting, setIsCompleting] = useState(false);
  const [showCompleteConfirm, setShowCompleteConfirm] = useState(false);
  const [error, setError] = useState('');
  const [saveMessage, setSaveMessage] = useState('');

  // 初期表示時に既存の目標データとマスタ情報を取得する
  useEffect(() => {
    Promise.all([
      getGoals(inventoryId),
      getItSkills(true),
      getQualifications(true),
      getAdSeminars(true),
    ]).then(([goalsRes, skillsRes, qualsRes, adsRes]) => {
      setItSkills(skillsRes.data);
      setQualifications(qualsRes.data);
      setAdSeminars(adsRes.data);
      setGoalRows(goalsRes.data.items.map(g => ({
        id: g.id,
        goalCategory: g.goalCategory as GoalCategory,
        itSkillId: g.itSkillId,
        qualificationId: g.qualificationId,
        adSeminarId: g.adSeminarId,
        customName: g.customName ?? '',
        targetPeriod: g.targetPeriod?.slice(0, 7) ?? '',
        reason: g.reason ?? '',
        isCustom: !g.itSkillId && !g.qualificationId && !g.adSeminarId,
      })));
    });
  }, [inventoryId]);

  const showMessage = (msg: string) => {
    setSaveMessage(msg);
    setTimeout(() => setSaveMessage(''), 3000);
  };

  const toApiItems = (rows: GoalRow[]) => rows.map(r => ({
    id: r.id,
    goalCategory: r.goalCategory,
    itSkillId: r.itSkillId,
    qualificationId: r.qualificationId,
    adSeminarId: r.adSeminarId,
    customName: r.customName || null,
    targetPeriod: r.targetPeriod ? `${r.targetPeriod}-01` : '',
    reason: r.reason || null,
  }));

  const handleSave = async () => {
    setIsSaving(true);
    try {
      await saveGoals(inventoryId, toApiItems(goalRows));
      showMessage(t('goalPage.saveSuccess'));
    } catch {
      showMessage(t('goalPage.saveFailed'));
    } finally {
      setIsSaving(false);
    }
  };

  const handleCompleteClick = () => setShowCompleteConfirm(true);

  const handleComplete = async () => {
    setShowCompleteConfirm(false);
    setIsCompleting(true);
    setError('');
    try {
      await saveGoals(inventoryId, toApiItems(goalRows));
      await completeGoal(inventoryId);
      navigate('/');
    } catch (e: unknown) {
      const axiosErr = e as { response?: { data?: { message?: string; errors?: { message: string }[] } } };
      const errData = axiosErr.response?.data;
      if (errData?.errors && errData.errors.length > 0) {
        setError(errData.errors.map((e: { message: string }) => e.message).join('\n'));
      } else {
        setError(errData?.message ?? t('goalPage.completeFailed'));
      }
    } finally {
      setIsCompleting(false);
    }
  };

  const addItSkillGoal = (isCustom = false) => {
    setGoalRows(prev => [...prev, {
      goalCategory: 'IT_SKILL', customName: '', targetPeriod: '', reason: '', isCustom,
    }]);
  };

  const addQualificationGoal = (isCustom = false) => {
    setGoalRows(prev => [...prev, {
      goalCategory: 'QUALIFICATION', customName: '', targetPeriod: '', reason: '', isCustom,
    }]);
  };

  const addAdGoal = () => {
    setGoalRows(prev => [...prev, {
      goalCategory: 'AD', customName: '', targetPeriod: '', reason: '', isCustom: false,
    }]);
  };

  const removeRow = (idx: number) => {
    setGoalRows(prev => prev.filter((_, i) => i !== idx));
  };

  const updateRow = (idx: number, patch: Partial<GoalRow>) => {
    setGoalRows(prev => prev.map((r, i) => i === idx ? { ...r, ...patch } : r));
  };

  const itOrQualCount = goalRows.filter(r => r.goalCategory === 'IT_SKILL' || r.goalCategory === 'QUALIFICATION').length;
  const adCount = goalRows.filter(r => r.goalCategory === 'AD').length;

  return (
    <div className="goal-page">
      <NavBar />
      <main className="goal-main">
        <button className="page-back-btn" onClick={() => navigate(`/inventory/${inventoryId}/goal-review`)}>{t('goalPage.backButton')}</button>
        <h1 className="page-title">{t('goalPage.title')}</h1>
        <p className="page-subtitle">
          {t('goalPage.subtitle')}
        </p>

        <div className="goal-counter">
          <span className={itOrQualCount >= 1 ? 'counter-ok' : 'counter-ng'}>
            {t('goalPage.counter.itQual', { count: itOrQualCount })} {itOrQualCount >= 1 ? '✓' : t('goalPage.counter.itQualRequired')}
          </span>
          <span className={adCount >= 2 ? 'counter-ok' : 'counter-ng'}>
            {t('goalPage.counter.ad', { count: adCount })} {adCount >= 2 ? '✓' : t('goalPage.counter.adRequired')}
          </span>
        </div>

        {saveMessage && <div className="save-message">{saveMessage}</div>}
        {error && <div className="error-message">{error}</div>}

        <div className="goal-list">
          {goalRows.map((row, idx) => (
            <div key={idx} className="goal-card">
              <div className="goal-card-header">
                <span className="goal-category-badge">{t(CATEGORY_KEY[row.goalCategory])}</span>
                <button className="remove-btn" onClick={() => removeRow(idx)}>✕</button>
              </div>
              <div className="goal-card-body">
                {row.goalCategory === 'IT_SKILL' && !row.isCustom && (
                  <select className="select" value={row.itSkillId ?? ''}
                    onChange={e => updateRow(idx, { itSkillId: Number(e.target.value) || null })}>
                    <option value="">{t('goalPage.form.selectSkill')}</option>
                    {itSkills.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                  </select>
                )}
                {row.goalCategory === 'QUALIFICATION' && !row.isCustom && (
                  <select className="select" value={row.qualificationId ?? ''}
                    onChange={e => updateRow(idx, { qualificationId: Number(e.target.value) || null })}>
                    <option value="">{t('goalPage.form.selectQualification')}</option>
                    {qualifications.map(q => <option key={q.id} value={q.id}>{q.name}</option>)}
                  </select>
                )}
                {row.goalCategory === 'AD' && (
                  <select className="select" value={row.adSeminarId ?? ''}
                    onChange={e => updateRow(idx, { adSeminarId: Number(e.target.value) || null })}>
                    <option value="">{t('goalPage.form.selectAd')}</option>
                    {adSeminars.map(a => <option key={a.id} value={a.id}>{a.name}</option>)}
                  </select>
                )}
                {row.isCustom && (
                  <input className="input" placeholder={t('goalPage.form.customNamePlaceholder')}
                    value={row.customName}
                    onChange={e => updateRow(idx, { customName: e.target.value })} />
                )}

                <label className="form-label">{t('goalPage.form.targetPeriodLabel')}</label>
                <input type="month" className="input" value={row.targetPeriod}
                  onChange={e => updateRow(idx, { targetPeriod: e.target.value })} />

                <label className="form-label">{t('goalPage.form.reasonLabel')}</label>
                <textarea className="textarea" placeholder={t('inventoryPage.table.optional')} value={row.reason}
                  onChange={e => updateRow(idx, { reason: e.target.value })} />
              </div>
            </div>
          ))}
        </div>

        <div className="goal-add-buttons">
          <button className="btn btn-secondary" onClick={() => addItSkillGoal(false)}><IconPlus size={13} />{t('goalPage.addButton.itSkill')}</button>
          <button className="btn btn-secondary" onClick={() => addItSkillGoal(true)}><IconPlus size={13} />{t('goalPage.addButton.itSkillCustom')}</button>
          <button className="btn btn-secondary" onClick={() => addQualificationGoal(false)}><IconPlus size={13} />{t('goalPage.addButton.qualification')}</button>
          <button className="btn btn-secondary" onClick={() => addQualificationGoal(true)}><IconPlus size={13} />{t('goalPage.addButton.qualificationCustom')}</button>
          <button className="btn btn-secondary" onClick={addAdGoal}><IconPlus size={13} />{t('goalPage.addButton.ad')}</button>
        </div>

        <div className="action-row">
          <button className="btn btn-secondary" onClick={handleSave} disabled={isSaving}>
            <IconSave size={15} />
            {isSaving ? t('inventoryPage.savingButton') : t('inventoryPage.saveButton')}
          </button>
          <button className="btn btn-primary" onClick={handleCompleteClick} disabled={isCompleting}>
            <IconCheck size={15} />
            {isCompleting ? t('goalPage.completingButton') : t('goalPage.completeButton')}
          </button>
        </div>
      </main>

      {showCompleteConfirm && (
        <ConfirmDialog
          title={t('goalPage.completeConfirm.title')}
          message={t('goalPage.completeConfirm.message')}
          confirmLabel={t('goalPage.completeConfirm.confirmLabel')}
          onConfirm={handleComplete}
          onCancel={() => setShowCompleteConfirm(false)}
        />
      )}
    </div>
  );
}
