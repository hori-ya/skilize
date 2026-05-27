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
