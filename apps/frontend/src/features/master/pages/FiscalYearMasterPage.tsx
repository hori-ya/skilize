import { useEffect, useState } from 'react';
import NavBar from '../../../app/layouts/NavBar';
import type { FiscalYear, FiscalYearSettings } from '../../../shared/types/master';
import {
  getFiscalYears,
  getFiscalYearSettings,
  updateFiscalYearSettings,
  createFiscalYear,
  updateFiscalYear,
} from '../../../shared/api/masterApi';
import { IconPlus, IconEdit, IconX, IconCheck } from '../../../shared/ui/Icons';
import StickyHorizontalScroll from '../../../shared/ui/StickyHorizontalScroll';

type ModalMode = 'create' | 'edit';

interface FormState {
  name: string;
  startDate: string;
  endDate: string;
  inputStartDate: string;
  inputEndDate: string;
  active: boolean;
}

const emptyForm = (): FormState => ({
  name: '',
  startDate: '',
  endDate: '',
  inputStartDate: '',
  inputEndDate: '',
  active: true,
});

function fiscalYearStatus(fy: FiscalYear): string {
  const today = new Date().toISOString().slice(0, 10);
  if (!fy.isActive) return '無効';
  if (today < fy.startDate) return '予定';
  if (today > fy.endDate) return '完了';
  return '進行中';
}

function statusClass(status: string) {
  if (status === '進行中') return 'fy-status fy-status--active';
  if (status === '予定') return 'fy-status fy-status--planned';
  if (status === '完了') return 'fy-status fy-status--done';
  return 'fy-status fy-status--inactive';
}

