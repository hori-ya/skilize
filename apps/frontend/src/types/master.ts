export interface FiscalYear {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  inputStartDate: string | null;
  inputEndDate: string | null;
  isActive: boolean;
}

export interface SkillLevel {
  id: number;
  levelValue: number;
  description: string;
  isActive: boolean;
}

export interface ItSkillCategory {
  id: number;
  parentId: number | null;
  level: number;
  name: string;
  sortOrder: number;
  isActive: boolean;
}

export interface ItSkill {
  id: number;
  name: string;
  categoryId: number;
  category1Id: number | null;
  category1Name: string | null;
  category2Name: string | null;
  category3Name: string | null;
  description: string | null;
  sortOrder: number;
  isActive: boolean;
}

export interface Qualification {
  id: number;
  name: string;
  categoryId: number | null;
  categoryName: string | null;
  description: string | null;
  sortOrder: number;
  isActive: boolean;
}

export interface AdSeminar {
  id: number;
  name: string;
  categoryId: number | null;
  categoryName: string | null;
  description: string | null;
  sortOrder: number;
  isActive: boolean;
}

export interface QualificationCategory {
  id: number;
  name: string;
  sortOrder: number;
  isActive: boolean;
}

export interface AdSeminarCategory {
  id: number;
  name: string;
  sortOrder: number;
  isActive: boolean;
}

export interface SeminarCategory {
  id: number;
  name: string;
  sortOrder: number;
  isActive: boolean;
}

export interface FiscalYearSettings {
  fiscalYearStartMonth: number;
}

export interface FiscalYearRequest {
  name: string;
  startDate: string;
  endDate: string;
  inputStartDate: string | null;
  inputEndDate: string | null;
  active?: boolean;
}
