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

/** 画面上の振り返り入力状態を保存用のリクエスト配列に変換する。 */
function buildReviewItems(reviewState: Record<number, ReviewState>): { prevGoalId: number; achievementStatus: string | null; reviewNote: string | null }[] {
  const items: { prevGoalId: number; achievementStatus: string | null; reviewNote: string | null }[] = [];
  for (const [prevGoalId, state] of Object.entries(reviewState)) {
    items.push({
      prevGoalId: Number(prevGoalId),
      achievementStatus: state.achievementStatus || null,
      reviewNote: state.reviewNote || null,
    });
  }
  return items;
}

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
      for (const item of res.data.items) {
        let achievementStatus: AchievementStatus | '' = '';
        if (item.achievementStatus != null) {
          achievementStatus = item.achievementStatus;
        }
        let reviewNote = '';
        if (item.reviewNote != null) {
          reviewNote = item.reviewNote;
        }
        initial[item.prevGoalId] = { achievementStatus, reviewNote };
      }
      setReviewState(initial);
    });
  }, [inventoryId]);

  const handleSave = async () => {
    setIsSaving(true);
    try {
      const items = buildReviewItems(reviewState);
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
      const items = buildReviewItems(reviewState);
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

  const achievementOptionElements: React.ReactNode[] = [];
  for (const opt of ACHIEVEMENT_OPTIONS) {
    achievementOptionElements.push(<option key={opt.value} value={opt.value}>{opt.label}</option>);
  }

  const goalReviewCards: React.ReactNode[] = [];
  for (const item of goalReview.items) {
    let state = reviewState[item.prevGoalId];
    if (state == null) {
      state = { achievementStatus: '', reviewNote: '' };
    }
    let goalCategoryKey: string = item.goalCategory;
    if (GOAL_CATEGORY_KEY[item.goalCategory] != null) {
      goalCategoryKey = GOAL_CATEGORY_KEY[item.goalCategory];
    }
    let targetPeriodLabel = '';
    if (item.targetPeriod != null) {
      targetPeriodLabel = item.targetPeriod.slice(0, 7);
    }
    const currentState = state;
    goalReviewCards.push(
      <div key={item.prevGoalId} className="goal-review-card">
        <div className="goal-review-header">
          <span className="goal-category-badge">{t(goalCategoryKey)}</span>
          <span className="goal-name">{item.goalName}</span>
          <span className="goal-period">{t('goalReviewPage.goalPeriodLabel')}{targetPeriodLabel}</span>
        </div>
        {item.reason && <p className="goal-reason">{t('goalReviewPage.reasonPrefix')}{item.reason}</p>}
        <div className="goal-review-inputs">
          <label className="form-label">{t('goalReviewPage.achievementLabel')}</label>
          <select
            className="select"
            value={currentState.achievementStatus}
            onChange={e => setReviewState(prev => ({
              ...prev,
              [item.prevGoalId]: { ...currentState, achievementStatus: e.target.value as AchievementStatus | '' },
            }))}
          >
            {achievementOptionElements}
          </select>
          <label className="form-label">{t('goalReviewPage.reviewNoteLabel')}</label>
          <textarea
            className="textarea"
            placeholder={t('inventoryPage.table.optional')}
            value={currentState.reviewNote}
            onChange={e => setReviewState(prev => ({
              ...prev,
              [item.prevGoalId]: { ...currentState, reviewNote: e.target.value },
            }))}
          />
        </div>
      </div>,
    );
  }

  let saveButtonLabel = t('inventoryPage.saveButton');
  if (isSaving) {
    saveButtonLabel = t('inventoryPage.savingButton');
  }

  let completeButtonLabel = t('goalReviewPage.completeButton');
  if (isCompleting) {
    completeButtonLabel = t('goalReviewPage.completingButton');
  }

  return (
    <div className="goal-review-page">
      <NavBar />
      <main className="goal-review-main">
        <button className="page-back-btn" onClick={() => navigate(`/inventory/${inventoryId}/comparison`)}>{t('goalReviewPage.backButton')}</button>
        <h1 className="page-title">{t('goalReviewPage.title')}</h1>
        <p className="page-subtitle">{t('goalReviewPage.subtitle', { prevYear: goalReview.prevFiscalYear })}</p>

        <div className="goal-review-list">
          {goalReviewCards}
        </div>

        <div className="action-row">
          <button className="btn btn-secondary" onClick={handleSave} disabled={isSaving}>
            <IconSave size={15} />
            {saveButtonLabel}
          </button>
          <button className="btn btn-primary" onClick={handleCompleteClick} disabled={isCompleting}>
            <IconCheck size={15} />
            {completeButtonLabel}
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
