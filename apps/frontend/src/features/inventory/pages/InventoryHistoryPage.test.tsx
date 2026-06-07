import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import InventoryHistoryPage from './InventoryHistoryPage';
import * as inventoryApi from '../api/inventoryApi';
import * as aiAnalysisApi from '../../ai/api/aiAnalysisApi';
import * as masterApi from '../../../shared/api/masterApi';

// ─── モック定義 ───────────────────────────────────────────────

vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => vi.fn() };
});

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

vi.mock('../api/inventoryApi');
vi.mock('../../ai/api/aiAnalysisApi');
vi.mock('../../../shared/api/masterApi');
vi.mock('../../../app/layouts/NavBar', () => ({ default: () => null }));
vi.mock('../../ai/components/AiAnalysisCard', () => ({ default: () => null }));
vi.mock('../../../shared/ui/StickyHorizontalScroll', () => ({
  default: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

// ─── テストデータ ─────────────────────────────────────────────

const inventorySummary = {
  id: 1,
  fiscalYear: { id: 2, name: '2025年度' },
  status: 'DRAFT' as const,
  submittedAt: null,
  goalCompletedAt: null,
};

const masterSkillDetail = {
  id: 101, itSkillId: 10, itSkillName: 'Java', customSkillName: null,
  skillLevelId: 3, levelValue: 3, remarks: null,
};

const customSkillDetail = {
  id: 102, itSkillId: null, itSkillName: null, customSkillName: '自作スキル',
  skillLevelId: 2, levelValue: 2, remarks: null,
};

const itSkillMaster = [{
  id: 10, categoryId: 1, category1Id: 1, category1Name: 'プログラミング',
  category1SortOrder: 1, category2Name: null, category3Name: null,
  name: 'Java', description: null, sortOrder: 1, isActive: true,
}];

const comparisonWithPrevYear = {
  inventoryId: 1, currentFiscalYear: '2025年度', prevFiscalYear: '2024年度',
  hasPrevYear: true,
  items: [
    { itSkillId: 10, skillName: 'Java', currentDetailId: 101, currentLevelValue: 3, currentRemarks: null, prevLevelValue: 2, diff: 1 },
  ],
};

// ─── テストヘルパー ───────────────────────────────────────────

const mockInventoryApi = vi.mocked(inventoryApi);
const mockAiAnalysisApi = vi.mocked(aiAnalysisApi);
const mockMasterApi = vi.mocked(masterApi);

function setupDefaultMocks() {
  mockInventoryApi.getMyInventories.mockResolvedValue({ data: [inventorySummary] } as never);
  mockAiAnalysisApi.getMyAiAnalyses.mockResolvedValue({ data: [] } as never);
  mockInventoryApi.getItSkillDetails.mockResolvedValue({ data: { items: [masterSkillDetail, customSkillDetail] } } as never);
  mockInventoryApi.getQualificationDetails.mockResolvedValue({ data: { items: [] } } as never);
  mockInventoryApi.getSeminarDetails.mockResolvedValue({ data: { items: [] } } as never);
  mockInventoryApi.getGoals.mockResolvedValue({ data: { items: [] } } as never);
  mockInventoryApi.getComparison.mockResolvedValue({ data: comparisonWithPrevYear } as never);
  mockMasterApi.getItSkills.mockResolvedValue({ data: itSkillMaster } as never);
  mockMasterApi.getFiscalYears.mockResolvedValue({ data: [] } as never);
}

function renderPage() {
  return render(<MemoryRouter><InventoryHistoryPage /></MemoryRouter>);
}

// diff フィルター select を取得するヘルパー
function getDiffFilterSelect(): HTMLElement {
  const selects = screen.getAllByRole('combobox');
  const diffSelect = selects.find(el =>
    el.querySelector('option[value="up"]') !== null
  );
  if (!diffSelect) throw new Error('diff filter select が見つかりません');
  return diffSelect;
}

// ─── テストスイート ───────────────────────────────────────────

describe('InventoryHistoryPage', () => {

  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ─── 基本表示 ────────────────────────────────────────────

  it('正常系_棚卸データなし_noDataメッセージが表示される', async () => {
    mockInventoryApi.getMyInventories.mockResolvedValue({ data: [] } as never);
    mockAiAnalysisApi.getMyAiAnalyses.mockResolvedValue({ data: [] } as never);
    mockMasterApi.getFiscalYears.mockResolvedValue({ data: [] } as never);

    renderPage();

    await waitFor(() =>
      expect(screen.getByText('historyPage.noData')).toBeInTheDocument()
    );
  });

  it('正常系_棚卸データあり_年度セレクタとITスキルタブが表示される', async () => {
    setupDefaultMocks();
    renderPage();

    await waitFor(() =>
      expect(screen.getByDisplayValue('2025年度（historyPage.status.draft）')).toBeInTheDocument()
    );
    expect(screen.getByRole('button', { name: 'historyPage.tab.itSkills' })).toBeInTheDocument();
  });

  // ─── カスタムスキル表示 ──────────────────────────────────

  it('正常系_前年度あり_フィルターなし_カスタムスキルが表示される', async () => {
    setupDefaultMocks();
    renderPage();

    await waitFor(() =>
      expect(screen.getByText('自作スキル ※')).toBeInTheDocument()
    );
    // DiffCell で diff=null, hasPrevYear=true → 新規ラベルが表示される
    expect(document.querySelector('.diff-new')).toBeInTheDocument();
  });

  it('正常系_前年度あり_新規フィルター選択_カスタムスキルが表示される', async () => {
    setupDefaultMocks();
    renderPage();

    await waitFor(() => screen.getByText('自作スキル ※'));

    fireEvent.change(getDiffFilterSelect(), { target: { value: 'new' } });

    await waitFor(() =>
      expect(screen.getByText('自作スキル ※')).toBeInTheDocument()
    );
  });

  // ─── カスタムスキル非表示（バグ修正検証） ────────────────

  it('正常系_前年度あり_上昇フィルター選択_カスタムスキルが非表示になる', async () => {
    setupDefaultMocks();
    renderPage();

    await waitFor(() => screen.getByText('自作スキル ※'));

    fireEvent.change(getDiffFilterSelect(), { target: { value: 'up' } });

    await waitFor(() =>
      expect(screen.queryByText('自作スキル ※')).not.toBeInTheDocument()
    );
  });

  it('正常系_前年度あり_下降フィルター選択_カスタムスキルが非表示になる', async () => {
    setupDefaultMocks();
    renderPage();

    await waitFor(() => screen.getByText('自作スキル ※'));

    fireEvent.change(getDiffFilterSelect(), { target: { value: 'down' } });

    await waitFor(() =>
      expect(screen.queryByText('自作スキル ※')).not.toBeInTheDocument()
    );
  });

  it('正常系_前年度あり_上昇フィルター後に全表示に戻す_カスタムスキルが再表示される', async () => {
    setupDefaultMocks();
    renderPage();

    await waitFor(() => screen.getByText('自作スキル ※'));

    // 上昇フィルターで非表示
    fireEvent.change(getDiffFilterSelect(), { target: { value: 'up' } });
    await waitFor(() =>
      expect(screen.queryByText('自作スキル ※')).not.toBeInTheDocument()
    );

    // フィルター解除で再表示
    fireEvent.change(getDiffFilterSelect(), { target: { value: '' } });
    await waitFor(() =>
      expect(screen.getByText('自作スキル ※')).toBeInTheDocument()
    );
  });

  // ─── 前年度なし ──────────────────────────────────────────

  it('正常系_前年度なし_diffフィルターが表示されない', async () => {
    setupDefaultMocks();
    mockInventoryApi.getComparison.mockResolvedValue({
      data: {
        inventoryId: 1, currentFiscalYear: '2025年度', prevFiscalYear: null,
        hasPrevYear: false, items: [],
      },
    } as never);

    renderPage();

    await waitFor(() => screen.getByText('自作スキル ※'));

    // hasPrevYear=false なので diff フィルターが存在しない
    const selects = screen.getAllByRole('combobox');
    const hasDiffFilter = selects.some(el => el.querySelector('option[value="up"]') !== null);
    expect(hasDiffFilter).toBe(false);
  });

  // ─── APIエラー ───────────────────────────────────────────

  it('異常系_comparison取得失敗_比較なしで表示される', async () => {
    setupDefaultMocks();
    // getComparison は .catch(() => null) で失敗を吸収する
    mockInventoryApi.getComparison.mockRejectedValue(new Error('Not found'));

    renderPage();

    // comparison がなくても ITスキルは表示される
    await waitFor(() =>
      expect(screen.getByText('自作スキル ※')).toBeInTheDocument()
    );
    // hasPrevYear=false (comparison=null) → diff フィルターなし
    const selects = screen.getAllByRole('combobox');
    const hasDiffFilter = selects.some(el => el.querySelector('option[value="up"]') !== null);
    expect(hasDiffFilter).toBe(false);
  });
});
