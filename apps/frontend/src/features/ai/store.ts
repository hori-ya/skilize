/*******************************************************************************
 * 機能ID      ：AI
 * 機能名      ：AI機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * AIサポートウィジェットのモジュールスコープ状態管理。
 * NavBar は画面遷移のたびに再マウントされるため、開閉状態・モード・
 * 会話履歴をモジュールレベルのシングルトンで保持し、再マウント時に復元する。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import type { AiMode, ChatMessage } from './types';

/**
 * AIサポートウィジェットの状態インターフェース。
 */
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

/**
 * AIサポートウィジェットの現在状態をコピーして返す。
 *
 * @returns 現在の状態のシャローコピー
 */
export function getAiSupportState(): AiSupportState {
  return { ...state, history: [...state.history] };
}

/**
 * AIサポートウィジェットの状態を部分更新する。
 *
 * @param partial 更新するフィールドのみを含む部分オブジェクト
 */
export function setAiSupportState(partial: Partial<AiSupportState>): void {
  Object.assign(state, partial);
}
