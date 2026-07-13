/*******************************************************************************
 * 機能ID      ：CHT
 * 機能名      ：グラフ・チャート
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * ラーニングタイムラインカード。
 * 資格取得・セミナー受講の実績と今年度の目標を時系列で表示する。
 * 実績（ACHIEVEMENT）と学習活動（ACTIVITY）の2レーンで構成される。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import type { TimelineEvent, TimelineEventType } from '../types/index';

interface Props {
  events: TimelineEvent[];
}

const EVENT_LABEL: Record<TimelineEventType, string> = {
  QUALIFICATION: '資格取得',
  AD_SEMINAR: 'ADセミナー',
  FREE_SEMINAR: '自由セミナー',
  GOAL_QUALIFICATION: '目標：資格',
  GOAL_IT_SKILL: '目標：ITスキル',
  GOAL_AD: '目標：AD',
};

const EVENT_ICON: Record<TimelineEventType, string> = {
  QUALIFICATION: '🏅',
  AD_SEMINAR: '📚',
  FREE_SEMINAR: '🎓',
  GOAL_QUALIFICATION: '🎯',
  GOAL_IT_SKILL: '🎯',
  GOAL_AD: '🎯',
};

/**
 * 年月文字列（YYYY-MM）を日本語表示形式（YYYY年M月）に変換する。
 *
 * @param yearMonth 年月文字列（例: "2025-04"）
 * @returns 日本語表示文字列（例: "2025年4月"）
 */
function formatYearMonth(yearMonth: string): string {
  const [year, month] = yearMonth.split('-');
  return `${year}年${parseInt(month)}月`;
}

/**
 * ラーニングタイムラインカード。
 *
 * 資格取得・セミナー受講の実績と目標を時系列で表示する。
 * ACHIEVEMENT レーンと ACTIVITY レーンの2段構成で描画する。
 */
export default function TimelineChartCard({ events }: Props) {
  if (events.length === 0) {
    return (
      <div className="chart-card chart-card--full">
        <p className="chart-card__title">ラーニングタイムライン</p>
        <div className="chart-no-data">記録された実績・目標がありません</div>
      </div>
    );
  }

  const achievement: TimelineEvent[] = [];
  const activity: TimelineEvent[] = [];
  for (const e of events) {
    if (e.lane === 'ACHIEVEMENT') {
      achievement.push(e);
    } else if (e.lane === 'ACTIVITY') {
      activity.push(e);
    }
  }

  return (
    <div className="chart-card chart-card--full">
      <p className="chart-card__title">ラーニングタイムライン</p>
      <div className="timeline">
        <TimelineLane label="資格取得" events={achievement} colorClass="timeline-lane--achievement" />
        <TimelineLane label="学習活動" events={activity} colorClass="timeline-lane--activity" />
      </div>
    </div>
  );
}

function TimelineLane({
  label,
  events,
  colorClass,
}: {
  label: string;
  events: TimelineEvent[];
  colorClass: string;
}) {
  let content: React.ReactNode;
  if (events.length === 0) {
    content = <div className="timeline-lane__empty">記録なし</div>;
  } else {
    const eventItems: React.ReactNode[] = [];
    for (let i = 0; i < events.length; i++) {
      const ev = events[i];
      let eventClassName = 'timeline-event';
      if (!ev.isPast) {
        eventClassName += ' timeline-event--goal';
      }
      eventItems.push(
        <div key={i} className={eventClassName}>
          <div className="timeline-event__dot" />
          <div className="timeline-event__body">
            <span className="timeline-event__icon">{EVENT_ICON[ev.type]}</span>
            <span className="timeline-event__name">{ev.name}</span>
            <span className="timeline-event__meta">
              {formatYearMonth(ev.yearMonth)}
              {!ev.isPast && <span className="timeline-event__tag">目標</span>}
            </span>
            <span className="timeline-event__type">{EVENT_LABEL[ev.type]}</span>
          </div>
        </div>,
      );
    }
    content = (
      <div className="timeline-lane__track">
        <div className="timeline-lane__line" />
        <div className="timeline-lane__events">
          {eventItems}
        </div>
      </div>
    );
  }

  return (
    <div className={`timeline-lane ${colorClass}`}>
      <div className="timeline-lane__header">{label}</div>
      {content}
    </div>
  );
}
