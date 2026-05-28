import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AiSupportWidget from './AiSupportWidget';
import { postAiChat } from '../api/aiSupportApi';
import { setAiSupportState } from '../store';

// ─── モック定義 ───────────────────────────────────────────────

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}));

vi.mock('../api/aiSupportApi', () => ({
  postAiChat: vi.fn(),
}));

// ストアはテスト間で状態が漏れないようにリセットする
vi.mock('../store', () => {
  let state = { open: false, mode: 'NORMAL' as const, history: [] as never[] };
  return {
    getAiSupportState: () => ({ ...state, history: [...state.history] }),
    setAiSupportState: (partial: Partial<typeof state>) => { Object.assign(state, partial); },
  };
});

const mockPostAiChat = vi.mocked(postAiChat);

// ─── テストヘルパー ───────────────────────────────────────────

function renderWidget() {
  return render(<AiSupportWidget />);
}

// ─── テストケース ─────────────────────────────────────────────

describe('AiSupportWidget', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setAiSupportState({ open: false, mode: 'NORMAL', history: [] });
  });

  describe('ボタン表示', () => {
    it('AI ボタンが表示されること', () => {
      renderWidget();
      expect(screen.getByRole('button', { name: 'button.ariaLabel' })).toBeInTheDocument();
    });

    it('初期状態でパネルは非表示であること', () => {
      renderWidget();
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
  });

  describe('パネル開閉', () => {
    it('ボタンクリックでパネルが開くこと', async () => {
      renderWidget();
      await userEvent.click(screen.getByRole('button', { name: 'button.ariaLabel' }));
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('閉じるボタンでパネルが閉じること', async () => {
      renderWidget();
      await userEvent.click(screen.getByRole('button', { name: 'button.ariaLabel' }));
      await userEvent.click(screen.getByRole('button', { name: 'panel.close' }));
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('パネルを閉じても会話履歴が消えないこと', async () => {
      mockPostAiChat.mockResolvedValueOnce({ response: 'AI回答', mode: 'NORMAL' });
      renderWidget();

      await userEvent.click(screen.getByRole('button', { name: 'button.ariaLabel' }));
      await userEvent.type(screen.getByRole('textbox', { name: 'input.ariaLabel' }), '質問');
      await userEvent.click(screen.getByRole('button', { name: 'input.send' }));
      await waitFor(() => expect(screen.getByText('AI回答')).toBeInTheDocument());

      // パネルを閉じて再度開く
      await userEvent.click(screen.getByRole('button', { name: 'panel.close' }));
      await userEvent.click(screen.getByRole('button', { name: 'button.ariaLabel' }));

      expect(screen.getByText('AI回答')).toBeInTheDocument();
    });
  });

  describe('モード切替', () => {
    it('4つのモードボタンが表示されること', async () => {
      renderWidget();
      await userEvent.click(screen.getByRole('button', { name: 'button.ariaLabel' }));
      expect(screen.getByText('mode.normal')).toBeInTheDocument();
      expect(screen.getByText('mode.proofreading')).toBeInTheDocument();
      expect(screen.getByText('mode.career')).toBeInTheDocument();
      expect(screen.getByText('mode.help')).toBeInTheDocument();
    });

    it('モードボタンクリックで会話履歴がリセットされること', async () => {
      mockPostAiChat.mockResolvedValueOnce({ response: 'AI応答', mode: 'NORMAL' });
      renderWidget();

      await userEvent.click(screen.getByRole('button', { name: 'button.ariaLabel' }));
      await userEvent.type(screen.getByRole('textbox', { name: 'input.ariaLabel' }), 'テスト');
      await userEvent.click(screen.getByRole('button', { name: 'input.send' }));
      await waitFor(() => expect(mockPostAiChat).toHaveBeenCalledTimes(1));

      await userEvent.click(screen.getByText('mode.career'));
      expect(screen.queryByText('テスト')).not.toBeInTheDocument();
    });

    it('同じモードをクリックしても履歴がクリアされないこと', async () => {
      mockPostAiChat.mockResolvedValueOnce({ response: 'AI応答', mode: 'NORMAL' });
      renderWidget();

      await userEvent.click(screen.getByRole('button', { name: 'button.ariaLabel' }));
      await userEvent.type(screen.getByRole('textbox', { name: 'input.ariaLabel' }), 'テスト');
      await userEvent.click(screen.getByRole('button', { name: 'input.send' }));
      await waitFor(() => expect(screen.getByText('AI応答')).toBeInTheDocument());

      // 同じモード（NORMAL）を再クリック
      await userEvent.click(screen.getByText('mode.normal'));
      expect(screen.getByText('AI応答')).toBeInTheDocument();
    });
  });

  describe('メッセージ送信', () => {
    it('送信ボタンクリックで API が呼ばれ応答が表示されること', async () => {
      mockPostAiChat.mockResolvedValueOnce({ response: 'AIの回答です', mode: 'NORMAL' });

      renderWidget();
      await userEvent.click(screen.getByRole('button', { name: 'button.ariaLabel' }));

      const textarea = screen.getByRole('textbox', { name: 'input.ariaLabel' });
      await userEvent.type(textarea, '質問です');
      await userEvent.click(screen.getByRole('button', { name: 'input.send' }));

      await waitFor(() => {
        expect(screen.getByText('AIの回答です')).toBeInTheDocument();
      });

      expect(mockPostAiChat).toHaveBeenCalledWith({
        message: '質問です',
        mode: 'NORMAL',
        history: [],
      });
    });

    it('空のメッセージでは API を呼ばないこと', async () => {
      renderWidget();
      await userEvent.click(screen.getByRole('button', { name: 'button.ariaLabel' }));
      await userEvent.click(screen.getByRole('button', { name: 'input.send' }));
      expect(mockPostAiChat).not.toHaveBeenCalled();
    });

    it('API エラー時にエラーメッセージを表示すること', async () => {
      mockPostAiChat.mockRejectedValueOnce(new Error('Network error'));

      renderWidget();
      await userEvent.click(screen.getByRole('button', { name: 'button.ariaLabel' }));

      const textarea = screen.getByRole('textbox', { name: 'input.ariaLabel' });
      await userEvent.type(textarea, 'テスト');
      await userEvent.click(screen.getByRole('button', { name: 'input.send' }));

      await waitFor(() => {
        expect(screen.getByText('error.failed')).toBeInTheDocument();
      });
    });

    it('Ctrl+Enter で送信できること', async () => {
      mockPostAiChat.mockResolvedValueOnce({ response: '応答', mode: 'NORMAL' });

      renderWidget();
      await userEvent.click(screen.getByRole('button', { name: 'button.ariaLabel' }));

      const textarea = screen.getByRole('textbox', { name: 'input.ariaLabel' });
      await userEvent.type(textarea, '送信テスト');
      await userEvent.keyboard('{Control>}{Enter}{/Control}');

      await waitFor(() => expect(mockPostAiChat).toHaveBeenCalledTimes(1));
    });
  });
});
