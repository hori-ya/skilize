/*******************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * 資格マスタ管理ページ。資格カテゴリ・資格・未登録カスタム資格の昇格を
 * タブ切り替えで管理する。Excel インポート・エクスポート機能も提供する。
 * ADMIN ロールのみアクセス可能。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import NavBar from '../../../app/layouts/NavBar';
import type { Qualification, QualificationCategory, CustomUnregisteredItem, MasterImportError, MasterImportResult } from '../../../shared/types/master';
import {
  getQualifications, createQualification, updateQualification,
  getQualificationCategories, createQualificationCategory, updateQualificationCategory,
  getCustomUnregisteredQualifications, promoteQualification,
  downloadQualificationExcel, uploadQualificationExcel,
} from '../../../shared/api/masterApi';
import { IconPlus, IconEdit, IconX, IconCheck } from '../../../shared/ui/Icons';
import StickyHorizontalScroll from '../../../shared/ui/StickyHorizontalScroll';

type ModalMode = 'create' | 'edit';

// ─── Category tab ─────────────────────────────────────────────────────────────

interface CatForm { name: string; sortOrder: string; active: boolean }

/**
 * 資格カテゴリ管理タブ。
 *
 * 資格カテゴリの一覧表示・新規作成・編集を行う。
 */
function CategoryTab({ categories, onReload }: { categories: QualificationCategory[]; onReload: () => void }) {
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

  const openEdit = (c: QualificationCategory) => {
    setForm({ name: c.name, sortOrder: String(c.sortOrder), active: c.isActive });
    setFormError('');
    setMode('edit');
    setEditingId(c.id);
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    if (!form.name.trim()) { setFormError(t('qualification.category.validation.nameRequired')); return; }
    setSaving(true); setFormError('');
    try {
      if (mode === 'create') {
        await createQualificationCategory({ name: form.name, sortOrder: Number(form.sortOrder) || 0 });
      } else {
        await updateQualificationCategory(editingId!, { name: form.name, sortOrder: Number(form.sortOrder) || 0, active: form.active });
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
        <h3 className="master-card__title" style={{ marginBottom: 0 }}>{t('qualification.category.listTitle')}</h3>
        <button className="btn btn--primary btn--sm" onClick={openCreate}>
          <IconPlus size={12} />{t('qualification.category.addButton')}
        </button>
      </div>

      <StickyHorizontalScroll className="master-table-wrap">
        <table className="master-table">
          <thead>
            <tr>
              <th>{t('qualification.category.table.name')}</th>
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
              <h3>{mode === 'create' ? t('qualification.modalCategoryCreate') : t('qualification.modalCategoryEdit')}</h3>
              <button className="modal__close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal__body">
              {formError && <div className="alert alert--error">{formError}</div>}
              <div className="form-group">
                <label className="form-label">{t('qualification.category.form.nameLabel')} <span className="required">*</span></label>
                <input className="form-input" value={form.name}
                  onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  placeholder={t('qualification.category.form.namePlaceholder')} maxLength={100} />
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

// ─── Qualification tab ────────────────────────────────────────────────────────

interface QualForm {
  categoryId: number | null;
  name: string;
  description: string;
  sortOrder: string;
  active: boolean;
}

/**
 * 資格管理タブ。
 *
 * 資格の一覧表示・カテゴリフィルタ・新規作成・編集を行う。
 */
function QualificationTab({ qualifications, categories, onReload }: {
  qualifications: Qualification[];
  categories: QualificationCategory[];
  onReload: () => void;
}) {
  const { t } = useTranslation('master');
  const [filterCatId, setFilterCatId] = useState<number | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [mode, setMode] = useState<ModalMode>('create');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<QualForm>({ categoryId: null, name: '', description: '', sortOrder: '0', active: true });
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const filtered = filterCatId
    ? qualifications.filter(q => q.categoryId === filterCatId)
    : qualifications;

  const openCreate = () => {
    setForm({ categoryId: null, name: '', description: '', sortOrder: '0', active: true });
    setFormError('');
    setMode('create');
    setEditingId(null);
    setModalOpen(true);
  };

  const openEdit = (q: Qualification) => {
    setForm({
      categoryId: q.categoryId,
      name: q.name,
      description: q.description ?? '',
      sortOrder: String(q.sortOrder),
      active: q.isActive,
    });
    setFormError('');
    setMode('edit');
    setEditingId(q.id);
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    if (!form.name.trim()) { setFormError(t('qualification.qual.validation.nameRequired')); return; }
    setSaving(true); setFormError('');
    try {
      const payload = {
        categoryId: form.categoryId,
        name: form.name,
        description: form.description || null,
        sortOrder: Number(form.sortOrder) || 0,
      };
      if (mode === 'create') {
        await createQualification(payload);
      } else {
        await updateQualification(editingId!, { ...payload, active: form.active });
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
        <label className="master-label" style={{ minWidth: 'auto' }}>{t('common.categoryFilterLabel')}</label>
        <select className="master-select"
          value={filterCatId ?? ''}
          onChange={e => setFilterCatId(e.target.value === '' ? null : Number(e.target.value))}>
          <option value="">{t('common.allOption')}</option>
          {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <button className="btn btn--primary btn--sm" onClick={openCreate} style={{ marginLeft: 'auto' }}>
          <IconPlus size={12} />{t('qualification.addButton')}
        </button>
      </div>

      <StickyHorizontalScroll className="master-table-wrap">
        <table className="master-table">
          <thead>
            <tr>
              <th style={{ width: 160 }}>{t('qualification.table.category')}</th>
              <th>{t('qualification.table.name')}</th>
              <th style={{ width: 200 }}>{t('qualification.table.description')}</th>
              <th style={{ width: 56 }}>{t('common.sortOrder')}</th>
              <th style={{ width: 72 }}>{t('common.status')}</th>
              <th style={{ width: 72 }}>{t('common.actions')}</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr><td colSpan={6} className="master-table__empty">{t('common.noData')}</td></tr>
            ) : (
              filtered.map(q => (
                <tr key={q.id}>
                  <td style={{ fontSize: 13, color: 'var(--color-text-muted)' }}>{q.categoryName ?? '—'}</td>
                  <td>{q.name}</td>
                  <td style={{ fontSize: 13, maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {q.description ?? '—'}
                  </td>
                  <td style={{ textAlign: 'center' }}>{q.sortOrder}</td>
                  <td>
                    <span className={q.isActive ? 'fy-status fy-status--active' : 'fy-status fy-status--inactive'}>
                      {q.isActive ? t('common.activeLabel') : t('common.inactiveLabel')}
                    </span>
                  </td>
                  <td>
                    <button className="btn btn--secondary btn--sm" onClick={() => openEdit(q)}>
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
              <h3>{mode === 'create' ? t('qualification.modalCreate') : t('qualification.modalEdit')}</h3>
              <button className="modal__close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal__body">
              {formError && <div className="alert alert--error">{formError}</div>}
              <div className="form-group">
                <label className="form-label">{t('qualification.form.categoryLabel')}</label>
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
                <label className="form-label">{t('qualification.form.nameLabel')} <span className="required">*</span></label>
                <input className="form-input" value={form.name}
                  onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  placeholder={t('qualification.qual.form.namePlaceholder')} maxLength={200} />
              </div>
              <div className="form-group">
                <label className="form-label">{t('qualification.form.descriptionLabel')}</label>
                <textarea className="form-input" value={form.description}
                  onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                  rows={3} placeholder={t('qualification.qual.form.descriptionPlaceholder')} style={{ resize: 'vertical' }} />
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

// ─── Promotion tab ────────────────────────────────────────────────────────────

interface PromoteForm {
  categoryId: number | null;
  name: string;
  description: string;
  sortOrder: string;
}

/**
 * 資格昇格タブ。
 *
 * ユーザーが自由入力したカスタム資格名を正式マスタとして登録（昇格）する。
 */
function QualPromotionTab({ categories, onQualReload }: {
  categories: QualificationCategory[];
  onQualReload: () => void;
}) {
  const { t } = useTranslation('master');
  const [items, setItems] = useState<CustomUnregisteredItem[]>([]);
  const [tabLoading, setTabLoading] = useState(true);
  const [tabError, setTabError] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [promoting, setPromoting] = useState<CustomUnregisteredItem | null>(null);
  const [form, setForm] = useState<PromoteForm>({ categoryId: null, name: '', description: '', sortOrder: '0' });
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const loadItems = () => {
    setTabLoading(true);
    getCustomUnregisteredQualifications()
      .then(res => setItems(res.data))
      .catch(() => setTabError(t('common.loadFetchFailed')))
      .finally(() => setTabLoading(false));
  };

  // 初期表示時に未登録カスタム資格一覧を取得する
  useEffect(() => { loadItems(); }, []);

  const openPromote = (item: CustomUnregisteredItem) => {
    setPromoting(item);
    setForm({ categoryId: null, name: item.customName, description: '', sortOrder: '0' });
    setFormError('');
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    if (!form.name.trim()) { setFormError(t('qualification.promotion.validation.nameRequired')); return; }
    if (!promoting) return;
    setSaving(true); setFormError('');
    try {
      await promoteQualification({
        customName: promoting.customName,
        categoryId: form.categoryId,
        name: form.name,
        description: form.description || null,
        sortOrder: Number(form.sortOrder) || 0,
      });
      setModalOpen(false);
      loadItems();
      onQualReload();
    } catch {
      setFormError(t('common.promoteFailed'));
    } finally { setSaving(false); }
  };

  if (tabLoading) return <div className="chart-loading">{t('loading')}</div>;
  if (tabError) return <div className="alert alert--error">{tabError}</div>;

  return (
    <>
      <div className="master-card__header" style={{ marginBottom: 16 }}>
        <h3 className="master-card__title" style={{ marginBottom: 0 }}>{t('qualification.promotion.tabTitle')}</h3>
        <span style={{ fontSize: 13, color: 'var(--color-text-muted)' }}>
          {t('qualification.promotion.hint')}
        </span>
      </div>

      {items.length === 0 ? (
        <div style={{ padding: '32px 0', textAlign: 'center', color: 'var(--color-text-muted)', fontSize: 14 }}>
          {t('qualification.promotion.emptyMessage')}
        </div>
      ) : (
        <StickyHorizontalScroll className="master-table-wrap">
          <table className="master-table">
            <thead>
              <tr>
                <th>{t('qualification.promotion.table.customName')}</th>
                <th style={{ width: 110 }}>{t('qualification.table.usageCount')}</th>
                <th style={{ width: 80 }}>{t('common.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {items.map(item => (
                <tr key={item.customName}>
                  <td>{item.customName}</td>
                  <td style={{ textAlign: 'center' }}>{item.usageCount}{t('common.usageCountSuffix')}</td>
                  <td>
                    <button className="btn btn--primary btn--sm" onClick={() => openPromote(item)}>
                      {t('common.promoteButton')}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </StickyHorizontalScroll>
      )}

      {modalOpen && createPortal(
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal__header">
              <h3>{t('qualification.modalPromote')}</h3>
              <button className="modal__close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal__body">
              {formError && <div className="alert alert--error">{formError}</div>}

              <div className="form-group">
                <label className="form-label">{t('qualification.promotion.form.originalNameLabel')}</label>
                <div style={{ padding: '6px 0', fontWeight: 500 }}>{promoting?.customName}</div>
              </div>

              <div className="form-group">
                <label className="form-label">{t('qualification.form.categoryLabel')}</label>
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
                <label className="form-label">{t('qualification.form.nameLabel')} <span className="required">*</span></label>
                <input className="form-input" value={form.name}
                  onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  maxLength={200} />
                <span className="form-hint">{t('qualification.promotion.form.masterNameHint')}</span>
              </div>

              <div className="form-group">
                <label className="form-label">{t('qualification.form.descriptionLabel')}</label>
                <textarea className="form-input" value={form.description}
                  onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                  rows={3} style={{ resize: 'vertical' }} />
              </div>

              <div className="form-group">
                <label className="form-label">{t('common.sortOrder')}</label>
                <input type="number" className="form-input" value={form.sortOrder}
                  onChange={e => setForm(f => ({ ...f, sortOrder: e.target.value }))}
                  style={{ width: 100 }} />
              </div>
            </div>
            <div className="modal__footer">
              <button className="btn btn--secondary" onClick={() => setModalOpen(false)}>
                <IconX size={13} />{t('common.cancel')}
              </button>
              <button className="btn btn--primary" onClick={handleSubmit} disabled={saving}>
                <IconCheck size={13} />{saving ? t('common.registeringButton') : t('common.registerButton')}
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

type TabKey = 'categories' | 'qualifications' | 'promote';

/**
 * 資格マスタ管理ページ。
 *
 * 資格カテゴリ・資格・昇格の3タブで構成される資格マスタ管理画面。
 * Excel インポート・エクスポートもサポートする。ADMIN ロールのみアクセス可能。
 */
export default function QualificationMasterPage() {
  const { t } = useTranslation('master');
  const [activeTab, setActiveTab] = useState<TabKey>('categories');
  const [categories, setCategories] = useState<QualificationCategory[]>([]);
  const [qualifications, setQualifications] = useState<Qualification[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [downloading, setDownloading] = useState(false);
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState<MasterImportResult | null>(null);
  const [importErrors, setImportErrors] = useState<MasterImportError[] | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const loadAll = () => {
    setLoading(true);
    return Promise.all([getQualificationCategories(), getQualifications()])
      .then(([catRes, qualRes]) => {
        setCategories(catRes.data);
        setQualifications(qualRes.data);
      })
      .catch(() => setError(t('common.loadFailed')))
      .finally(() => setLoading(false));
  };

  // 初期表示時に資格カテゴリと資格一覧を取得する
  useEffect(() => { loadAll(); }, []);

  const handleDownload = async () => {
    setDownloading(true);
    try {
      const res = await downloadQualificationExcel();
      const url = URL.createObjectURL(res.data);
      const a = document.createElement('a');
      a.href = url; a.download = 'QualificationMaster.xlsx'; a.click();
      URL.revokeObjectURL(url);
    } catch {
      setError(t('excel.downloadFailed'));
    } finally { setDownloading(false); }
  };

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    e.target.value = '';
    setImporting(true);
    try {
      const res = await uploadQualificationExcel(file);
      await loadAll();
      setImportResult(res.data);
    } catch (err: unknown) {
      const apiErrors = (err as { response?: { data?: { errors?: MasterImportError[] } } })
        ?.response?.data?.errors;
      if (apiErrors) {
        setImportErrors(apiErrors);
      } else {
        const message = (err as { response?: { data?: { message?: string } } })
          ?.response?.data?.message ?? t('excel.uploadFailed');
        setImportErrors([{ sheet: '', row: 0, column: '', message }]);
      }
    } finally { setImporting(false); }
  };

  if (loading) return <div className="loading-screen"><span>{t('loading')}</span></div>;

  return (
    <div className="master-page">
      <NavBar />

      <main className="master-main">
        {error && <div className="alert alert--error">{error}</div>}

        <div className="tab-bar">
          <button className={`tab-btn${activeTab === 'categories' ? ' active' : ''}`}
            onClick={() => setActiveTab('categories')}>
            {t('qualification.tab.categories')}
          </button>
          <button className={`tab-btn${activeTab === 'qualifications' ? ' active' : ''}`}
            onClick={() => setActiveTab('qualifications')}>
            {t('qualification.tab.qualifications')}
          </button>
          <button className={`tab-btn${activeTab === 'promote' ? ' active' : ''}`}
            onClick={() => setActiveTab('promote')}>
            {t('qualification.tab.promote')}
          </button>
        </div>

        <section className="master-card">
          {activeTab === 'categories' ? (
            <CategoryTab categories={categories} onReload={loadAll} />
          ) : activeTab === 'qualifications' ? (
            <QualificationTab qualifications={qualifications} categories={categories} onReload={loadAll} />
          ) : (
            <QualPromotionTab categories={categories} onQualReload={loadAll} />
          )}
        </section>
      </main>

      <div className="excel-fab">
        <button className="excel-fab__btn" onClick={handleDownload} disabled={downloading}>
          {downloading ? t('excel.downloading') : t('excel.download')}
        </button>
        <button className="excel-fab__btn" onClick={() => fileInputRef.current?.click()}
          disabled={importing}>
          {importing ? t('excel.importing') : t('excel.upload')}
        </button>
        <input ref={fileInputRef} type="file" accept=".xlsx"
          aria-label={t('excel.upload')}
          style={{ position: 'absolute', width: 1, height: 1, opacity: 0, overflow: 'hidden' }}
          onChange={handleUpload} />
      </div>

      {importResult !== null && createPortal(
        <div className="modal-overlay" onClick={() => setImportResult(null)}>
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 360 }}>
            <div className="modal__header">
              <h3>{t('excel.importResultTitle')}</h3>
              <button className="modal__close" onClick={() => setImportResult(null)}>×</button>
            </div>
            <div className="modal__body">
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 15 }}>
                <tbody>
                  {[
                    { label: t('excel.importResultCreated'), value: importResult.created },
                    { label: t('excel.importResultUpdated'), value: importResult.updated },
                    { label: t('excel.importResultDeleted'), value: importResult.deleted },
                  ].map(row => (
                    <tr key={row.label} style={{ borderBottom: '1px solid var(--color-border)' }}>
                      <td style={{ padding: '10px 8px', color: 'var(--color-text-muted)' }}>{row.label}</td>
                      <td style={{ padding: '10px 8px', textAlign: 'right', fontWeight: 700, fontSize: 18 }}>
                        {row.value}<span style={{ fontSize: 13, fontWeight: 400, marginLeft: 2 }}>{t('excel.importResultUnit')}</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="modal__footer">
              <button className="btn btn--primary" onClick={() => setImportResult(null)}>
                {t('excel.importResultOk')}
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}

      {importErrors !== null && createPortal(
        <div className="modal-overlay" onClick={() => setImportErrors(null)}>
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 640 }}>
            <div className="modal__header">
              <h3>{t('excel.importError')}</h3>
              <button className="modal__close" onClick={() => setImportErrors(null)}>×</button>
            </div>
            <div className="modal__body" style={{ maxHeight: 400, overflowY: 'auto' }}>
              {importErrors.length === 0
                ? <p>{t('common.loadFailed')}</p>
                : importErrors.map((e, i) => (
                  <p key={i} style={{ fontSize: 13, margin: '4px 0', color: 'var(--color-danger)' }}>
                    {e.sheet && e.row ? t('excel.importErrorDetail', { sheet: e.sheet, row: e.row, column: e.column, message: e.message })
                      : e.message}
                  </p>
                ))}
            </div>
            <div className="modal__footer">
              <button className="btn btn--secondary" onClick={() => setImportErrors(null)}>
                {t('excel.importErrorClose')}
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}
    </div>
  );
}
