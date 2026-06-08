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
import type { GrowthResponse } from '../types/index';

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

  const chartData = fiscalYears.map((year, i) => {
    const point: Record<string, string | number> = { year };
    series.forEach(s => {
      point[s.category1Name] = s.yearlyTotalScores[i] ?? 0;
    });
    return point;
  });

  const activeSeries = series.filter(s =>
    s.yearlyTotalScores.some(v => v > 0)
  );

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
          {activeSeries.map((s, i) => (
            <Bar
              key={s.category1Id}
              dataKey={s.category1Name}
              stackId="stack"
              fill={PALETTE[i % PALETTE.length]}
              radius={i === activeSeries.length - 1 ? [3, 3, 0, 0] : undefined}
            />
          ))}
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
