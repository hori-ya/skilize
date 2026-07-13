/*******************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * ADセミナーマスタ管理ページ。ADセミナーカテゴリと ADセミナーをタブ切り替えで管理する。
 * Excel インポート・エクスポート機能も提供する。ADMIN ロールのみアクセス可能。
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
import type { AdSeminar, AdSeminarCategory, MasterImportError, MasterImportResult } from '../../../shared/types/master';
import {
  getAdSeminars, createAdSeminar, updateAdSeminar,
  getAdSeminarCategories, createAdSeminarCategory, updateAdSeminarCategory,
  downloadAdSeminarExcel, uploadAdSeminarExcel,
} from '../../../shared/api/masterApi';
import { IconPlus, IconEdit, IconX, IconCheck } from '../../../shared/ui/Icons';
import StickyHorizontalScroll from '../../../shared/ui/StickyHorizontalScroll';

type ModalMode = 'create' | 'edit';

// ─── Category tab ─────────────────────────────────────────────────────────────

interface CatForm { name: string; sortOrder: string; active: boolean }

/**
 * ADセミナーカテゴリ管理タブ。
 *
 * ADセミナーカテゴリの一覧表示・新規作成・編集を行う。
 */
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

  let tableBody: React.ReactNode;
  if (categories.length === 0) {
    tableBody = <tr><td colSpan={4} className="master-table__empty">{t('common.noData')}</td></tr>;
  } else {
    const rows: React.ReactNode[] = [];
    for (const c of categories) {
      let statusClassName = 'fy-status fy-status--inactive';
      let statusLabel = t('common.inactiveLabel');
      if (c.isActive) {
        statusClassName = 'fy-status fy-status--active';
        statusLabel = t('common.activeLabel');
      }
      rows.push(
        <tr key={c.id}>
          <td>{c.name}</td>
          <td style={{ textAlign: 'center' }}>{c.sortOrder}</td>
          <td>
            <span className={statusClassName}>
              {statusLabel}
            </span>
          </td>
          <td>
            <button className="btn btn--secondary btn--sm" onClick={() => openEdit(c)}>
              <IconEdit size={12} />{t('common.edit')}
            </button>
          </td>
        </tr>,
      );
    }
    tableBody = rows;
  }

  let modalTitle = t('adSeminar.modalCategoryCreate');
  if (mode === 'edit') {
    modalTitle = t('adSeminar.modalCategoryEdit');
  }

  let saveButtonLabel = t('common.save');
  if (saving) {
    saveButtonLabel = t('common.saving');
  }

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
            {tableBody}
          </tbody>
        </table>
      </StickyHorizontalScroll>

      {modalOpen && createPortal(
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal__header">
              <h3>{modalTitle}</h3>
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
                <IconCheck size={13} />{saveButtonLabel}
              </button>
            </div>
          </div>
        </div>,
        document.body,
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

/**
 * ADセミナー管理タブ。
 *
 * ADセミナーの一覧表示・カテゴリフィルタ・新規作成・編集を行う。
 */
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

  let filtered: AdSeminar[] = adSeminars;
  if (filterCatId) {
    filtered = [];
    for (const a of adSeminars) {
      if (a.categoryId === filterCatId) {
        filtered.push(a);
      }
    }
  }

  const openCreate = () => {
    setForm({ categoryId: null, name: '', description: '', sortOrder: '0', active: true });
    setFormError('');
    setMode('create');
    setEditingId(null);
    setModalOpen(true);
  };

  const openEdit = (a: AdSeminar) => {
    let description = '';
    if (a.description != null) {
      description = a.description;
    }
    setForm({
      categoryId: a.categoryId,
      name: a.name,
      description,
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

  let filterCatValue: number | string = '';
  if (filterCatId != null) {
    filterCatValue = filterCatId;
  }

  const filterCategoryOptions: React.ReactNode[] = [];
  for (const c of categories) {
    filterCategoryOptions.push(<option key={c.id} value={c.id}>{c.name}</option>);
  }

  let tableBody: React.ReactNode;
  if (filtered.length === 0) {
    tableBody = <tr><td colSpan={6} className="master-table__empty">{t('common.noData')}</td></tr>;
  } else {
    const rows: React.ReactNode[] = [];
    for (const a of filtered) {
      let categoryNameLabel = '—';
      if (a.categoryName != null) {
        categoryNameLabel = a.categoryName;
      }
      let descriptionLabel = '—';
      if (a.description != null) {
        descriptionLabel = a.description;
      }
      let statusClassName = 'fy-status fy-status--inactive';
      let statusLabel = t('common.inactiveLabel');
      if (a.isActive) {
        statusClassName = 'fy-status fy-status--active';
        statusLabel = t('common.activeLabel');
      }
      rows.push(
        <tr key={a.id}>
          <td style={{ fontSize: 13, color: 'var(--color-text-muted)' }}>{categoryNameLabel}</td>
          <td>{a.name}</td>
          <td style={{ fontSize: 13, maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {descriptionLabel}
          </td>
          <td style={{ textAlign: 'center' }}>{a.sortOrder}</td>
          <td>
            <span className={statusClassName}>
              {statusLabel}
            </span>
          </td>
          <td>
            <button className="btn btn--secondary btn--sm" onClick={() => openEdit(a)}>
              <IconEdit size={12} />{t('common.edit')}
            </button>
          </td>
        </tr>,
      );
    }
    tableBody = rows;
  }

  let modalTitle = t('adSeminar.modalCreate');
  if (mode === 'edit') {
    modalTitle = t('adSeminar.modalEdit');
  }

  let formCategoryValue: number | string = '';
  if (form.categoryId != null) {
    formCategoryValue = form.categoryId;
  }

  const formCategoryOptions: React.ReactNode[] = [];
  for (const c of categories) {
    if (c.isActive) {
      formCategoryOptions.push(<option key={c.id} value={c.id}>{c.name}</option>);
    }
  }

  let saveButtonLabel = t('common.save');
  if (saving) {
    saveButtonLabel = t('common.saving');
  }

  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
        <label className="master-label" style={{ minWidth: 'auto' }}>{t('adSeminar.seminar.filterLabel')}</label>
        <select className="master-select"
          value={filterCatValue}
          onChange={e => {
            if (e.target.value === '') {
              setFilterCatId(null);
            } else {
              setFilterCatId(Number(e.target.value));
            }
          }}>
          <option value="">{t('common.allOption')}</option>
          {filterCategoryOptions}
        </select>
        <button className="btn btn--primary btn--sm" onClick={openCreate} style={{ marginLeft: 'auto' }}>
          <IconPlus size={12} />{t('adSeminar.seminar.addButton')}
        </button>
      </div>

      <StickyHorizontalScroll className="master-table-wrap">
        <table className="master-table">
          <thead>
            <tr>
              <th style={{ width: 160 }}>{t('adSeminar.table.category')}</th>
              <th>{t('adSeminar.seminar.table.adName')}</th>
              <th style={{ width: 200 }}>{t('adSeminar.table.description')}</th>
              <th style={{ width: 56 }}>{t('common.sortOrder')}</th>
              <th style={{ width: 72 }}>{t('common.status')}</th>
              <th style={{ width: 72 }}>{t('common.actions')}</th>
            </tr>
          </thead>
          <tbody>
            {tableBody}
          </tbody>
        </table>
      </StickyHorizontalScroll>

      {modalOpen && createPortal(
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal__header">
              <h3>{modalTitle}</h3>
              <button className="modal__close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal__body">
              {formError && <div className="alert alert--error">{formError}</div>}
              <div className="form-group">
                <label className="form-label">{t('adSeminar.form.categoryLabel')}</label>
                <select className="master-select" style={{ width: '100%' }}
                  value={formCategoryValue}
                  onChange={e => {
                    if (e.target.value === '') {
                      setForm(f => ({ ...f, categoryId: null }));
                    } else {
                      setForm(f => ({ ...f, categoryId: Number(e.target.value) }));
                    }
                  }}>
                  <option value="">{t('common.noCategoryOption')}</option>
                  {formCategoryOptions}
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
                <IconCheck size={13} />{saveButtonLabel}
              </button>
            </div>
          </div>
        </div>,
        document.body,
      )}
    </>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

type TabKey = 'categories' | 'adSeminars';

/**
 * ADセミナーマスタ管理ページ。
 *
 * ADセミナーカテゴリと ADセミナーの2タブで構成されるマスタ管理画面。
 * Excel インポート・エクスポートもサポートする。ADMIN ロールのみアクセス可能。
 */
export default function AdSeminarMasterPage() {
  const { t } = useTranslation('master');
  const [activeTab, setActiveTab] = useState<TabKey>('categories');
  const [categories, setCategories] = useState<AdSeminarCategory[]>([]);
  const [adSeminars, setAdSeminars] = useState<AdSeminar[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [downloading, setDownloading] = useState(false);
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState<MasterImportResult | null>(null);
  const [importErrors, setImportErrors] = useState<MasterImportError[] | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const loadAll = () => {
    setLoading(true);
    return Promise.all([getAdSeminarCategories(), getAdSeminars()])
      .then(([catRes, adRes]) => {
        setCategories(catRes.data);
        setAdSeminars(adRes.data);
      })
      .catch(() => setError(t('common.loadFailed')))
      .finally(() => setLoading(false));
  };

  // 初期表示時にADセミナーカテゴリとADセミナー一覧を取得する
  useEffect(() => { loadAll(); }, []);

  const handleDownload = async () => {
    setDownloading(true);
    try {
      const res = await downloadAdSeminarExcel();
      const url = URL.createObjectURL(res.data);
      const a = document.createElement('a');
      a.href = url; a.download = 'AdSeminarMaster.xlsx'; a.click();
      URL.revokeObjectURL(url);
    } catch {
      setError(t('excel.downloadFailed'));
    } finally { setDownloading(false); }
  };

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    let file: File | null = null;
    if (files != null && files.length > 0) {
      file = files[0];
    }
    if (!file) return;
    e.target.value = '';
    setImporting(true);
    try {
      const res = await uploadAdSeminarExcel(file);
      await loadAll();
      setImportResult(res.data);
    } catch (err: unknown) {
      const typedErr = err as { response?: { data?: { errors?: MasterImportError[]; message?: string } } };
      let apiErrors: MasterImportError[] | undefined;
      if (typedErr.response != null && typedErr.response.data != null) {
        apiErrors = typedErr.response.data.errors;
      }
      if (apiErrors) {
        setImportErrors(apiErrors);
      } else {
        let message = t('excel.uploadFailed');
        if (typedErr.response != null && typedErr.response.data != null && typedErr.response.data.message != null) {
          message = typedErr.response.data.message;
        }
        setImportErrors([{ sheet: '', row: 0, column: '', message }]);
      }
    } finally { setImporting(false); }
  };

  if (loading) return <div className="loading-screen"><span>{t('loading')}</span></div>;

  let categoriesTabClassName = 'tab-btn';
  if (activeTab === 'categories') {
    categoriesTabClassName += ' active';
  }
  let adSeminarsTabClassName = 'tab-btn';
  if (activeTab === 'adSeminars') {
    adSeminarsTabClassName += ' active';
  }

  let tabContent: React.ReactNode;
  if (activeTab === 'categories') {
    tabContent = <CategoryTab categories={categories} onReload={loadAll} />;
  } else {
    tabContent = <AdSeminarTab adSeminars={adSeminars} categories={categories} onReload={loadAll} />;
  }

  let downloadButtonLabel = t('excel.download');
  if (downloading) {
    downloadButtonLabel = t('excel.downloading');
  }

  let uploadButtonLabel = t('excel.upload');
  if (importing) {
    uploadButtonLabel = t('excel.importing');
  }

  let importResultBody: React.ReactNode = null;
  if (importResult !== null) {
    const importResultRows = [
      { label: t('excel.importResultCreated'), value: importResult.created },
      { label: t('excel.importResultUpdated'), value: importResult.updated },
      { label: t('excel.importResultDeleted'), value: importResult.deleted },
    ];
    const resultRowElements: React.ReactNode[] = [];
    for (const row of importResultRows) {
      resultRowElements.push(
        <tr key={row.label} style={{ borderBottom: '1px solid var(--color-border)' }}>
          <td style={{ padding: '10px 8px', color: 'var(--color-text-muted)' }}>{row.label}</td>
          <td style={{ padding: '10px 8px', textAlign: 'right', fontWeight: 700, fontSize: 18 }}>
            {row.value}<span style={{ fontSize: 13, fontWeight: 400, marginLeft: 2 }}>{t('excel.importResultUnit')}</span>
          </td>
        </tr>,
      );
    }
    importResultBody = (
      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 15 }}>
        <tbody>
          {resultRowElements}
        </tbody>
      </table>
    );
  }

  let importErrorBody: React.ReactNode = null;
  if (importErrors !== null) {
    if (importErrors.length === 0) {
      importErrorBody = <p>{t('common.loadFailed')}</p>;
    } else {
      const errorRowElements: React.ReactNode[] = [];
      for (let i = 0; i < importErrors.length; i++) {
        const e = importErrors[i];
        let errorText: React.ReactNode = e.message;
        if (e.sheet && e.row) {
          errorText = t('excel.importErrorDetail', { sheet: e.sheet, row: e.row, column: e.column, message: e.message });
        }
        errorRowElements.push(
          <p key={i} style={{ fontSize: 13, margin: '4px 0', color: 'var(--color-danger)' }}>
            {errorText}
          </p>,
        );
      }
      importErrorBody = errorRowElements;
    }
  }

  return (
    <div className="master-page">
      <NavBar />

      <main className="master-main">
        {error && <div className="alert alert--error">{error}</div>}

        <div className="tab-bar">
          <button className={categoriesTabClassName}
            onClick={() => setActiveTab('categories')}>
            {t('adSeminar.tab.categories')}
          </button>
          <button className={adSeminarsTabClassName}
            onClick={() => setActiveTab('adSeminars')}>
            {t('adSeminar.tab.adSeminars')}
          </button>
        </div>

        <section className="master-card">
          {tabContent}
        </section>
      </main>

      <div className="excel-fab">
        <button className="excel-fab__btn" onClick={handleDownload} disabled={downloading}>
          {downloadButtonLabel}
        </button>
        <button className="excel-fab__btn" onClick={() => {
          if (fileInputRef.current != null) {
            fileInputRef.current.click();
          }
        }}
          disabled={importing}>
          {uploadButtonLabel}
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
              {importResultBody}
            </div>
            <div className="modal__footer">
              <button className="btn btn--primary" onClick={() => setImportResult(null)}>
                {t('excel.importResultOk')}
              </button>
            </div>
          </div>
        </div>,
        document.body,
      )}

      {importErrors !== null && createPortal(
        <div className="modal-overlay" onClick={() => setImportErrors(null)}>
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 640 }}>
            <div className="modal__header">
              <h3>{t('excel.importError')}</h3>
              <button className="modal__close" onClick={() => setImportErrors(null)}>×</button>
            </div>
            <div className="modal__body" style={{ maxHeight: 400, overflowY: 'auto' }}>
              {importErrorBody}
            </div>
            <div className="modal__footer">
              <button className="btn btn--secondary" onClick={() => setImportErrors(null)}>
                {t('excel.importErrorClose')}
              </button>
            </div>
          </div>
        </div>,
        document.body,
      )}
    </div>
  );
}
