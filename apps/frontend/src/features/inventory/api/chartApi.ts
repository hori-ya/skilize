import apiClient from '../../../shared/api/client';
import type { RadarResponse, GrowthResponse, HeatmapResponse, TimelineResponse } from '../types/charts';

export const getRadarChart = () => apiClient.get<RadarResponse>('/charts/radar');
export const getGrowthChart = () => apiClient.get<GrowthResponse>('/charts/growth');
export const getHeatmapChart = () => apiClient.get<HeatmapResponse>('/charts/heatmap');
export const getTimelineChart = () => apiClient.get<TimelineResponse>('/charts/timeline');
