/*******************************************************************************
 * 機能ID      ：SHR
 * 機能名      ：共通
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * マスタデータの API 関数群。ITスキル・資格・セミナー・会計年度などのマスタを取得・更新する。
 * 複数 feature から共有するため shared/api に配置する。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
/**
 * マスタデータ API。複数 feature（inventory/master）から共有するため shared/api に配置する。
 * isActive パラメーターを省略した場合はすべて（有効・無効含む）を取得する。
 */
import apiClient from './client';
import type { FiscalYear, FiscalYearSettings, FiscalYearRequest, SkillLevel, ItSkill, ItSkillCategory, Qualification, QualificationCategory, AdSeminar, AdSeminarCategory, SeminarCategory, CustomUnregisteredItem, MasterImportResult } from '../types/master';

/** isActive パラメーターが指定されている場合のみ params に含める（省略時は全件取得）。 */
function activeParam(isActive?: boolean): { isActive?: boolean } {
  if (isActive !== undefined) {
    return { isActive };
  }
  return {};
}

export const getFiscalYears = () => apiClient.get<FiscalYear[]>('/fiscal-years');
export const getCurrentFiscalYear = () => apiClient.get<FiscalYear>('/fiscal-years/current');
export const getSkillLevels = (isActive?: boolean) =>
  apiClient.get<SkillLevel[]>('/skill-levels', { params: activeParam(isActive) });
export const createSkillLevel = (data: { levelValue: number; description: string; scoreWeight: number }) =>
  apiClient.post<SkillLevel>('/skill-levels', data);
export const updateSkillLevel = (id: number, data: { levelValue: number; description: string; active: boolean; scoreWeight: number }) =>
  apiClient.put<SkillLevel>(`/skill-levels/${id}`, data);
export const getItSkills = (isActive?: boolean) =>
  apiClient.get<ItSkill[]>('/it-skills', { params: activeParam(isActive) });
export const createItSkill = (data: { categoryId: number; name: string; description: string | null; sortOrder: number }) =>
  apiClient.post<ItSkill>('/it-skills', data);
export const updateItSkill = (id: number, data: { categoryId: number; name: string; description: string | null; sortOrder: number; active: boolean }) =>
  apiClient.put<ItSkill>(`/it-skills/${id}`, data);
export const getItSkillCategories = (isActive?: boolean) =>
  apiClient.get<ItSkillCategory[]>('/it-skill-categories', { params: activeParam(isActive) });
export const createItSkillCategory = (data: { parentId: number | null; name: string; sortOrder: number }) =>
  apiClient.post<ItSkillCategory>('/it-skill-categories', data);
export const updateItSkillCategory = (id: number, data: { name: string; sortOrder: number; active: boolean }) =>
  apiClient.put<ItSkillCategory>(`/it-skill-categories/${id}`, data);
export const getQualifications = (isActive?: boolean) =>
  apiClient.get<Qualification[]>('/qualifications', { params: activeParam(isActive) });
export const createQualification = (data: { categoryId: number | null; name: string; description: string | null; sortOrder: number }) =>
  apiClient.post<Qualification>('/qualifications', data);
export const updateQualification = (id: number, data: { categoryId: number | null; name: string; description: string | null; sortOrder: number; active: boolean }) =>
  apiClient.put<Qualification>(`/qualifications/${id}`, data);
export const getQualificationCategories = (isActive?: boolean) =>
  apiClient.get<QualificationCategory[]>('/qualification-categories', { params: activeParam(isActive) });
export const createQualificationCategory = (data: { name: string; sortOrder: number }) =>
  apiClient.post<QualificationCategory>('/qualification-categories', data);
export const updateQualificationCategory = (id: number, data: { name: string; sortOrder: number; active: boolean }) =>
  apiClient.put<QualificationCategory>(`/qualification-categories/${id}`, data);
export const getAdSeminars = (isActive?: boolean) =>
  apiClient.get<AdSeminar[]>('/ad-seminars', { params: activeParam(isActive) });
export const createAdSeminar = (data: { categoryId: number | null; name: string; description: string | null; sortOrder: number }) =>
  apiClient.post<AdSeminar>('/ad-seminars', data);
export const updateAdSeminar = (id: number, data: { categoryId: number | null; name: string; description: string | null; sortOrder: number; active: boolean }) =>
  apiClient.put<AdSeminar>(`/ad-seminars/${id}`, data);
export const getAdSeminarCategories = (isActive?: boolean) =>
  apiClient.get<AdSeminarCategory[]>('/ad-seminar-categories', { params: activeParam(isActive) });
export const createAdSeminarCategory = (data: { name: string; sortOrder: number }) =>
  apiClient.post<AdSeminarCategory>('/ad-seminar-categories', data);
export const updateAdSeminarCategory = (id: number, data: { name: string; sortOrder: number; active: boolean }) =>
  apiClient.put<AdSeminarCategory>(`/ad-seminar-categories/${id}`, data);
export const getSeminarCategories = () => apiClient.get<SeminarCategory[]>('/seminar-categories');
export const getCustomUnregisteredItSkills = () =>
  apiClient.get<CustomUnregisteredItem[]>('/it-skills/custom-unregistered');
export const promoteItSkill = (data: { customName: string; categoryId: number; name: string; description: string | null; sortOrder: number }) =>
  apiClient.post<ItSkill>('/it-skills/promote', data);
export const getCustomUnregisteredQualifications = () =>
  apiClient.get<CustomUnregisteredItem[]>('/qualifications/custom-unregistered');
export const promoteQualification = (data: { customName: string; categoryId: number | null; name: string; description: string | null; sortOrder: number }) =>
  apiClient.post<Qualification>('/qualifications/promote', data);

// Excel 出力（Blob レスポンス）
export const downloadItSkillExcel = () =>
  apiClient.get<Blob>('/master-excel/it-skills/download', { responseType: 'blob' });
export const downloadQualificationExcel = () =>
  apiClient.get<Blob>('/master-excel/qualifications/download', { responseType: 'blob' });
export const downloadAdSeminarExcel = () =>
  apiClient.get<Blob>('/master-excel/ad-seminars/download', { responseType: 'blob' });

// Excel 取込（multipart/form-data）
const buildFormData = (file: File) => { const fd = new FormData(); fd.append('file', file); return fd; };
export const uploadItSkillExcel = (file: File) =>
  apiClient.post<MasterImportResult>('/master-excel/it-skills/upload', buildFormData(file));
export const uploadQualificationExcel = (file: File) =>
  apiClient.post<MasterImportResult>('/master-excel/qualifications/upload', buildFormData(file));
export const uploadAdSeminarExcel = (file: File) =>
  apiClient.post<MasterImportResult>('/master-excel/ad-seminars/upload', buildFormData(file));

export const getFiscalYearSettings = () => apiClient.get<FiscalYearSettings>('/fiscal-year-settings');
export const updateFiscalYearSettings = (data: { fiscalYearStartMonth: number }) =>
  apiClient.put<FiscalYearSettings>('/fiscal-year-settings', data);
export const createFiscalYear = (data: FiscalYearRequest) =>
  apiClient.post<FiscalYear>('/fiscal-years', data);
export const updateFiscalYear = (id: number, data: FiscalYearRequest) =>
  apiClient.put<FiscalYear>(`/fiscal-years/${id}`, data);
