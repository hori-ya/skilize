/*******************************************************************************
 * 機能ID      ：CHT
 * 機能名      ：グラフ・チャート
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * スキル成長推移棒グラフカード。
 * 年度別のスキルレベル合計を大分類ごとの積み上げ棒グラフで表示する。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import {
  ResponsiveContainer, BarChart, Bar, XAxis, YAxis,
  CartesianGrid, Tooltip, Legend,
} from 'recharts';
import type { GrowthResponse, GrowthSeries } from '../types/index';

const PALETTE = [
  '#3d6db3', '#bf7a3a', '#149b78', '#c05040',
  '#6b42a8', '#2d8fb3', '#a87d42', '#4a8540',
];

interface Props {
  data: GrowthResponse;
}

/**
 * スキル成長推移棒グラフカード。
 *
 * 年度別・大分類別のスキルレベル合計を積み上げ棒グラフで表示する。
 * 提出済みの棚卸データがない場合はデータなしメッセージを表示する。
 */
export default function GrowthChartCard({ data }: Props) {
  const { fiscalYears, series } = data;

  if (fiscalYears.length === 0) {
    return (
      <div className="chart-card">
        <p className="chart-card__title">スキル成長推移</p>
        <div className="chart-no-data">提出済みの棚卸データがありません</div>
      </div>
    );
  }

  const chartData: Record<string, string | number>[] = [];
  for (let i = 0; i < fiscalYears.length; i++) {
    const point: Record<string, string | number> = { year: fiscalYears[i] };
    for (const s of series) {
      let score = 0;
      if (s.yearlyTotalScores[i] != null) {
        score = s.yearlyTotalScores[i];
      }
      point[s.category1Name] = score;
    }
    chartData.push(point);
  }

  // 全年度でスコアが0の系列は凡例・グラフから除外する
  const activeSeries: GrowthSeries[] = [];
  for (const s of series) {
    let hasScore = false;
    for (const v of s.yearlyTotalScores) {
      if (v > 0) {
        hasScore = true;
        break;
      }
    }
    if (hasScore) {
      activeSeries.push(s);
    }
  }

  const bars: React.ReactNode[] = [];
  for (let i = 0; i < activeSeries.length; i++) {
    const s = activeSeries[i];
    let radius: [number, number, number, number] | undefined;
    if (i === activeSeries.length - 1) {
      radius = [3, 3, 0, 0];
    }
    bars.push(
      <Bar
        key={s.category1Id}
        dataKey={s.category1Name}
        stackId="stack"
        fill={PALETTE[i % PALETTE.length]}
        radius={radius}
      />,
    );
  }

  return (
    <div className="chart-card">
      <p className="chart-card__title">スキル成長推移</p>
      <ResponsiveContainer width="100%" height={280}>
        <BarChart data={chartData} margin={{ top: 8, right: 16, bottom: 8, left: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" vertical={false} />
          <XAxis dataKey="year" tick={{ fontSize: 12, fill: 'var(--color-text-muted)' }} />
          <YAxis tick={{ fontSize: 11, fill: 'var(--color-text-muted)' }} allowDecimals={false} />
          <Tooltip
            contentStyle={{ fontSize: 12, borderRadius: 8, border: '1px solid var(--color-border)' }}
          />
          <Legend wrapperStyle={{ fontSize: 11 }} />
          {bars}
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
