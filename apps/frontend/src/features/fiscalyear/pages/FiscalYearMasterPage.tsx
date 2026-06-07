import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
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
type FiscalYearStatusKey = 'active' | 'planned' | 'done' | 'inactive';

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

function fiscalYearStatusKey(fy: FiscalYear): FiscalYearStatusKey {
  const today = new Date().toISOString().slice(0, 10);
  if (!fy.isActive) return 'inactive';
  if (today < fy.startDate) return 'planned';
  if (today > fy.endDate) return 'done';
  return 'active';
}

const STATUS_CLASS: Record<FiscalYearStatusKey, string> = {
  active: 'fy-status fy-status--active',
  planned: 'fy-status fy-status--planned',
  done: 'fy-status fy-status--done',
  inactive: 'fy-status fy-status--inactive',
};

export default function FiscalYearMasterPage() {
  const { t } = useTranslation('master');
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
      .catch(() => setError(t('common.loadFailed')))
      .finally(() => setLoading(false));
  }, []);

  const handleSaveSettings = async () => {
    try {
      const res = await updateFiscalYearSettings({ fiscalYearStartMonth: startMonth });
      setSettings(res.data);
      setSettingsSaved(true);
      setTimeout(() => setSettingsSaved(false), 2000);
    } catch {
      setError(t('fiscalYear.validation.settingsSaveFailed'));
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
      setFormError(t('fiscalYear.validation.requiredFields'));
      return;
    }
    if (form.startDate >= form.endDate) {
      setFormError(t('fiscalYear.validation.endDateAfterStart'));
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
      setFormError(msg ?? t('common.saveFailed'));
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="loading-screen"><span>{t('loading')}</span></div>;

  return (
    <div className="master-page">
      <NavBar />

      <main className="master-main">
        {error && <div className="alert alert--error">{error}</div>}

        {/* Settings card */}
        <section className="master-card">
          <h2 className="master-card__title">{t('fiscalYear.settingsCardTitle')}</h2>
          <div className="master-card__row">
            <label className="master-label">{t('fiscalYear.settingsStartMonthLabel')}</label>
            <select
              className="master-select"
              value={startMonth}
              onChange={e => setStartMonth(Number(e.target.value))}
            >
              {Array.from({ length: 12 }, (_, i) => i + 1).map(m => (
                <option key={m} value={m}>{t('fiscalYear.month', { n: m })}</option>
              ))}
            </select>
            <button className="btn btn--primary btn--sm" onClick={handleSaveSettings}>
              <IconCheck size={12} />{t('common.save')}
            </button>
            {settingsSaved && <span className="master-saved">{t('fiscalYear.settingsSaved')}</span>}
          </div>
          {settings && (
            <p className="master-card__hint">
              {t('fiscalYear.currentSettingHint', { month: settings.fiscalYearStartMonth })}
            </p>
          )}
        </section>

        {/* Fiscal years list */}
        <section className="master-card">
          <div className="master-card__header">
            <h2 className="master-card__title">{t('fiscalYear.listTitle')}</h2>
            <button className="btn btn--primary btn--sm" onClick={openCreate}>
              <IconPlus size={12} />{t('fiscalYear.addButton')}
            </button>
          </div>

          <StickyHorizontalScroll className="master-table-wrap">
            <table className="master-table">
              <thead>
                <tr>
                  <th>{t('fiscalYear.table.name')}</th>
                  <th>{t('fiscalYear.table.startDate')}</th>
                  <th>{t('fiscalYear.table.endDate')}</th>
                  <th>{t('fiscalYear.table.inputStartDate')}</th>
                  <th>{t('fiscalYear.table.inputEndDate')}</th>
                  <th>{t('fiscalYear.table.status')}</th>
                  <th>{t('common.actions')}</th>
                </tr>
              </thead>
              <tbody>
                {fiscalYears.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="master-table__empty">{t('fiscalYear.emptyData')}</td>
                  </tr>
                ) : (
                  fiscalYears.map(fy => {
                    const sKey = fiscalYearStatusKey(fy);
                    return (
                      <tr key={fy.id}>
                        <td>{fy.name}</td>
                        <td>{fy.startDate}</td>
                        <td>{fy.endDate}</td>
                        <td>{fy.inputStartDate ?? '—'}</td>
                        <td>{fy.inputEndDate ?? '—'}</td>
                        <td><span className={STATUS_CLASS[sKey]}>{t(`fiscalYear.status.${sKey}`)}</span></td>
                        <td>
                          <button className="btn btn--secondary btn--sm" onClick={() => openEdit(fy)}>
                            <IconEdit size={12} />{t('common.edit')}
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
              <h3>{modalMode === 'create' ? t('fiscalYear.modalCreate') : t('fiscalYear.modalEdit')}</h3>
              <button className="modal__close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal__body">
              {formError && <div className="alert alert--error">{formError}</div>}

              <div className="form-group">
                <label className="form-label">{t('fiscalYear.form.nameLabel')} <span className="required">*</span></label>
                <input
                  className="form-input"
                  value={form.name}
                  onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  placeholder={t('fiscalYear.form.namePlaceholder')}
                  maxLength={20}
                />
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">{t('fiscalYear.form.startDateLabel')} <span className="required">*</span></label>
                  <input
                    type="date"
                    className="form-input"
                    value={form.startDate}
                    onChange={e => setForm(f => ({ ...f, startDate: e.target.value }))}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">{t('fiscalYear.form.endDateLabel')} <span className="required">*</span></label>
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
                  <label className="form-label">{t('fiscalYear.form.inputStartDateLabel')}</label>
                  <input
                    type="date"
                    className="form-input"
                    value={form.inputStartDate}
                    onChange={e => setForm(f => ({ ...f, inputStartDate: e.target.value }))}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">{t('fiscalYear.form.inputEndDateLabel')}</label>
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
                    <span>{t('fiscalYear.form.activeLabel')}</span>
                  </label>
                </div>
              )}
            </div>
            <div className="modal__footer">
              <button className="btn btn--secondary" onClick={() => setModalOpen(false)}>
                <IconX size={13} />{t('common.cancel')}
              </button>
              <button className="btn btn--primary" onClick={handleSubmit} disabled={saving}>
                <IconCheck size={13} />{saving ? t('common.saving') : t('common.save')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
