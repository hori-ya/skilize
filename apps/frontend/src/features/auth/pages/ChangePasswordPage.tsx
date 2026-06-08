/*******************************************************************************
 * 機能ID      ：AUTH
 * 機能名      ：認証機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * 初回ログイン時のパスワード変更ページ。初期パスワードのまま通常ページへアクセスした際に
 * リダイレクトされる。変更完了後はダッシュボードへ遷移する。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../app/providers/AuthProvider';
import { IconLock } from '../../../shared/ui/Icons';
import { useTranslation } from 'react-i18next';
import axios from 'axios';

/**
 * 初回ログイン時のパスワード変更ページ。
 *
 * 新しいパスワードと確認用パスワードを入力してパスワードを変更する。
 * 変更完了後はダッシュボードへ遷移する。
 */
export default function ChangePasswordPage() {
  const { user, changePassword } = useAuth();
  const navigate = useNavigate();
  const { t } = useTranslation('auth');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [errors, setErrors] = useState<{ newPassword?: string; confirmPassword?: string }>({});
  const [formError, setFormError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const validate = () => {
    const errs: typeof errors = {};
    if (newPassword.length < 8) errs.newPassword = t('validation.minLength');
    if (newPassword !== confirmPassword) errs.confirmPassword = t('validation.passwordMismatch');
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
        setFormError(t('error.changeFailed'));
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-logo">
          <h1 className="auth-title">{t('changePasswordForm.title')}</h1>
          <p className="auth-subtitle">{t('appSubtitle')}</p>
        </div>
        <form onSubmit={handleSubmit} noValidate>
          <div className="alert alert-info">
            <span className="alert-icon">ⓘ</span>
            {t('changePasswordForm.infoMessage')}
          </div>
          {formError && (
            <div className="alert alert-error">
              <span className="alert-icon">⚠</span>
              {formError}
            </div>
          )}
          <div className="form-group">
            <label htmlFor="newPassword" className="form-label">{t('changePasswordForm.newPasswordLabel')}</label>
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
            <label htmlFor="confirmPassword" className="form-label">{t('changePasswordForm.confirmPasswordLabel')}</label>
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
          <p className="form-hint">{t('changePasswordForm.minLengthHint')}</p>
          <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
            <IconLock size={15} />
            {isSubmitting ? t('changePasswordForm.submittingButton') : t('changePasswordForm.submitButton')}
          </button>
        </form>
      </div>
      <p className="auth-footer-note">{t('changePasswordForm.footerNote')}</p>
    </div>
  );
}
