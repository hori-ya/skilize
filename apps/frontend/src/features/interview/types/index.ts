/** 面談ノートの対象種別（ITスキル明細・資格明細・セミナー明細・目標） */
export type DetailType = 'IT_SKILL' | 'QUALIFICATION' | 'SEMINAR' | 'GOAL';

export interface DetailNoteItem {
  id: number;
  detailType: DetailType;
  detailId: number;
  note: string;
}

/**
 * 面談メモ。interviewerId は面談実施者（TL/ADMIN）の ID。
 * generalNote は全体備忘録、detailNotes は各明細ごとのメモ。
 */
export interface InterviewMemo {
  id: number;
  inventoryId: number;
  interviewerId: number;
  interviewerName: string;
  generalNote: string | null;
  detailNotes: DetailNoteItem[];
}
