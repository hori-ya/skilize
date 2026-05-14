import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getGoalReview, saveGoalReview, completeGoalReview } from '../api/inventoryApi';
import type { GoalReviewResponse, AchievementStatus } from '../types/index';
import NavBar from '../../../app/layouts/NavBar';
import { IconSave, IconCheck } from '../../../shared/ui/Icons';

const ACHIEVEMENT_OPTIONS: { value: AchievementStatus | ''; label: string }[] = [
  { value: '', label: '（未選択）' },
  { value: 'ACHIEVED', label: '達成' },
  { value: 'PARTIAL', label: '一部達成' },
  { value: 'NOT_ACHIEVED', label: '未達成' },
];

const GOAL_CATEGORY_LABEL: Record<string, string> = {
  IT_SKILL: 'ITスキル',
  QUALIFICATION: '資格',
  AD: 'AD',
};

interface ReviewState {
  achievementStatus: AchievementStatus | '';
  reviewNote: string;
}

export default function GoalReviewPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const inventoryId = Number(id);

  const [goalReview, setGoalReview] = useState<GoalReviewResponse | null>(null);
  const [reviewState, setReviewState] = useState<Record<number, ReviewState>>({});
  const [isSaving, setIsSaving] = useState(false);
  const [isCompleting, setIsCompleting] = useState(false);

  useEffect(() => {
    getGoalReview(inventoryId).then(res => {
      setGoalReview(res.data);
      const initial: Record<number, ReviewState> = {};
      res.data.items.forEach(item => {
        initial[item.prevGoalId] = {
          achievementStatus: (item.achievementStatus ?? '') as AchievementStatus | '',
          reviewNote: item.reviewNote ?? '',
        };
      });
      setReviewState(initial);
    });
  }, [inventoryId]);

  const handleSave = async () => {
    setIsSaving(true);
    try {
      const items = Object.entries(reviewState).map(([prevGoalId, state]) => ({
        prevGoalId: Number(prevGoalId),
        achievementStatus: state.achievementStatus || null,
        reviewNote: state.reviewNote || null,
      }));
      const res = await saveGoalReview(inventoryId, items);
      setGoalReview(res.data);
    } finally {
      setIsSaving(false);
    }
  };

  const handleComplete = async () => {
    setIsCompleting(true);
    try {
      // Save first
      const items = Object.entries(reviewState).map(([prevGoalId, state]) => ({
        prevGoalId: Number(prevGoalId),
        achievementStatus: state.achievementStatus || null,
        reviewNote: state.reviewNote || null,
      }));
      await saveGoalReview(inventoryId, items);
      await completeGoalReview(inventoryId);
      navigate(`/inventory/${inventoryId}/goals`);
    } finally {
      setIsCompleting(false);
    }
  };

  if (!goalReview) return <div className="loading">読み込み中...</div>;

  if (!goalReview.hasPrevGoals) {
    navigate(`/inventory/${inventoryId}/goals`);
    return null;
  }

  return (
    <div className="goal-review-page">
      <NavBar />
      <main className="goal-review-main">
        <button className="page-back-btn" onClick={() => navigate(`/inventory/${inventoryId}/comparison`)}>← 前年度比較に戻る</button>
        <h1 className="page-title">前回目標の振り返り</h1>
        <p className="page-subtitle">前年度（{goalReview.prevFiscalYear}）に設定した目標を振り返ってください。入力は任意です。</p>

        <div className="goal-review-list">
          {goalReview.items.map(item => {
            const state = reviewState[item.prevGoalId] ?? { achievementStatus: '', reviewNote: '' };
            return (
              <div key={item.prevGoalId} className="goal-review-card">
                <div className="goal-review-header">
                  <span className="goal-category-badge">{GOAL_CATEGORY_LABEL[item.goalCategory]}</span>
                  <span className="goal-name">{item.goalName}</span>
                  <span className="goal-period">目標時期：{item.targetPeriod?.slice(0, 7)}</span>
                </div>
                {item.reason && <p className="goal-reason">理由：{item.reason}</p>}
                <div className="goal-review-inputs">
                  <label className="form-label">達成状況</label>
                  <select
                    className="select"
                    value={state.achievementStatus}
                    onChange={e => setReviewState(prev => ({
                      ...prev,
                      [item.prevGoalId]: { ...state, achievementStatus: e.target.value as AchievementStatus | '' },
                    }))}
                  >
                    {ACHIEVEMENT_OPTIONS.map(opt => (
                      <option key={opt.value} value={opt.value}>{opt.label}</option>
                    ))}
                  </select>
                  <label className="form-label">振り返りコメント</label>
                  <textarea
                    className="textarea"
                    placeholder="任意"
                    value={state.reviewNote}
                    onChange={e => setReviewState(prev => ({
                      ...prev,
                      [item.prevGoalId]: { ...state, reviewNote: e.target.value },
                    }))}
                  />
                </div>
              </div>
            );
          })}
        </div>

        <div className="action-row">
          <button className="btn btn-secondary" onClick={handleSave} disabled={isSaving}>
            <IconSave size={15} />
            {isSaving ? '保存中...' : '一時保存'}
          </button>
          <button className="btn btn-primary" onClick={handleComplete} disabled={isCompleting}>
            <IconCheck size={15} />
            {isCompleting ? '完了中...' : '振り返りを完了して目標設定へ'}
          </button>
        </div>
      </main>
    </div>
  );
}
