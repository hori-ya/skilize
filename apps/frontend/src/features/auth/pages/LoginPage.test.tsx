import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import type { AxiosError } from 'axios';
import LoginPage from './LoginPage';
import { useAuth } from '../../../app/providers/AuthProvider';

// ─── モック定義 ───────────────────────────────────────────────

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('../../../app/providers/AuthProvider', () => ({
  useAuth: vi.fn(),
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

vi.mock('../../../shared/ui/SkilizeLogo', () => ({ default: () => null }));
vi.mock('../../../shared/ui/Icons', () => ({ IconLogin: () => null }));

// ─── テストヘルパー ───────────────────────────────────────────

const mockLogin = vi.fn();
const mockUseAuth = vi.mocked(useAuth);

function renderLoginPage() {
  return render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>
  );
}

function makeAxiosError(code: string, status: number): AxiosError {
  return {
    isAxiosError: true,
    response: { data: { code }, status, headers: {}, config: {} as never, statusText: '' },
    message: 'Request failed',
    name: 'AxiosError',
    config: {} as never,
    toJSON: () => ({}),
  } as unknown as AxiosError;
}

// ─── テストスイート ───────────────────────────────────────────

describe('LoginPage', () => {

  beforeEach(() => {
    mockNavigate.mockClear();
    mockLogin.mockClear();
    // デフォルト: 未ログイン・ローディング完了状態
    mockUseAuth.mockReturnValue({
      user: null,
      isLoading: false,
      login: mockLogin,
      logout: vi.fn(),
      changePassword: vi.fn(),
    });
  });

  // ─── 表示 ────────────────────────────────────────────────

  it('正常系_未ログイン時_ログインフォームが表示される', () => {
    renderLoginPage();
    expect(screen.getByLabelText('loginForm.userIdLabel')).toBeInTheDocument();
    expect(screen.getByLabelText('loginForm.passwordLabel')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'loginForm.submitButton' })).toBeInTheDocument();
  });

  it('正常系_isLoading中_フォームを表示しない', () => {
    mockUseAuth.mockReturnValue({
      user: null,
      isLoading: true,
      login: mockLogin,
      logout: vi.fn(),
      changePassword: vi.fn(),
    });
    const { container } = renderLoginPage();
    expect(container).toBeEmptyDOMElement();
  });

  it('正常系_既ログイン済み非初回パスワード_ダッシュボードにリダイレクトする', () => {
    mockUseAuth.mockReturnValue({
      user: { isInitialPassword: false } as never,
      isLoading: false,
      login: mockLogin,
      logout: vi.fn(),
      changePassword: vi.fn(),
    });
    renderLoginPage();
    // Navigate コンポーネントが to="/" でレンダリングされていること
    expect(screen.queryByRole('button', { name: 'loginForm.submitButton' })).not.toBeInTheDocument();
  });

  it('正常系_既ログイン済み初回パスワード_パスワード変更画面にリダイレクトする', () => {
    mockUseAuth.mockReturnValue({
      user: { isInitialPassword: true } as never,
      isLoading: false,
      login: mockLogin,
      logout: vi.fn(),
      changePassword: vi.fn(),
    });
    renderLoginPage();
    expect(screen.queryByRole('button', { name: 'loginForm.submitButton' })).not.toBeInTheDocument();
  });

  // ─── ログイン正常系 ──────────────────────────────────────

  it('正常系_ログイン成功_通常ユーザー_ダッシュボードに遷移する', async () => {
    mockLogin.mockResolvedValue({ isInitialPassword: false });

    renderLoginPage();
    await userEvent.type(screen.getByLabelText('loginForm.userIdLabel'), 'user01');
    await userEvent.type(screen.getByLabelText('loginForm.passwordLabel'), 'password');
    await userEvent.click(screen.getByRole('button', { name: 'loginForm.submitButton' }));

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/'));
    expect(mockLogin).toHaveBeenCalledWith('user01', 'password');
  });

  it('正常系_ログイン成功_初回パスワード変更ユーザー_パスワード変更画面に遷移する', async () => {
    mockLogin.mockResolvedValue({ isInitialPassword: true });

    renderLoginPage();
    await userEvent.type(screen.getByLabelText('loginForm.userIdLabel'), 'user01');
    await userEvent.type(screen.getByLabelText('loginForm.passwordLabel'), 'pass');
    await userEvent.click(screen.getByRole('button', { name: 'loginForm.submitButton' }));

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith('/change-password'));
  });

  // ─── ログイン異常系 ──────────────────────────────────────

  it('異常系_認証失敗_invalidCredentialsエラーメッセージが表示される', async () => {
    mockLogin.mockRejectedValue(makeAxiosError('AUTH_FAILED', 401));

    renderLoginPage();
    await userEvent.type(screen.getByLabelText('loginForm.userIdLabel'), 'user01');
    await userEvent.type(screen.getByLabelText('loginForm.passwordLabel'), 'wrong');
    await userEvent.click(screen.getByRole('button', { name: 'loginForm.submitButton' }));

    await waitFor(() =>
      expect(screen.getByText('error.invalidCredentials')).toBeInTheDocument()
    );
  });

  it('異常系_アカウント無効_accountDisabledエラーメッセージが表示される', async () => {
    mockLogin.mockRejectedValue(makeAxiosError('ACCOUNT_DISABLED', 403));

    renderLoginPage();
    await userEvent.type(screen.getByLabelText('loginForm.userIdLabel'), 'user01');
    await userEvent.type(screen.getByLabelText('loginForm.passwordLabel'), 'pass');
    await userEvent.click(screen.getByRole('button', { name: 'loginForm.submitButton' }));

    await waitFor(() =>
      expect(screen.getByText('error.accountDisabled')).toBeInTheDocument()
    );
  });

  it('異常系_ネットワークエラー_networkErrorエラーメッセージが表示される', async () => {
    mockLogin.mockRejectedValue(new Error('Network Error'));

    renderLoginPage();
    await userEvent.type(screen.getByLabelText('loginForm.userIdLabel'), 'user01');
    await userEvent.type(screen.getByLabelText('loginForm.passwordLabel'), 'pass');
    await userEvent.click(screen.getByRole('button', { name: 'loginForm.submitButton' }));

    await waitFor(() =>
      expect(screen.getByText('error.networkError')).toBeInTheDocument()
    );
  });

  it('異常系_送信中_ボタンが無効化される', async () => {
    // ログイン処理を遅延させてローディング状態を確認する
    let resolveFn!: (val: unknown) => void;
    mockLogin.mockReturnValue(new Promise(resolve => { resolveFn = resolve; }));

    renderLoginPage();
    await userEvent.type(screen.getByLabelText('loginForm.userIdLabel'), 'user01');
    await userEvent.type(screen.getByLabelText('loginForm.passwordLabel'), 'pass');
    await userEvent.click(screen.getByRole('button', { name: 'loginForm.submitButton' }));

    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'loginForm.submittingButton' })).toBeDisabled()
    );

    resolveFn({ isInitialPassword: false });
    await waitFor(() =>
      expect(screen.queryByRole('button', { name: 'loginForm.submittingButton' })).not.toBeInTheDocument()
    );
  });
});
