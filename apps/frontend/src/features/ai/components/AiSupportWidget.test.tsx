import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import AiSupportWidget from './AiSupportWidget';
import * as aiChatApi from '../api/aiChatApi';
import { setAiSupportState } from '../store';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

vi.mock('../api/aiChatApi');

vi.mock('react-markdown', () => ({
  default: ({ children }: { children: string }) => <>{children}</>,
}));

vi.mock('remark-gfm', () => ({ default: () => {} }));

const mockPostAiChat = vi.mocked(aiChatApi.postAiChat);

describe('AiSupportWidget', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // モジュールレベルのシングルトンをリセットして各テストを独立させる
    setAiSupportState({ open: false, mode: 'NORMAL', history: [] });
  });

  it('ボタンクリックでパネルが開く', () => {
    render(<AiSupportWidget />);
    fireEvent.click(screen.getByRole('button', { name: 'button.ariaLabel' }));
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('メッセージ送信でチャット履歴に追加される', async () => {
    mockPostAiChat.mockResolvedValue({ response: 'AI の返答', mode: 'NORMAL' });
    render(<AiSupportWidget />);
    fireEvent.click(screen.getByRole('button', { name: 'button.ariaLabel' }));

    const textarea = screen.getByRole('textbox');
    fireEvent.change(textarea, { target: { value: 'テスト送信' } });
    fireEvent.click(screen.getByRole('button', { name: 'input.send' }));

    await waitFor(() => expect(screen.getByText('テスト送信')).toBeInTheDocument());
    await waitFor(() => expect(screen.getByText('AI の返答')).toBeInTheDocument());
  });

  it('API エラー時にエラーメッセージが表示される', async () => {
    mockPostAiChat.mockRejectedValue(new Error('Network Error'));
    render(<AiSupportWidget />);
    fireEvent.click(screen.getByRole('button', { name: 'button.ariaLabel' }));

    const textarea = screen.getByRole('textbox');
    fireEvent.change(textarea, { target: { value: 'エラーテスト' } });
    fireEvent.click(screen.getByRole('button', { name: 'input.send' }));

    await waitFor(() => expect(screen.getByText('error.failed')).toBeInTheDocument());
  });
});
