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

export type TimelineEventType =
  | 'QUALIFICATION'
  | 'AD_SEMINAR'
  | 'FREE_SEMINAR'
  | 'GOAL_QUALIFICATION'
  | 'GOAL_IT_SKILL'
  | 'GOAL_AD';

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
