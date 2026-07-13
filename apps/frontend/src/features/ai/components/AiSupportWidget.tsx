/*******************************************************************************
 * 機能ID      ：AI
 * 機能名      ：AI機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * AIサポートウィジェット（フローティングパネル）。
 * NavBar に配置されるボタンから開閉するチャットパネルを提供する。
 * NORMAL / PROOFREADING / CAREER / HELP の4モードを切り替えて使用できる。
 * 会話履歴はモジュールストアで保持し、画面遷移後も復元される。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { postAiChat } from '../api/aiChatApi';
import { getAiSupportState, setAiSupportState } from '../store';
import type { AiMode, ChatMessage } from '../types';

const MODES: AiMode[] = ['NORMAL', 'PROOFREADING', 'CAREER', 'HELP'];

/**
 * AIサポートウィジェット。
 *
 * NavBar に配置されるフローティングチャットパネル。
 * モードを切り替えながらAIと会話でき、画面遷移後も会話履歴が保持される。
 */
export default function AiSupportWidget() {
  const { t } = useTranslation('ai');

  // 再マウント時はモジュールストアから状態を復元する
  const [open, setOpen] = useState(() => getAiSupportState().open);
  const [mode, setMode] = useState<AiMode>(() => getAiSupportState().mode);
  const [history, setHistory] = useState<ChatMessage[]>(() => getAiSupportState().history);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const historyEndRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // 状態変化をストアに同期して、次のマウント時に復元できるようにする
  useEffect(() => {
    setAiSupportState({ open, mode, history });
  }, [open, mode, history]);

  // 新しいメッセージが追加されたときのみスクロール（モード切替によるクリア時は除く）
  useEffect(() => {
    if (open && history.length > 0 && historyEndRef.current != null) {
      // block: 'nearest' でパネル内スクロールに限定し、ページ全体がスクロールするのを防ぐ
      historyEndRef.current.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
  }, [history, open]);

  // パネルが開いたときにテキストエリアにフォーカスを当てる
  useEffect(() => {
    if (open && textareaRef.current != null) {
      textareaRef.current.focus();
    }
  }, [open]);

  const handleModeChange = (newMode: AiMode) => {
    if (newMode === mode) return;
    setMode(newMode);
    setHistory([]);
    setError('');
  };

  const handleSubmit = async () => {
    const trimmed = input.trim();
    if (!trimmed || loading) return;

    const userMessage: ChatMessage = { role: 'user', content: trimmed };
    const nextHistory = [...history, userMessage];
    setHistory(nextHistory);
    setInput('');
    setLoading(true);
    setError('');

    try {
      const res = await postAiChat({ message: trimmed, mode, history });
      setHistory([...nextHistory, { role: 'assistant', content: res.response }]);
    } catch {
      setError(t('error.failed'));
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
      e.preventDefault();
      handleSubmit();
    }
  };

  const modeButtons: React.ReactNode[] = [];
  for (const m of MODES) {
    let modeButtonClassName = 'ai-panel__mode-btn';
    if (mode === m) {
      modeButtonClassName += ' ai-panel__mode-btn--active';
    }
    modeButtons.push(
      <button
        type="button"
        key={m}
        className={modeButtonClassName}
        onClick={() => handleModeChange(m)}
      >
        {t(`mode.${m.toLowerCase()}`)}
      </button>,
    );
  }

  const historyItems: React.ReactNode[] = [];
  for (let i = 0; i < history.length; i++) {
    const msg = history[i];
    let roleLabel = t('message.ai');
    if (msg.role === 'user') {
      roleLabel = t('message.you');
    }
    let messageContent: React.ReactNode;
    if (msg.role === 'assistant') {
      messageContent = (
        <div className="ai-panel__message-content ai-panel__message-content--md">
          <ReactMarkdown remarkPlugins={[remarkGfm]}>
            {msg.content}
          </ReactMarkdown>
        </div>
      );
    } else {
      messageContent = <p className="ai-panel__message-content">{msg.content}</p>;
    }
    historyItems.push(
      <div key={i} className={`ai-panel__message ai-panel__message--${msg.role}`}>
        <span className="ai-panel__message-label">
          {roleLabel}
        </span>
        {messageContent}
      </div>,
    );
  }

  let sendButtonLabel = t('input.send');
  if (loading) {
    sendButtonLabel = t('input.sending');
  }

  return (
    <>
      <button
        type="button"
        className="navbar__ai-btn"
        onClick={() => setOpen(v => !v)}
        aria-label={t('button.ariaLabel')}
        aria-expanded={open}
      >
        {t('button.label')}
      </button>

      {open && (
        <div className="ai-panel" role="dialog" aria-label={t('panel.title')}>
          <div className="ai-panel__header">
            <span className="ai-panel__title">{t('panel.title')}</span>
            <button
              type="button"
              className="ai-panel__close"
              onClick={() => setOpen(false)}
              aria-label={t('panel.close')}
            >
              ✕
            </button>
          </div>

          <div className="ai-panel__modes" role="group" aria-label={t('mode.label')}>
            {modeButtons}
          </div>

          <div className="ai-panel__history" aria-live="polite">
            {history.length === 0 && (
              <p className="ai-panel__empty">{t(`placeholder.${mode.toLowerCase()}`)}</p>
            )}
            {historyItems}
            {loading && (
              <div className="ai-panel__message ai-panel__message--assistant">
                <span className="ai-panel__message-label">{t('message.ai')}</span>
                <p className="ai-panel__typing">{t('message.thinking')}</p>
              </div>
            )}
            {error && <p className="ai-panel__error">{error}</p>}
            <div ref={historyEndRef} />
          </div>

          <div className="ai-panel__input-area">
            <textarea
              ref={textareaRef}
              className="ai-panel__textarea"
              value={input}
              onChange={e => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder={t('input.placeholder')}
              rows={3}
              disabled={loading}
              aria-label={t('input.ariaLabel')}
            />
            <div className="ai-panel__input-footer">
              <span className="ai-panel__shortcut">{t('input.shortcut')}</span>
              <button
                type="button"
                className="ai-panel__send-btn"
                onClick={handleSubmit}
                disabled={loading || !input.trim()}
                aria-label={t('input.send')}
              >
                {sendButtonLabel}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
