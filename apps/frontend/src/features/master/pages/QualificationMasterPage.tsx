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

  let modalTitle = t('qualification.modalCategoryCreate');
  if (mode === 'edit') {
    modalTitle = t('qualification.modalCategoryEdit');
  }

  let saveButtonLabel = t('common.save');
  if (saving) {
    saveButtonLabel = t('common.saving');
  }

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

  let filtered: Qualification[] = qualifications;
  if (filterCatId) {
    filtered = [];
    for (const q of qualifications) {
      if (q.categoryId === filterCatId) {
        filtered.push(q);
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

  const openEdit = (q: Qualification) => {
    let description = '';
    if (q.description != null) {
      description = q.description;
    }
    setForm({
      categoryId: q.categoryId,
      name: q.name,
      description,
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
    for (const q of filtered) {
      let categoryNameLabel = '—';
      if (q.categoryName != null) {
        categoryNameLabel = q.categoryName;
      }
      let descriptionLabel = '—';
      if (q.description != null) {
        descriptionLabel = q.description;
      }
      let statusClassName = 'fy-status fy-status--inactive';
      let statusLabel = t('common.inactiveLabel');
      if (q.isActive) {
        statusClassName = 'fy-status fy-status--active';
        statusLabel = t('common.activeLabel');
      }
      rows.push(
        <tr key={q.id}>
          <td style={{ fontSize: 13, color: 'var(--color-text-muted)' }}>{categoryNameLabel}</td>
          <td>{q.name}</td>
          <td style={{ fontSize: 13, maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {descriptionLabel}
          </td>
          <td style={{ textAlign: 'center' }}>{q.sortOrder}</td>
          <td>
            <span className={statusClassName}>
              {statusLabel}
            </span>
          </td>
          <td>
            <button className="btn btn--secondary btn--sm" onClick={() => openEdit(q)}>
              <IconEdit size={12} />{t('common.edit')}
            </button>
          </td>
        </tr>,
      );
    }
    tableBody = rows;
  }

  let modalTitle = t('qualification.modalCreate');
  if (mode === 'edit') {
    modalTitle = t('qualification.modalEdit');
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
        <label className="master-label" style={{ minWidth: 'auto' }}>{t('common.categoryFilterLabel')}</label>
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
                <label className="form-label">{t('qualification.form.categoryLabel')}</label>
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

  let listBody: React.ReactNode;
  if (items.length === 0) {
    listBody = (
      <div style={{ padding: '32px 0', textAlign: 'center', color: 'var(--color-text-muted)', fontSize: 14 }}>
        {t('qualification.promotion.emptyMessage')}
      </div>
    );
  } else {
    const rows: React.ReactNode[] = [];
    for (const item of items) {
      rows.push(
        <tr key={item.customName}>
          <td>{item.customName}</td>
          <td style={{ textAlign: 'center' }}>{item.usageCount}{t('common.usageCountSuffix')}</td>
          <td>
            <button className="btn btn--primary btn--sm" onClick={() => openPromote(item)}>
              {t('common.promoteButton')}
            </button>
          </td>
        </tr>,
      );
    }
    listBody = (
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
            {rows}
          </tbody>
        </table>
      </StickyHorizontalScroll>
    );
  }

  let originalNameLabel = '';
  if (promoting != null) {
    originalNameLabel = promoting.customName;
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

  let submitButtonLabel = t('common.registerButton');
  if (saving) {
    submitButtonLabel = t('common.registeringButton');
  }

  return (
    <>
      <div className="master-card__header" style={{ marginBottom: 16 }}>
        <h3 className="master-card__title" style={{ marginBottom: 0 }}>{t('qualification.promotion.tabTitle')}</h3>
        <span style={{ fontSize: 13, color: 'var(--color-text-muted)' }}>
          {t('qualification.promotion.hint')}
        </span>
      </div>

      {listBody}

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
                <div style={{ padding: '6px 0', fontWeight: 500 }}>{originalNameLabel}</div>
              </div>

              <div className="form-group">
                <label className="form-label">{t('qualification.form.categoryLabel')}</label>
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
                <IconCheck size={13} />{submitButtonLabel}
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
    const files = e.target.files;
    let file: File | null = null;
    if (files != null && files.length > 0) {
      file = files[0];
    }
    if (!file) return;
    e.target.value = '';
    setImporting(true);
    try {
      const res = await uploadQualificationExcel(file);
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
  let qualificationsTabClassName = 'tab-btn';
  if (activeTab === 'qualifications') {
    qualificationsTabClassName += ' active';
  }
  let promoteTabClassName = 'tab-btn';
  if (activeTab === 'promote') {
    promoteTabClassName += ' active';
  }

  let tabContent: React.ReactNode;
  if (activeTab === 'categories') {
    tabContent = <CategoryTab categories={categories} onReload={loadAll} />;
  } else if (activeTab === 'qualifications') {
    tabContent = <QualificationTab qualifications={qualifications} categories={categories} onReload={loadAll} />;
  } else {
    tabContent = <QualPromotionTab categories={categories} onQualReload={loadAll} />;
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
            {t('qualification.tab.categories')}
          </button>
          <button className={qualificationsTabClassName}
            onClick={() => setActiveTab('qualifications')}>
            {t('qualification.tab.qualifications')}
          </button>
          <button className={promoteTabClassName}
            onClick={() => setActiveTab('promote')}>
            {t('qualification.tab.promote')}
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
