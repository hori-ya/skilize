export type DetailType = 'IT_SKILL' | 'QUALIFICATION' | 'SEMINAR' | 'GOAL';

export interface DetailNoteItem {
  id: number;
  detailType: DetailType;
  detailId: number;
  note: string;
}

export interface InterviewMemo {
  id: number;
  inventoryId: number;
  interviewerId: number;
  interviewerName: string;
  generalNote: string | null;
  detailNotes: DetailNoteItem[];
}
