import { useState, type FormEvent } from 'react';
import NavBar from '../../../app/layouts/NavBar';
import { useAuth } from '../../../app/providers/AuthProvider';
import { IconLock } from '../../../shared/ui/Icons';
import axios from 'axios';

export default function MyPasswordPage() {
  const { changePassword } = useAuth();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [errors, setErrors] = useState<{ currentPassword?: string; newPassword?: string; confirmPassword?: string }>({});
  const [formError, setFormError] = useState('');
  const [success, setSuccess] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const validate = () => {
    const errs: typeof errors = {};
    if (!currentPassword) errs.currentPassword = '現在のパスワードを入力してください';
    if (newPassword.length < 8) errs.newPassword = '8文字以上で入力してください';
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
    setSuccess(false);
    setIsSubmitting(true);
    try {
      await changePassword(currentPassword, newPassword);
      setSuccess(true);
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      if (axios.isAxiosError(err)) {
        const code = err.response?.data?.code;
        if (code === 'AUTH_FAILED') {
          setFormError('現在のパスワードが正しくありません');
        } else {
          setFormError('パスワードの変更に失敗しました。もう一度お試しください。');
        }
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="master-page">
      <NavBar />
      <div className="my-password-page__body">
        <div className="auth-card">
          <p className="my-password-page__title">パスワード変更</p>
          {success && (
            <div className="alert alert-success">
              <span className="alert-icon">✓</span>
              パスワードを変更しました
            </div>
          )}
          {formError && (
            <div className="alert alert-error">
              <span className="alert-icon">⚠</span>
              {formError}
            </div>
          )}
          <form onSubmit={handleSubmit} noValidate>
            <div className="form-group">
              <label htmlFor="currentPassword" className="form-label">現在のパスワード</label>
              <input
                id="currentPassword"
                type="password"
                className={`form-input${errors.currentPassword ? ' form-input-error' : ''}`}
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                autoComplete="current-password"
                autoFocus
                disabled={isSubmitting}
              />
              {errors.currentPassword && <p className="form-error-text">※ {errors.currentPassword}</p>}
            </div>
            <div className="form-group">
              <label htmlFor="newPassword" className="form-label">新しいパスワード</label>
              <input
                id="newPassword"
                type="password"
                className={`form-input${errors.newPassword ? ' form-input-error' : ''}`}
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                autoComplete="new-password"
                disabled={isSubmitting}
              />
              {errors.newPassword && <p className="form-error-text">※ {errors.newPassword}</p>}
            </div>
            <div className="form-group">
              <label htmlFor="confirmPassword" className="form-label">新しいパスワード（確認）</label>
              <input
                id="confirmPassword"
                type="password"
                className={`form-input${errors.confirmPassword ? ' form-input-error' : ''}`}
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
      </div>
    </div>
  );
}
