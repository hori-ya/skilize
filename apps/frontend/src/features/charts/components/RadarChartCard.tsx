/*******************************************************************************
 * 機能ID      ：CHT
 * 機能名      ：グラフ・チャート
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * スキルバランスレーダーチャートカード。
 * ITスキル大分類ごとの平均スキルレベルをレーダーチャートで表示する。
 * 前年度データがある場合は比較表示する。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import {
  ResponsiveContainer, RadarChart, PolarGrid, PolarAngleAxis,
  PolarRadiusAxis, Radar, Legend, Tooltip,
} from 'recharts';
import type { RadarResponse } from '../types/index';

interface Props {
  data: RadarResponse;
}

/**
 * スキルバランスレーダーチャートカード。
 *
 * ITスキル大分類の平均スコアをレーダーチャートで可視化する。
 * 前年度データがある場合は重ねて比較表示する。
 */
export default function RadarChartCard({ data }: Props) {
  const { currentFiscalYear, prevFiscalYear, hasCurrentYearData, maxScoreWeight, axes } = data;

  const chartData = axes.map(a => ({
    subject: a.category1Name.length > 8 ? a.category1Name.slice(0, 8) + '…' : a.category1Name,
    current: a.currentAvgScore,
    prev: a.prevAvgScore ?? 0,
    fullName: a.category1Name,
  }));

  const showPrev = prevFiscalYear !== null;

  return (
    <div className="chart-card">
      <p className="chart-card__title">
        スキルバランス
        {currentFiscalYear && <span className="chart-card__fy">（{currentFiscalYear}）</span>}
      </p>

      {!hasCurrentYearData ? (
        <div className="chart-no-data">今年度の採点データがありません</div>
      ) : axes.length < 3 ? (
        <div className="chart-no-data">分類1が3件以上あるとレーダー表示されます</div>
      ) : (
        <ResponsiveContainer width="100%" height={280}>
          <RadarChart data={chartData} margin={{ top: 8, right: 24, bottom: 8, left: 24 }}>
            <PolarGrid stroke="var(--color-border)" />
            <PolarAngleAxis dataKey="subject" tick={{ fontSize: 11, fill: 'var(--color-text-muted)' }} />
            <PolarRadiusAxis
              domain={[0, maxScoreWeight]}
              tickCount={maxScoreWeight + 1}
              tick={{ fontSize: 10, fill: 'var(--color-text-muted)' }}
            />
            <Tooltip
              formatter={(value) => [typeof value === 'number' ? value.toFixed(1) : value]}
              contentStyle={{ fontSize: 12, borderRadius: 8, border: '1px solid var(--color-border)' }}
            />
            {showPrev && (
              <Radar
                name={prevFiscalYear!}
                dataKey="prev"
                stroke="#94a3b8"
                fill="#94a3b8"
                fillOpacity={0.18}
                strokeDasharray="4 3"
              />
            )}
            <Radar
              name={currentFiscalYear ?? '今年度'}
              dataKey="current"
              stroke="var(--color-primary)"
              fill="var(--color-primary)"
              fillOpacity={0.28}
            />
            {showPrev && <Legend wrapperStyle={{ fontSize: 12 }} />}
          </RadarChart>
        </ResponsiveContainer>
      )}
    </div>
  );
}
