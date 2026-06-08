/*******************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * 前年度目標の振り返りページ。前年度に設定した目標の達成状況と所感を入力する。
 * 振り返り完了後は目標設定ページへ遷移する。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getGoalReview, saveGoalReview, completeGoalReview } from '../api/inventoryApi';
import type { GoalReviewResponse, AchievementStatus } from '../types/index';
import NavBar from '../../../app/layouts/NavBar';
import { IconSave, IconCheck } from '../../../shared/ui/Icons';
import ConfirmDialog from '../../../shared/ui/ConfirmDialog';
import { useTranslation } from 'react-i18next';

type ReviewState = {
  achievementStatus: AchievementStatus | '';
  reviewNote: string;
};

const GOAL_CATEGORY_KEY: Record<string, string> = {
  IT_SKILL: 'goalReviewPage.goalCategory.itSkill',
  QUALIFICATION: 'goalReviewPage.goalCategory.qualification',
  AD: 'goalReviewPage.goalCategory.ad',
};

/**
 * 前年度目標の振り返りページ。
 *
 * 前年度に設定した目標ごとに達成状況（ACHIEVED / PARTIAL / NOT_ACHIEVED）と
 * 振り返りコメントを入力する。振り返り完了後は目標設定ページへ遷移する。
 */
export default function GoalReviewPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation('inventory');
  const inventoryId = Number(id);

  const [goalReview, setGoalReview] = useState<GoalReviewResponse | null>(null);
  const [reviewState, setReviewState] = useState<Record<number, ReviewState>>({});
  const [isSaving, setIsSaving] = useState(false);
  const [isCompleting, setIsCompleting] = useState(false);
  const [showCompleteConfirm, setShowCompleteConfirm] = useState(false);

  const ACHIEVEMENT_OPTIONS: { value: AchievementStatus | ''; label: string }[] = [
    { value: '', label: t('goalReviewPage.achievement.unselected') },
    { value: 'ACHIEVED', label: t('goalReviewPage.achievement.achieved') },
    { value: 'PARTIAL', label: t('goalReviewPage.achievement.partial') },
    { value: 'NOT_ACHIEVED', label: t('goalReviewPage.achievement.notAchieved') },
  ];

  // 初期表示時に前年度目標の振り返りデータを取得する
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

  const handleCompleteClick = () => setShowCompleteConfirm(true);

  const handleComplete = async () => {
    setShowCompleteConfirm(false);
    setIsCompleting(true);
    try {
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

  if (!goalReview) return <div className="loading">{t('loading')}</div>;

  if (!goalReview.hasPrevGoals) {
    navigate(`/inventory/${inventoryId}/goals`);
    return null;
  }

  return (
    <div className="goal-review-page">
      <NavBar />
      <main className="goal-review-main">
        <button className="page-back-btn" onClick={() => navigate(`/inventory/${inventoryId}/comparison`)}>{t('goalReviewPage.backButton')}</button>
        <h1 className="page-title">{t('goalReviewPage.title')}</h1>
        <p className="page-subtitle">{t('goalReviewPage.subtitle', { prevYear: goalReview.prevFiscalYear })}</p>

        <div className="goal-review-list">
          {goalReview.items.map(item => {
            const state = reviewState[item.prevGoalId] ?? { achievementStatus: '', reviewNote: '' };
            return (
              <div key={item.prevGoalId} className="goal-review-card">
                <div className="goal-review-header">
                  <span className="goal-category-badge">{t(GOAL_CATEGORY_KEY[item.goalCategory] ?? item.goalCategory)}</span>
                  <span className="goal-name">{item.goalName}</span>
                  <span className="goal-period">{t('goalReviewPage.goalPeriodLabel')}{item.targetPeriod?.slice(0, 7)}</span>
                </div>
                {item.reason && <p className="goal-reason">{t('goalReviewPage.reasonPrefix')}{item.reason}</p>}
                <div className="goal-review-inputs">
                  <label className="form-label">{t('goalReviewPage.achievementLabel')}</label>
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
                  <label className="form-label">{t('goalReviewPage.reviewNoteLabel')}</label>
                  <textarea
                    className="textarea"
                    placeholder={t('inventoryPage.table.optional')}
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
            {isSaving ? t('inventoryPage.savingButton') : t('inventoryPage.saveButton')}
          </button>
          <button className="btn btn-primary" onClick={handleCompleteClick} disabled={isCompleting}>
            <IconCheck size={15} />
            {isCompleting ? t('goalReviewPage.completingButton') : t('goalReviewPage.completeButton')}
          </button>
        </div>
      </main>

      {showCompleteConfirm && (
        <ConfirmDialog
          title={t('goalReviewPage.completeConfirm.title')}
          message={t('goalReviewPage.completeConfirm.message')}
          confirmLabel={t('goalReviewPage.completeConfirm.confirmLabel')}
          onConfirm={handleComplete}
          onCancel={() => setShowCompleteConfirm(false)}
        />
      )}
    </div>
  );
}
