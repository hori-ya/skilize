/*******************************************************************************
 * 機能ID      ：AUTH
 * 機能名      ：認証機能
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * ログイン済みユーザー向けのパスワード変更ページ。
 * 現在のパスワードと新しいパスワードを入力してパスワードを変更する。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import { useState, type FormEvent } from 'react';
import NavBar from '../../../app/layouts/NavBar';
import { useAuth } from '../../../app/providers/AuthProvider';
import { IconLock } from '../../../shared/ui/Icons';
import { useTranslation } from 'react-i18next';
import axios from 'axios';

/**
 * ログイン済みユーザー向けのパスワード変更ページ。
 *
 * 現在のパスワードと新しいパスワードを入力してパスワードを変更する。
 * 変更成功時はページ内に成功メッセージを表示する。
 */
export default function MyPasswordPage() {
  const { changePassword } = useAuth();
  const { t } = useTranslation('auth');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [errors, setErrors] = useState<{ currentPassword?: string; newPassword?: string; confirmPassword?: string }>({});
  const [formError, setFormError] = useState('');
  const [success, setSuccess] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const validate = () => {
    const errs: typeof errors = {};
    if (!currentPassword) errs.currentPassword = t('validation.currentPasswordRequired');
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
          setFormError(t('error.currentPasswordWrong'));
        } else {
          setFormError(t('error.changeFailed'));
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
          <p className="my-password-page__title">{t('myPasswordPage.title')}</p>
          {success && (
            <div className="alert alert-success">
              <span className="alert-icon">✓</span>
              {t('myPasswordPage.successMessage')}
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
              <label htmlFor="currentPassword" className="form-label">{t('myPasswordPage.currentPasswordLabel')}</label>
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
              <label htmlFor="newPassword" className="form-label">{t('myPasswordPage.newPasswordLabel')}</label>
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
              <label htmlFor="confirmPassword" className="form-label">{t('myPasswordPage.confirmPasswordLabel')}</label>
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
            <p className="form-hint">{t('myPasswordPage.minLengthHint')}</p>
            <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
              <IconLock size={15} />
              {isSubmitting ? t('myPasswordPage.submittingButton') : t('myPasswordPage.submitButton')}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
