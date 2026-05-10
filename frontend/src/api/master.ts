import apiClient from './client';
import type { FiscalYear, FiscalYearSettings, FiscalYearRequest, SkillLevel, ItSkill, ItSkillCategory, Qualification, QualificationCategory, AdSeminar, AdSeminarCategory, SeminarCategory } from '../types/master';

export const getFiscalYears = () => apiClient.get<FiscalYear[]>('/fiscal-years');
export const getCurrentFiscalYear = () => apiClient.get<FiscalYear>('/fiscal-years/current');
export const getSkillLevels = (isActive?: boolean) =>
  apiClient.get<SkillLevel[]>('/skill-levels', { params: isActive !== undefined ? { isActive } : {} });
export const createSkillLevel = (data: { levelValue: number; description: string }) =>
  apiClient.post<SkillLevel>('/skill-levels', data);
export const updateSkillLevel = (id: number, data: { levelValue: number; description: string; active: boolean }) =>
  apiClient.put<SkillLevel>(`/skill-levels/${id}`, data);
export const getItSkills = (isActive?: boolean) =>
  apiClient.get<ItSkill[]>('/it-skills', { params: isActive !== undefined ? { isActive } : {} });
export const createItSkill = (data: { categoryId: number; name: string; description: string | null; sortOrder: number }) =>
  apiClient.post<ItSkill>('/it-skills', data);
export const updateItSkill = (id: number, data: { categoryId: number; name: string; description: string | null; sortOrder: number; active: boolean }) =>
  apiClient.put<ItSkill>(`/it-skills/${id}`, data);
export const getItSkillCategories = (isActive?: boolean) =>
  apiClient.get<ItSkillCategory[]>('/it-skill-categories', { params: isActive !== undefined ? { isActive } : {} });
export const createItSkillCategory = (data: { parentId: number | null; name: string; sortOrder: number }) =>
  apiClient.post<ItSkillCategory>('/it-skill-categories', data);
export const updateItSkillCategory = (id: number, data: { name: string; sortOrder: number; active: boolean }) =>
  apiClient.put<ItSkillCategory>(`/it-skill-categories/${id}`, data);
export const getQualifications = (isActive?: boolean) =>
  apiClient.get<Qualification[]>('/qualifications', { params: isActive !== undefined ? { isActive } : {} });
export const createQualification = (data: { categoryId: number | null; name: string; description: string | null; sortOrder: number }) =>
  apiClient.post<Qualification>('/qualifications', data);
export const updateQualification = (id: number, data: { categoryId: number | null; name: string; description: string | null; sortOrder: number; active: boolean }) =>
  apiClient.put<Qualification>(`/qualifications/${id}`, data);
export const getQualificationCategories = (isActive?: boolean) =>
  apiClient.get<QualificationCategory[]>('/qualification-categories', { params: isActive !== undefined ? { isActive } : {} });
export const createQualificationCategory = (data: { name: string; sortOrder: number }) =>
  apiClient.post<QualificationCategory>('/qualification-categories', data);
export const updateQualificationCategory = (id: number, data: { name: string; sortOrder: number; active: boolean }) =>
  apiClient.put<QualificationCategory>(`/qualification-categories/${id}`, data);
export const getAdSeminars = (isActive?: boolean) =>
  apiClient.get<AdSeminar[]>('/ad-seminars', { params: isActive !== undefined ? { isActive } : {} });
export const createAdSeminar = (data: { categoryId: number | null; name: string; description: string | null; sortOrder: number }) =>
  apiClient.post<AdSeminar>('/ad-seminars', data);
export const updateAdSeminar = (id: number, data: { categoryId: number | null; name: string; description: string | null; sortOrder: number; active: boolean }) =>
  apiClient.put<AdSeminar>(`/ad-seminars/${id}`, data);
export const getAdSeminarCategories = (isActive?: boolean) =>
  apiClient.get<AdSeminarCategory[]>('/ad-seminar-categories', { params: isActive !== undefined ? { isActive } : {} });
export const createAdSeminarCategory = (data: { name: string; sortOrder: number }) =>
  apiClient.post<AdSeminarCategory>('/ad-seminar-categories', data);
export const updateAdSeminarCategory = (id: number, data: { name: string; sortOrder: number; active: boolean }) =>
  apiClient.put<AdSeminarCategory>(`/ad-seminar-categories/${id}`, data);
export const getSeminarCategories = () => apiClient.get<SeminarCategory[]>('/seminar-categories');

export const getFiscalYearSettings = () => apiClient.get<FiscalYearSettings>('/fiscal-year-settings');
export const updateFiscalYearSettings = (data: { fiscalYearStartMonth: number }) =>
  apiClient.put<FiscalYearSettings>('/fiscal-year-settings', data);
export const createFiscalYear = (data: FiscalYearRequest) =>
  apiClient.post<FiscalYear>('/fiscal-years', data);
export const updateFiscalYear = (id: number, data: FiscalYearRequest) =>
  apiClient.put<FiscalYear>(`/fiscal-years/${id}`, data);
