/**
 * グラフデータ API。認証済みユーザー自身の棚卸データを集計したグラフ情報を取得する。
 * レーダー・成長推移・ヒートマップ・タイムラインの4種類。集計はバックエンドが行う。
 */
import apiClient from '../../../shared/api/client';
import type { RadarResponse, GrowthResponse, HeatmapResponse, TimelineResponse } from '../types/index';

/** ITスキル大分類ごとの平均スキルレベルをレーダーチャート形式で取得する。 */
export const getRadarChart = () => apiClient.get<RadarResponse>('/charts/radar');
/** 年度別スキルレベル合計の推移を折れ線グラフ形式で取得する。 */
export const getGrowthChart = () => apiClient.get<GrowthResponse>('/charts/growth');
/** 年度×スキルレベルの分布をヒートマップ形式で取得する。 */
export const getHeatmapChart = () => apiClient.get<HeatmapResponse>('/charts/heatmap');
/** 資格取得・セミナー受講の時系列データをタイムライン形式で取得する。 */
export const getTimelineChart = () => apiClient.get<TimelineResponse>('/charts/timeline');
