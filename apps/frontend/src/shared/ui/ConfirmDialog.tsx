/*******************************************************************************
 * 機能ID      ：SHR
 * 機能名      ：共通
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * 確認ダイアログコンポーネント。削除・提出などの操作前に確認を求めるモーダルを表示する。
 * variant により primary（通常確認）と danger（破壊的操作確認）を切り替えられる。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import { IconX, IconCheck } from './Icons';

interface ConfirmDialogProps {
  title?: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  variant?: 'primary' | 'danger';
  onConfirm: () => void;
  onCancel: () => void;
}

/**
 * 確認ダイアログコンポーネント。
 *
 * タイトル・メッセージ・ボタンラベルをカスタマイズできる。
 * variant='danger' を指定すると確認ボタンが赤色になる。
 */
export default function ConfirmDialog({
  title = '確認',
  message,
  confirmLabel = '確認',
  cancelLabel = 'キャンセル',
  variant = 'primary',
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  let confirmButtonClassName = 'btn btn--primary';
  if (variant === 'danger') {
    confirmButtonClassName = 'btn btn--danger';
  }

  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal" style={{ width: 400 }} onClick={e => e.stopPropagation()}>
        <div className="modal__header">
          <h3>{title}</h3>
          <button className="modal__close" onClick={onCancel}>×</button>
        </div>
        <div className="modal__body">
          <p style={{ margin: 0, lineHeight: 1.6 }}>{message}</p>
        </div>
        <div className="modal__footer">
          <button className="btn btn--secondary" onClick={onCancel}>
            <IconX size={13} />{cancelLabel}
          </button>
          <button
            className={confirmButtonClassName}
            onClick={onConfirm}
          >
            <IconCheck size={13} />{confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
