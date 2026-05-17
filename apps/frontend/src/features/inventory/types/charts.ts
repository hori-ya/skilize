/**
 * グラフ用の型定義。バックエンド ChartService のレスポンスと 1:1 対応する。
 * レーダー（radar）・成長推移（growth）・ヒートマップ（heatmap）・タイムライン（timeline）の4種類。
 */

export interface RadarAxis {
  category1Id: number;
  category1Name: string;
  currentAvgScore: number;
  prevAvgScore: number | null;
}

export interface RadarResponse {
  currentFiscalYear: string | null;
  prevFiscalYear: string | null;
  hasCurrentYearData: boolean;
  maxLevelValue: number;
  axes: RadarAxis[];
}

export interface GrowthSeries {
  category1Id: number;
  category1Name: string;
  yearlyTotalScores: number[];
}

export interface GrowthResponse {
  fiscalYears: string[];
  series: GrowthSeries[];
}

export interface HeatmapSkill {
  skillName: string;
  levelValue: number | null;
}

export interface HeatmapCell {
  category2Id: number | null;
  category2Name: string;
  avgLevelValue: number | null;
  scoredSkillCount: number;
  skills: HeatmapSkill[];
}

export interface HeatmapRow {
  category1Id: number;
  category1Name: string;
  cells: HeatmapCell[];
}

export interface HeatmapResponse {
  currentFiscalYear: string | null;
  hasCurrentYearData: boolean;
  maxLevelValue: number;
  rows: HeatmapRow[];
}

/**
 * タイムラインのイベント種別。
 * QUALIFICATION / AD_SEMINAR / FREE_SEMINAR: 過去の実績（取得・受講済み）
 * GOAL_*: 今年度の目標（未来の予定）
 */
export type TimelineEventType =
  | 'QUALIFICATION'
  | 'AD_SEMINAR'
  | 'FREE_SEMINAR'
  | 'GOAL_QUALIFICATION'
  | 'GOAL_IT_SKILL'
  | 'GOAL_AD';

/**
 * タイムラインのレーン（行）。
 * ACHIEVEMENT: 資格取得など達成事項のレーン
 * ACTIVITY:    セミナー受講など活動のレーン
 */
export type TimelineLane = 'ACHIEVEMENT' | 'ACTIVITY';

export interface TimelineEvent {
  type: TimelineEventType;
  lane: TimelineLane;
  name: string;
  yearMonth: string;
  isPast: boolean;
}

export interface TimelineResponse {
  events: TimelineEvent[];
}
