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

export default function ConfirmDialog({
  title = '確認',
  message,
  confirmLabel = '確認',
  cancelLabel = 'キャンセル',
  variant = 'primary',
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
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
            className={variant === 'danger' ? 'btn btn--danger' : 'btn btn--primary'}
            onClick={onConfirm}
          >
            <IconCheck size={13} />{confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
