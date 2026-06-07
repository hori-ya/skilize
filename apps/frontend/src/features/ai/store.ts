import type { AiMode, ChatMessage } from './types';

export interface AiSupportState {
  open: boolean;
  mode: AiMode;
  history: ChatMessage[];
}

// モジュールレベルのシングルトン。NavBar は画面遷移のたびに再マウントされるが、
// モジュールスコープの変数は SPA のライフタイムを通じて保持される。
const state: AiSupportState = {
  open: false,
  mode: 'NORMAL',
  history: [],
};

export function getAiSupportState(): AiSupportState {
  return { ...state, history: [...state.history] };
}

export function setAiSupportState(partial: Partial<AiSupportState>): void {
  Object.assign(state, partial);
}