export default function FiscalYearMasterPage() {
  const [fiscalYears, setFiscalYears] = useState<FiscalYear[]>([]);
  const [settings, setSettings] = useState<FiscalYearSettings | null>(null);
  const [startMonth, setStartMonth] = useState<number>(4);
  const [settingsSaved, setSettingsSaved] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState<ModalMode>('create');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<FormState>(emptyForm());
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    Promise.all([getFiscalYears(), getFiscalYearSettings()])
      .then(([fyRes, sRes]) => {
        setFiscalYears(fyRes.data);
        setSettings(sRes.data);
        setStartMonth(sRes.data.fiscalYearStartMonth);
      })
      .catch(() => setError('データの取得に失敗しました'))
      .finally(() => setLoading(false));
  }, []);

  const handleSaveSettings = async () => {
    try {
      const res = await updateFiscalYearSettings({ fiscalYearStartMonth: startMonth });
      setSettings(res.data);
      setSettingsSaved(true);
      setTimeout(() => setSettingsSaved(false), 2000);
    } catch {
      setError('設定の保存に失敗しました');
    }
  };

  const openCreate = () => {
    setForm(emptyForm());
    setFormError('');
    setModalMode('create');
    setEditingId(null);
    setModalOpen(true);
  };

  const openEdit = (fy: FiscalYear) => {
    setForm({
      name: fy.name,
      startDate: fy.startDate,
      endDate: fy.endDate,
      inputStartDate: fy.inputStartDate ?? '',
      inputEndDate: fy.inputEndDate ?? '',
      active: fy.isActive,
    });
    setFormError('');
    setModalMode('edit');
    setEditingId(fy.id);
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    if (!form.name || !form.startDate || !form.endDate) {
      setFormError('年度名・開始日・終了日は必須です');
      return;
    }
    if (form.startDate >= form.endDate) {
      setFormError('終了日は開始日より後の日付を指定してください');
      return;
    }

    setSaving(true);
    setFormError('');
    try {
      const payload = {
        name: form.name,
        startDate: form.startDate,
        endDate: form.endDate,
        inputStartDate: form.inputStartDate || null,
        inputEndDate: form.inputEndDate || null,
        active: form.active,
      };

      if (modalMode === 'create') {
        const res = await createFiscalYear(payload);
        setFiscalYears(prev =>
          [...prev, res.data].sort((a, b) => b.startDate.localeCompare(a.startDate))
        );
      } else {
        const res = await updateFiscalYear(editingId!, payload);
        setFiscalYears(prev =>
          prev.map(f => (f.id === editingId ? res.data : f))
            .sort((a, b) => b.startDate.localeCompare(a.startDate))
        );
      }
      setModalOpen(false);
    } catch (e: unknown) {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setFormError(msg ?? '保存に失敗しました');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="loading-screen"><span>読み込み中...</span></div>;

  return (
    <div className="master-page">
      <NavBar />

      <main className="master-main">
        {error && <div className="alert alert--error">{error}</div>}

        {/* Settings card */}
        <section className="master-card">
          <h2 className="master-card__title">会計年度設定</h2>
          <div className="master-card__row">
            <label className="master-label">会計年度開始月</label>
            <select
              className="master-select"
              value={startMonth}
              onChange={e => setStartMonth(Number(e.target.value))}
            >
              {Array.from({ length: 12 }, (_, i) => i + 1).map(m => (
                <option key={m} value={m}>{m}月</option>
              ))}
            </select>
            <button className="btn btn--primary btn--sm" onClick={handleSaveSettings}>
              <IconCheck size={12} />保存
            </button>
            {settingsSaved && <span className="master-saved">保存しました</span>}
          </div>
          {settings && (
            <p className="master-card__hint">
              現在の設定: {settings.fiscalYearStartMonth}月始まり
            </p>
          )}
        </section>

        {/* Fiscal years list */}
        <section className="master-card">
          <div className="master-card__header">
            <h2 className="master-card__title">年度一覧</h2>
            <button className="btn btn--primary btn--sm" onClick={openCreate}><IconPlus size={12} />年度追加</button>
          </div>

          <StickyHorizontalScroll className="master-table-wrap">
            <table className="master-table">
              <thead>
                <tr>
                  <th>年度名</th>
                  <th>開始日</th>
                  <th>終了日</th>
                  <th>入力開始日</th>
                  <th>入力締切日</th>
                  <th>状態</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {fiscalYears.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="master-table__empty">年度データがありません</td>
                  </tr>
                ) : (
                  fiscalYears.map(fy => {
                    const status = fiscalYearStatus(fy);
                    return (
                      <tr key={fy.id}>
                        <td>{fy.name}</td>
                        <td>{fy.startDate}</td>
                        <td>{fy.endDate}</td>
                        <td>{fy.inputStartDate ?? '—'}</td>
                        <td>{fy.inputEndDate ?? '—'}</td>
                        <td><span className={statusClass(status)}>{status}</span></td>
                        <td>
                          <button className="btn btn--secondary btn--sm" onClick={() => openEdit(fy)}>
                            <IconEdit size={12} />編集
                          </button>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </StickyHorizontalScroll>
        </section>
      </main>

      {/* Modal */}
      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal__header">
              <h3>{modalMode === 'create' ? '年度追加' : '年度編集'}</h3>
              <button className="modal__close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal__body">
              {formError && <div className="alert alert--error">{formError}</div>}

              <div className="form-group">
                <label className="form-label">年度名 <span className="required">*</span></label>
                <input
                  className="form-input"
                  value={form.name}
                  onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  placeholder="例: 2025年度"
                  maxLength={20}
                />
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">開始日 <span className="required">*</span></label>
                  <input
                    type="date"
                    className="form-input"
                    value={form.startDate}
                    onChange={e => setForm(f => ({ ...f, startDate: e.target.value }))}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">終了日 <span className="required">*</span></label>
                  <input
                    type="date"
                    className="form-input"
                    value={form.endDate}
                    onChange={e => setForm(f => ({ ...f, endDate: e.target.value }))}
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">入力開始日</label>
                  <input
                    type="date"
                    className="form-input"
                    value={form.inputStartDate}
                    onChange={e => setForm(f => ({ ...f, inputStartDate: e.target.value }))}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">入力締切日</label>
                  <input
                    type="date"
                    className="form-input"
                    value={form.inputEndDate}
                    onChange={e => setForm(f => ({ ...f, inputEndDate: e.target.value }))}
                  />
                </div>
              </div>

              {modalMode === 'edit' && (
                <div className="form-group">
                  <label className="form-check">
                    <input
                      type="checkbox"
                      checked={form.active}
                      onChange={e => setForm(f => ({ ...f, active: e.target.checked }))}
                    />
                    <span>有効</span>
                  </label>
                </div>
              )}
            </div>
            <div className="modal__footer">
              <button className="btn btn--secondary" onClick={() => setModalOpen(false)}>
                <IconX size={13} />キャンセル
              </button>
              <button className="btn btn--primary" onClick={handleSubmit} disabled={saving}>
                <IconCheck size={13} />{saving ? '保存中...' : '保存'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
