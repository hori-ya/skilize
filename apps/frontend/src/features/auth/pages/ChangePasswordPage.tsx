import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../app/providers/AuthProvider';
import { IconLock } from '../../../shared/ui/Icons';
import axios from 'axios';

export default function ChangePasswordPage() {
  const { user, changePassword } = useAuth();
  const navigate = useNavigate();
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [errors, setErrors] = useState<{ newPassword?: string; confirmPassword?: string }>({});
  const [formError, setFormError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const validate = () => {
    const errs: typeof errors = {};
    if (newPassword.length < 8) errs.newPassword = '8 文字以上で入力してください';
    if (newPassword !== confirmPassword) errs.confirmPassword = 'パスワードが一致しません';
    return errs;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length > 0) {
      setErrors(errs);
      return;
    }
    setErrors({});
    setFormError('');
    setIsSubmitting(true);
    try {
      // 初期パスワード = ユーザーID（設計仕様）
      await changePassword(user!.userId, newPassword);
      navigate('/');
    } catch (err) {
      if (axios.isAxiosError(err)) {
        setFormError('パスワードの変更に失敗しました。もう一度お試しください。');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-logo">
          <h1 className="auth-title">Skilize</h1>
          <p className="auth-subtitle">スキル棚卸管理システム</p>
        </div>
        <form onSubmit={handleSubmit} noValidate>
          <div className="alert alert-info">
            <span className="alert-icon">ⓘ</span>
            初回ログインのため、パスワードを変更してください
          </div>
          {formError && (
            <div className="alert alert-error">
              <span className="alert-icon">⚠</span>
              {formError}
            </div>
          )}
          <div className="form-group">
            <label htmlFor="newPassword" className="form-label">新しいパスワード</label>
            <input
              id="newPassword"
              type="password"
              className={`form-input ${errors.newPassword ? 'form-input-error' : ''}`}
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              autoComplete="new-password"
              autoFocus
              disabled={isSubmitting}
            />
            {errors.newPassword && <p className="form-error-text">※ {errors.newPassword}</p>}
          </div>
          <div className="form-group">
            <label htmlFor="confirmPassword" className="form-label">新しいパスワード（確認）</label>
            <input
              id="confirmPassword"
              type="password"
              className={`form-input ${errors.confirmPassword ? 'form-input-error' : ''}`}
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              autoComplete="new-password"
              disabled={isSubmitting}
            />
            {errors.confirmPassword && <p className="form-error-text">※ {errors.confirmPassword}</p>}
          </div>
          <p className="form-hint">※ 8文字以上で入力してください</p>
          <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
            <IconLock size={15} />
            {isSubmitting ? '変更中...' : 'パスワードを変更する'}
          </button>
        </form>
      </div>
      <p className="auth-footer-note">※ パスワード変更が完了するまで、他の操作はできません</p>
    </div>
  );
}
