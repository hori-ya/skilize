import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getGoals, saveGoals, completeGoal } from '../api/inventoryApi';
import { getItSkills, getQualifications, getAdSeminars } from '../../../shared/api/masterApi';
import type { ItSkill, Qualification, AdSeminar } from '../../../shared/types/master';
import NavBar from '../../../app/layouts/NavBar';
import { IconSave, IconCheck, IconPlus } from '../../../shared/ui/Icons';
import ConfirmDialog from '../../../shared/ui/ConfirmDialog';

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

const CATEGORY_LABEL: Record<GoalCategory, string> = {
  IT_SKILL: 'ITスキル',
  QUALIFICATION: '資格',
  AD: 'AD',
};

export default function GoalPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
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
      showMessage('目標を保存しました');
    } catch {
      showMessage('保存に失敗しました');
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
        setError(errData?.message ?? '完了処理に失敗しました');
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
        <button className="page-back-btn" onClick={() => navigate(`/inventory/${inventoryId}/goal-review`)}>← 目標振り返りに戻る</button>
        <h1 className="page-title">目標設定</h1>
        <p className="page-subtitle">
          ITスキル・資格の目標を1件以上、ADの目標を2件設定してください。
        </p>

        <div className="goal-counter">
          <span className={itOrQualCount >= 1 ? 'counter-ok' : 'counter-ng'}>
            ITスキル・資格：{itOrQualCount}件 {itOrQualCount >= 1 ? '✓' : '（1件以上必要）'}
          </span>
          <span className={adCount >= 2 ? 'counter-ok' : 'counter-ng'}>
            AD：{adCount}件 {adCount >= 2 ? '✓' : '（2件必要）'}
          </span>
        </div>

        {saveMessage && <div className="save-message">{saveMessage}</div>}
        {error && <div className="error-message">{error}</div>}

        <div className="goal-list">
          {goalRows.map((row, idx) => (
            <div key={idx} className="goal-card">
              <div className="goal-card-header">
                <span className="goal-category-badge">{CATEGORY_LABEL[row.goalCategory]}</span>
                <button className="remove-btn" onClick={() => removeRow(idx)}>✕</button>
              </div>
              <div className="goal-card-body">
                {/* Target selection */}
                {row.goalCategory === 'IT_SKILL' && !row.isCustom && (
                  <select className="select" value={row.itSkillId ?? ''}
                    onChange={e => updateRow(idx, { itSkillId: Number(e.target.value) || null })}>
                    <option value="">スキルを選択</option>
                    {itSkills.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                  </select>
                )}
                {row.goalCategory === 'QUALIFICATION' && !row.isCustom && (
                  <select className="select" value={row.qualificationId ?? ''}
                    onChange={e => updateRow(idx, { qualificationId: Number(e.target.value) || null })}>
                    <option value="">資格を選択</option>
                    {qualifications.map(q => <option key={q.id} value={q.id}>{q.name}</option>)}
                  </select>
                )}
                {row.goalCategory === 'AD' && (
                  <select className="select" value={row.adSeminarId ?? ''}
                    onChange={e => updateRow(idx, { adSeminarId: Number(e.target.value) || null })}>
                    <option value="">ADを選択</option>
                    {adSeminars.map(a => <option key={a.id} value={a.id}>{a.name}</option>)}
                  </select>
                )}
                {row.isCustom && (
                  <input className="input" placeholder="目標名（自由入力）"
                    value={row.customName}
                    onChange={e => updateRow(idx, { customName: e.target.value })} />
                )}

                <label className="form-label">達成・予定時期</label>
                <input type="month" className="input" value={row.targetPeriod}
                  onChange={e => updateRow(idx, { targetPeriod: e.target.value })} />

                <label className="form-label">理由・計画（任意）</label>
                <textarea className="textarea" placeholder="任意" value={row.reason}
                  onChange={e => updateRow(idx, { reason: e.target.value })} />
              </div>
            </div>
          ))}
        </div>

        <div className="goal-add-buttons">
          <button className="btn btn-secondary" onClick={() => addItSkillGoal(false)}><IconPlus size={13} />ITスキル目標</button>
          <button className="btn btn-secondary" onClick={() => addItSkillGoal(true)}><IconPlus size={13} />ITスキル目標（自由入力）</button>
          <button className="btn btn-secondary" onClick={() => addQualificationGoal(false)}><IconPlus size={13} />資格目標</button>
          <button className="btn btn-secondary" onClick={() => addQualificationGoal(true)}><IconPlus size={13} />資格目標（自由入力）</button>
          <button className="btn btn-secondary" onClick={addAdGoal}><IconPlus size={13} />AD目標</button>
        </div>

        <div className="action-row">
          <button className="btn btn-secondary" onClick={handleSave} disabled={isSaving}>
            <IconSave size={15} />
            {isSaving ? '保存中...' : '一時保存'}
          </button>
          <button className="btn btn-primary" onClick={handleCompleteClick} disabled={isCompleting}>
            <IconCheck size={15} />
            {isCompleting ? '完了中...' : '目標設定を完了してダッシュボードへ'}
          </button>
        </div>
      </main>

      {showCompleteConfirm && (
        <ConfirmDialog
          title="目標設定の完了"
          message="目標設定を完了してダッシュボードに戻りますか？"
          confirmLabel="完了する"
          onConfirm={handleComplete}
          onCancel={() => setShowCompleteConfirm(false)}
        />
      )}
    </div>
  );
}
