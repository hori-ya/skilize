import { useState, type FormEvent } from 'react';
import { useNavigate, Navigate } from 'react-router-dom';
import { useAuth } from '../../../app/providers/AuthProvider';
import SkilizeLogo from '../../../shared/ui/SkilizeLogo';
import { IconLogin } from '../../../shared/ui/Icons';
import { useTranslation } from 'react-i18next';
import axios from 'axios';

export default function LoginPage() {
  const { user, isLoading, login } = useAuth();
  const navigate = useNavigate();
  const { t } = useTranslation('auth');
  const [userId, setUserId] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (isLoading) return null;
  if (user && !user.isInitialPassword) return <Navigate to="/" replace />;
  if (user && user.isInitialPassword) return <Navigate to="/change-password" replace />;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setIsSubmitting(true);
    try {
      const loggedInUser = await login(userId, password);
      if (loggedInUser.isInitialPassword) {
        navigate('/change-password');
      } else {
        navigate('/');
      }
    } catch (err) {
      if (axios.isAxiosError(err)) {
        const code = err.response?.data?.code;
        if (code === 'FORBIDDEN') {
          setError(t('error.accountDisabled'));
        } else {
          setError(t('error.invalidCredentials'));
        }
      } else {
        setError(t('error.networkError'));
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-logo">
          <div className="auth-brand">
            <SkilizeLogo size={30} />
            <h1 className="auth-title">Skilize</h1>
          </div>
          <p className="auth-subtitle">{t('appSubtitle')}</p>
        </div>
        <form onSubmit={handleSubmit} noValidate>
          {error && (
            <div className="alert alert-error">
              <span className="alert-icon">⚠</span>
              {error}
            </div>
          )}
          <div className="form-group">
            <label htmlFor="userId" className="form-label">{t('loginForm.userIdLabel')}</label>
            <input
              id="userId"
              type="text"
              className="form-input"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              autoComplete="username"
              autoFocus
              disabled={isSubmitting}
            />
          </div>
          <div className="form-group">
            <label htmlFor="password" className="form-label">{t('loginForm.passwordLabel')}</label>
            <input
              id="password"
              type="password"
              className="form-input"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              disabled={isSubmitting}
            />
          </div>
          <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
            <IconLogin size={15} />
            {isSubmitting ? t('loginForm.submittingButton') : t('loginForm.submitButton')}
          </button>
        </form>
      </div>
    </div>
  );
}
