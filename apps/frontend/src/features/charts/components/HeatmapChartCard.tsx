/*******************************************************************************
 * 機能ID      ：CHT
 * 機能名      ：グラフ・チャート
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * スキル分布ヒートマップカード。
 * ITスキル大分類・中分類ごとの平均スキルレベルを色の濃淡で表示する。
 * セルにホバーするとスキル詳細のツールチップを表示する。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import { useState } from 'react';
import { createPortal } from 'react-dom';
import type { HeatmapCell, HeatmapResponse, HeatmapSkill } from '../types/index';

interface Props {
  data: HeatmapResponse;
}

interface TooltipState {
  cell: HeatmapCell;
  x: number;
  y: number;
}

/**
 * セルの背景色を平均スキルレベルに応じて返す。
 *
 * @param avgLevelValue セルの平均スキルレベル値（null または 0 の場合は透明）
 * @param maxLevelValue スキルレベルの最大値
 * @returns CSS カラー文字列
 */
function cellColor(avgLevelValue: number | null, maxLevelValue: number): string {
  if (avgLevelValue === null || avgLevelValue === 0) return 'transparent';
  const ratio = Math.min(avgLevelValue / maxLevelValue, 1);
  const opacity = 0.12 + ratio * 0.68;
  return `rgba(61, 109, 179, ${opacity.toFixed(2)})`;
}

/**
 * セルのテキスト色を平均スキルレベルに応じて返す。
 *
 * @param avgLevelValue セルの平均スキルレベル値
 * @param maxLevelValue スキルレベルの最大値
 * @returns CSS カラー文字列
 */
function cellTextColor(avgLevelValue: number | null, maxLevelValue: number): string {
  if (avgLevelValue === null || avgLevelValue === 0) return 'var(--color-text-muted)';
  const ratio = Math.min(avgLevelValue / maxLevelValue, 1);
  return ratio >= 0.6 ? '#fff' : 'var(--color-text)';
}

/**
 * スキル分布ヒートマップカード。
 *
 * ITスキルの大分類・中分類ごとの平均スキルレベルを色の濃淡で可視化する。
 * セルにホバーするとスキル詳細のツールチップを表示する。
 */
export default function HeatmapChartCard({ data }: Props) {
  const { currentFiscalYear, hasCurrentYearData, maxLevelValue, rows } = data;
  const [tooltip, setTooltip] = useState<TooltipState | null>(null);

  const handleMouseEnter = (e: React.MouseEvent<HTMLDivElement>, cell: HeatmapCell) => {
    setTooltip({ cell, x: e.clientX, y: e.clientY });
  };

  const handleMouseLeave = () => setTooltip(null);

  const handleMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    if (tooltip) {
      setTooltip(prev => prev ? { ...prev, x: e.clientX, y: e.clientY } : null);
    }
  };

  return (
    <div className="chart-card chart-card--full">
      <p className="chart-card__title">
        スキル分布ヒートマップ
        {currentFiscalYear && <span className="chart-card__fy">（{currentFiscalYear}）</span>}
      </p>

      {!hasCurrentYearData ? (
        <div className="chart-no-data">今年度の採点データがありません</div>
      ) : (
        <div className="heatmap">
          {rows.map(row => (
            <div key={row.category1Id} className="heatmap-row">
              <div className="heatmap-row__label">{row.category1Name}</div>
              <div className="heatmap-row__cells">
                {row.cells.map((cell, ci) => (
                  <div
                    key={cell.category2Id ?? `null-${ci}`}
                    className="heatmap-cell"
                    style={{
                      background: cellColor(cell.avgLevelValue, maxLevelValue),
                      color: cellTextColor(cell.avgLevelValue, maxLevelValue),
                    }}
                    onMouseEnter={e => handleMouseEnter(e, cell)}
                    onMouseLeave={handleMouseLeave}
                    onMouseMove={handleMouseMove}
                  >
                    <span className="heatmap-cell__name">{cell.category2Name}</span>
                    <span className="heatmap-cell__score">
                      {cell.avgLevelValue !== null ? cell.avgLevelValue.toFixed(1) : '—'}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          ))}

          {/* Legend */}
          <div className="heatmap-legend">
            <span className="heatmap-legend__label">低</span>
            <div className="heatmap-legend__bar" />
            <span className="heatmap-legend__label">高</span>
          </div>
        </div>
      )}

      {tooltip && createPortal(
        <div
          className="heatmap-tooltip"
          style={{ left: tooltip.x + 12, top: tooltip.y + 12 }}
        >
          <p className="heatmap-tooltip__title">{tooltip.cell.category2Name}</p>
          <p className="heatmap-tooltip__avg">
            平均: {tooltip.cell.avgLevelValue !== null ? tooltip.cell.avgLevelValue.toFixed(1) : '—'}
            <span className="heatmap-tooltip__count">（採点 {tooltip.cell.scoredSkillCount} 件）</span>
          </p>
          <ul className="heatmap-tooltip__skills">
            {tooltip.cell.skills.map((s: HeatmapSkill, i: number) => (
              <li key={i} className="heatmap-tooltip__skill">
                <span className="heatmap-tooltip__skill-name">{s.skillName}</span>
                <span className={`heatmap-tooltip__skill-lv${s.levelValue === null ? ' heatmap-tooltip__skill-lv--none' : ''}`}>
                  {s.levelValue !== null ? String(s.levelValue) : '—'}
                </span>
              </li>
            ))}
          </ul>
        </div>,
        document.body
      )}
    </div>
  );
}
