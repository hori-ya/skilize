export type AiMode = 'NORMAL' | 'PROOFREADING' | 'CAREER' | 'HELP';

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

export interface AiChatRequest {
  message: string;
  mode: AiMode;
  history: ChatMessage[];
}

export interface AiChatResponse {
  response: string;
  mode: AiMode;
}

/** PENDING=待機中 / PROCESSING=分析中 / COMPLETED=完了 / FAILED=失敗 */
export type AiAnalysisStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface AiAnalysisResult {
  summary: string;
  strengths: string[];
  growth_areas: string[];
  expectation_fit: string;
  recommended_actions: string[];
}

export interface AiAnalysis {
  id: number;
  fiscalYearId: number;
  status: AiAnalysisStatus;
  analysisResult: AiAnalysisResult | null;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
}
