import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import NavBar from '../../../app/layouts/NavBar';
import type { AdSeminar, AdSeminarCategory } from '../../../shared/types/master';
import {
  getAdSeminars, createAdSeminar, updateAdSeminar,
  getAdSeminarCategories, createAdSeminarCategory, updateAdSeminarCategory,
} from '../../../shared/api/masterApi';
import { IconPlus, IconEdit, IconX, IconCheck } from '../../../shared/ui/Icons';
import StickyHorizontalScroll from '../../../shared/ui/StickyHorizontalScroll';

type ModalMode = 'create' | 'edit';

// ─── Category tab ─────────────────────────────────────────────────────────────

interface CatForm { name: string; sortOrder: string; active: boolean }

function CategoryTab({ categories, onReload }: { categories: AdSeminarCategory[]; onReload: () => void }) {
  const { t } = useTranslation('master');
  const [modalOpen, setModalOpen] = useState(false);
  const [mode, setMode] = useState<ModalMode>('create');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<CatForm>({ name: '', sortOrder: '0', active: true });
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const openCreate = () => {
    setForm({ name: '', sortOrder: '0', active: true });
    setFormError('');
    setMode('create');
    setEditingId(null);
    setModalOpen(true);
  };

  const openEdit = (c: AdSeminarCategory) => {
    setForm({ name: c.name, sortOrder: String(c.sortOrder), active: c.isActive });
    setFormError('');
    setMode('edit');
    setEditingId(c.id);
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    if (!form.name.trim()) { setFormError(t('adSeminar.category.validation.nameRequired')); return; }
    setSaving(true); setFormError('');
    try {
      if (mode === 'create') {
        await createAdSeminarCategory({ name: form.name, sortOrder: Number(form.sortOrder) || 0 });
      } else {
        await updateAdSeminarCategory(editingId!, { name: form.name, sortOrder: Number(form.sortOrder) || 0, active: form.active });
      }
      setModalOpen(false);
      onReload();
    } catch {
      setFormError(t('common.saveFailed'));
    } finally { setSaving(false); }
  };

  return (
    <>
      <div className="master-card__header" style={{ marginBottom: 16 }}>
        <h3 className="master-card__title" style={{ marginBottom: 0 }}>{t('adSeminar.category.listTitle')}</h3>
        <button className="btn btn--primary btn--sm" onClick={openCreate}>
          <IconPlus size={12} />{t('adSeminar.category.addButton')}
        </button>
      </div>

      <StickyHorizontalScroll className="master-table-wrap">
        <table className="master-table">
          <thead>
            <tr>
              <th>{t('adSeminar.category.table.name')}</th>
              <th style={{ width: 64 }}>{t('common.sortOrder')}</th>
              <th style={{ width: 72 }}>{t('common.status')}</th>
              <th style={{ width: 72 }}>{t('common.actions')}</th>
            </tr>
          </thead>
          <tbody>
            {categories.length === 0 ? (
              <tr><td colSpan={4} className="master-table__empty">{t('common.noData')}</td></tr>
            ) : (
              categories.map(c => (
                <tr key={c.id}>
                  <td>{c.name}</td>
                  <td style={{ textAlign: 'center' }}>{c.sortOrder}</td>
                  <td>
                    <span className={c.isActive ? 'fy-status fy-status--active' : 'fy-status fy-status--inactive'}>
                      {c.isActive ? t('common.activeLabel') : t('common.inactiveLabel')}
                    </span>
                  </td>
                  <td>
                    <button className="btn btn--secondary btn--sm" onClick={() => openEdit(c)}>
                      <IconEdit size={12} />{t('common.edit')}
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </StickyHorizontalScroll>

      {modalOpen && createPortal(
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal__header">
              <h3>{mode === 'create' ? t('adSeminar.modalCategoryCreate') : t('adSeminar.modalCategoryEdit')}</h3>
              <button className="modal__close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal__body">
              {formError && <div className="alert alert--error">{formError}</div>}
              <div className="form-group">
                <label className="form-label">{t('adSeminar.category.form.nameLabel')} <span className="required">*</span></label>
                <input className="form-input" value={form.name}
                  onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  placeholder={t('adSeminar.category.form.namePlaceholder')} maxLength={100} />
              </div>
              <div className="form-group">
                <label className="form-label">{t('common.sortOrder')}</label>
                <input type="number" className="form-input" value={form.sortOrder}
                  onChange={e => setForm(f => ({ ...f, sortOrder: e.target.value }))}
                  style={{ width: 100 }} />
              </div>
              {mode === 'edit' && (
                <div className="form-group">
                  <label className="form-check">
                    <input type="checkbox" checked={form.active}
                      onChange={e => setForm(f => ({ ...f, active: e.target.checked }))} />
                    <span>{t('common.activeLabel')}</span>
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
        </div>,
        document.body
      )}
    </>
  );
}

// ─── AD seminar tab ───────────────────────────────────────────────────────────

interface AdForm {
  categoryId: number | null;
  name: string;
  description: string;
  sortOrder: string;
  active: boolean;
}

function AdSeminarTab({ adSeminars, categories, onReload }: {
  adSeminars: AdSeminar[];
  categories: AdSeminarCategory[];
  onReload: () => void;
}) {
  const { t } = useTranslation('master');
  const [filterCatId, setFilterCatId] = useState<number | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [mode, setMode] = useState<ModalMode>('create');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<AdForm>({ categoryId: null, name: '', description: '', sortOrder: '0', active: true });
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const filtered = filterCatId
    ? adSeminars.filter(a => a.categoryId === filterCatId)
    : adSeminars;

  const openCreate = () => {
    setForm({ categoryId: null, name: '', description: '', sortOrder: '0', active: true });
    setFormError('');
    setMode('create');
    setEditingId(null);
    setModalOpen(true);
  };

  const openEdit = (a: AdSeminar) => {
    setForm({
      categoryId: a.categoryId,
      name: a.name,
      description: a.description ?? '',
      sortOrder: String(a.sortOrder),
      active: a.isActive,
    });
    setFormError('');
    setMode('edit');
    setEditingId(a.id);
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    if (!form.name.trim()) { setFormError(t('adSeminar.seminar.validation.nameRequired')); return; }
    setSaving(true); setFormError('');
    try {
      const payload = {
        categoryId: form.categoryId,
        name: form.name,
        description: form.description || null,
        sortOrder: Number(form.sortOrder) || 0,
      };
      if (mode === 'create') {
        await createAdSeminar(payload);
      } else {
        await updateAdSeminar(editingId!, { ...payload, active: form.active });
      }
      setModalOpen(false);
      onReload();
    } catch {
      setFormError(t('common.saveFailed'));
    } finally { setSaving(false); }
  };

  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
        <label className="master-label" style={{ minWidth: 'auto' }}>{t('adSeminar.seminar.filterLabel')}</label>
        <select className="master-select"
          value={filterCatId ?? ''}
          onChange={e => setFilterCatId(e.target.value === '' ? null : Number(e.target.value))}>
          <option value="">{t('common.allOption')}</option>
          {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <button className="btn btn--primary btn--sm" onClick={openCreate} style={{ marginLeft: 'auto' }}>
          <IconPlus size={12} />{t('adSeminar.seminar.addButton')}
        </button>
      </div>

      <StickyHorizontalScroll className="master-table-wrap">
        <table className="master-table">
          <thead>
            <tr>
              <th>{t('adSeminar.seminar.table.adName')}</th>
              <th style={{ width: 160 }}>{t('adSeminar.table.category')}</th>
              <th style={{ width: 200 }}>{t('adSeminar.table.description')}</th>
              <th style={{ width: 56 }}>{t('common.sortOrder')}</th>
              <th style={{ width: 72 }}>{t('common.status')}</th>
              <th style={{ width: 72 }}>{t('common.actions')}</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr><td colSpan={6} className="master-table__empty">{t('common.noData')}</td></tr>
            ) : (
              filtered.map(a => (
                <tr key={a.id}>
                  <td>{a.name}</td>
                  <td style={{ fontSize: 13, color: 'var(--color-text-muted)' }}>{a.categoryName ?? '—'}</td>
                  <td style={{ fontSize: 13, maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {a.description ?? '—'}
                  </td>
                  <td style={{ textAlign: 'center' }}>{a.sortOrder}</td>
                  <td>
                    <span className={a.isActive ? 'fy-status fy-status--active' : 'fy-status fy-status--inactive'}>
                      {a.isActive ? t('common.activeLabel') : t('common.inactiveLabel')}
                    </span>
                  </td>
                  <td>
                    <button className="btn btn--secondary btn--sm" onClick={() => openEdit(a)}>
                      <IconEdit size={12} />{t('common.edit')}
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </StickyHorizontalScroll>

      {modalOpen && createPortal(
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal__header">
              <h3>{mode === 'create' ? t('adSeminar.modalCreate') : t('adSeminar.modalEdit')}</h3>
              <button className="modal__close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal__body">
              {formError && <div className="alert alert--error">{formError}</div>}
              <div className="form-group">
                <label className="form-label">{t('adSeminar.form.categoryLabel')}</label>
                <select className="master-select" style={{ width: '100%' }}
                  value={form.categoryId ?? ''}
                  onChange={e => setForm(f => ({ ...f, categoryId: e.target.value === '' ? null : Number(e.target.value) }))}>
                  <option value="">{t('common.noCategoryOption')}</option>
                  {categories.filter(c => c.isActive).map(c => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">{t('adSeminar.seminar.form.adNameLabel')} <span className="required">*</span></label>
                <input className="form-input" value={form.name}
                  onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  placeholder="例: AWS研修" maxLength={200} />
              </div>
              <div className="form-group">
                <label className="form-label">{t('adSeminar.form.descriptionLabel')}</label>
                <textarea className="form-input" value={form.description}
                  onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                  rows={3} placeholder={t('adSeminar.seminar.form.descriptionPlaceholder')} style={{ resize: 'vertical' }} />
              </div>
              <div className="form-group">
                <label className="form-label">{t('common.sortOrder')}</label>
                <input type="number" className="form-input" value={form.sortOrder}
                  onChange={e => setForm(f => ({ ...f, sortOrder: e.target.value }))}
                  style={{ width: 100 }} />
              </div>
              {mode === 'edit' && (
                <div className="form-group">
                  <label className="form-check">
                    <input type="checkbox" checked={form.active}
                      onChange={e => setForm(f => ({ ...f, active: e.target.checked }))} />
                    <span>{t('common.activeLabel')}</span>
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
        </div>,
        document.body
      )}
    </>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

type TabKey = 'categories' | 'adSeminars';

export default function AdSeminarMasterPage() {
  const { t } = useTranslation('master');
  const [activeTab, setActiveTab] = useState<TabKey>('categories');
  const [categories, setCategories] = useState<AdSeminarCategory[]>([]);
  const [adSeminars, setAdSeminars] = useState<AdSeminar[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadAll = () => {
    setLoading(true);
    Promise.all([getAdSeminarCategories(), getAdSeminars()])
      .then(([catRes, adRes]) => {
        setCategories(catRes.data);
        setAdSeminars(adRes.data);
      })
      .catch(() => setError(t('common.loadFailed')))
      .finally(() => setLoading(false));
  };

  useEffect(() => { loadAll(); }, []);

  if (loading) return <div className="loading-screen"><span>{t('loading')}</span></div>;

  return (
    <div className="master-page">
      <NavBar />

      <main className="master-main">
        {error && <div className="alert alert--error">{error}</div>}

        <div className="tab-bar">
          <button className={`tab-btn${activeTab === 'categories' ? ' active' : ''}`}
            onClick={() => setActiveTab('categories')}>
            {t('adSeminar.tab.categories')}
          </button>
          <button className={`tab-btn${activeTab === 'adSeminars' ? ' active' : ''}`}
            onClick={() => setActiveTab('adSeminars')}>
            {t('adSeminar.tab.adSeminars')}
          </button>
        </div>

        <section className="master-card">
          {activeTab === 'categories' ? (
            <CategoryTab categories={categories} onReload={loadAll} />
          ) : (
            <AdSeminarTab adSeminars={adSeminars} categories={categories} onReload={loadAll} />
          )}
        </section>
      </main>
    </div>
  );
}
