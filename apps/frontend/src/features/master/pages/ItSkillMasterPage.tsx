/*******************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * ITスキルマスタ管理ページ。ITスキルカテゴリ・ITスキル・未登録カスタムスキルの昇格を
 * タブ切り替えで管理する。Excel インポート・エクスポート機能も提供する。
 * ADMIN ロールのみアクセス可能。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import { useEffect, useRef, useState, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import NavBar from '../../../app/layouts/NavBar';
import type { ItSkillCategory, ItSkill, CustomUnregisteredItem, MasterImportError, MasterImportResult } from '../../../shared/types/master';
import {
  getItSkillCategories, createItSkillCategory, updateItSkillCategory,
  getItSkills, createItSkill, updateItSkill,
  getCustomUnregisteredItSkills, promoteItSkill,
  downloadItSkillExcel, uploadItSkillExcel,
} from '../../../shared/api/masterApi';
import { IconPlus, IconEdit, IconX, IconCheck } from '../../../shared/ui/Icons';
import StickyHorizontalScroll from '../../../shared/ui/StickyHorizontalScroll';

/** 指定した階層のカテゴリのみを抽出する。 */
function filterByLevel(categories: ItSkillCategory[], level: number): ItSkillCategory[] {
  const result: ItSkillCategory[] = [];
  for (const c of categories) {
    if (c.level === level) {
      result.push(c);
    }
  }
  return result;
}

/** 指定した階層・親カテゴリに属するカテゴリのみを抽出する。 */
function filterByLevelAndParent(categories: ItSkillCategory[], level: number, parentId: number | null): ItSkillCategory[] {
  const result: ItSkillCategory[] = [];
  for (const c of categories) {
    if (c.level === level && c.parentId === parentId) {
      result.push(c);
    }
  }
  return result;
}

/** 指定した階層の有効なカテゴリのみを抽出する。 */
function filterActiveByLevel(categories: ItSkillCategory[], level: number): ItSkillCategory[] {
  const result: ItSkillCategory[] = [];
  for (const c of categories) {
    if (c.level === level && c.isActive) {
      result.push(c);
    }
  }
  return result;
}

/** 指定した階層・親カテゴリに属する有効なカテゴリのみを抽出する。 */
function filterActiveByLevelAndParent(categories: ItSkillCategory[], level: number, parentId: number | null): ItSkillCategory[] {
  const result: ItSkillCategory[] = [];
  for (const c of categories) {
    if (c.level === level && c.parentId === parentId && c.isActive) {
      result.push(c);
    }
  }
  return result;
}

/** カテゴリID → カテゴリの Map を構築する。 */
function buildCategoryMap(categories: ItSkillCategory[]): Map<number, ItSkillCategory> {
  const map = new Map<number, ItSkillCategory>();
  for (const c of categories) {
    map.set(c.id, c);
  }
  return map;
}

/** カテゴリ選択肢の option 要素群を構築する。 */
function buildCategoryOptions(cats: ItSkillCategory[]): React.ReactNode[] {
  const options: React.ReactNode[] = [];
  for (const c of cats) {
    options.push(<option key={c.id} value={c.id}>{c.name}</option>);
  }
  return options;
}

/** Lv1/Lv2/Lv3 の選択状態から実際に登録対象となるカテゴリIDを解決する（最も深い階層を優先）。 */
function resolveSelectedCategoryId(form: { lv1Id: number | null; lv2Id: number | null; lv3Id: number | null }): number | null {
  if (form.lv3Id != null) return form.lv3Id;
  if (form.lv2Id != null) return form.lv2Id;
  return form.lv1Id;
}

// ─── Category tab ────────────────────────────────────────────────────────────

interface CatForm {
  parentId: number | null;
  name: string;
  sortOrder: string;
  active: boolean;
}

type CatModalMode = 'create' | 'edit';

const LEVEL_BADGE_CLASS = ['', 'cat-lv cat-lv--1', 'cat-lv cat-lv--2', 'cat-lv cat-lv--3'];

/**
 * ITスキルカテゴリ管理タブ。
 *
 * カテゴリ（Lv1/Lv2/Lv3）の一覧表示・新規作成・編集を行う。
 */
function CategoryTab({ categories, onReload }: { categories: ItSkillCategory[]; onReload: () => void }) {
  const { t } = useTranslation('master');
  const [modalOpen, setModalOpen] = useState(false);
  const [mode, setMode] = useState<CatModalMode>('create');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<CatForm>({ parentId: null, name: '', sortOrder: '0', active: true });
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const catMap = useMemo(() => buildCategoryMap(categories), [categories]);
  const lv1 = filterByLevel(categories, 1);
  const lv2 = filterByLevel(categories, 2);

  const parentLabel = (c: ItSkillCategory): string => {
    if (c.level === 1) return '—';
    const parent = catMap.get(c.parentId!);
    if (parent != null) return parent.name;
    return `ID:${c.parentId}`;
  };

  const levelBadge = (level: number) => {
    let cls = '';
    if (LEVEL_BADGE_CLASS[level] != null) {
      cls = LEVEL_BADGE_CLASS[level];
    }
    return <span className={cls}>Lv{level}</span>;
  };

  const openCreate = () => {
    setForm({ parentId: null, name: '', sortOrder: '0', active: true });
    setFormError('');
    setMode('create');
    setEditingId(null);
    setModalOpen(true);
  };

  const openEdit = (c: ItSkillCategory) => {
    setForm({ parentId: c.parentId, name: c.name, sortOrder: String(c.sortOrder), active: c.isActive });
    setFormError('');
    setMode('edit');
    setEditingId(c.id);
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    if (!form.name.trim()) { setFormError(t('itSkill.category.validation.nameRequired')); return; }
    setSaving(true); setFormError('');
    try {
      if (mode === 'create') {
        await createItSkillCategory({ parentId: form.parentId, name: form.name, sortOrder: Number(form.sortOrder) || 0 });
      } else {
        await updateItSkillCategory(editingId!, { name: form.name, sortOrder: Number(form.sortOrder) || 0, active: form.active });
      }
      setModalOpen(false);
      onReload();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { detail?: string } } };
      let msg = t('common.saveFailed');
      if (err.response != null && err.response.data != null && err.response.data.detail != null) {
        msg = err.response.data.detail;
      }
      setFormError(msg);
    } finally { setSaving(false); }
  };

  let tableBody: React.ReactNode;
  if (categories.length === 0) {
    tableBody = <tr><td colSpan={6} className="master-table__empty">{t('common.noData')}</td></tr>;
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
          <td>{levelBadge(c.level)}</td>
          <td style={{ color: 'var(--color-text-muted)', fontSize: 13 }}>{parentLabel(c)}</td>
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

  let modalTitle = t('itSkill.modalCategoryCreate');
  if (mode === 'edit') {
    modalTitle = t('itSkill.modalCategoryEdit');
  }

  let parentValue: number | string = '';
  if (form.parentId != null) {
    parentValue = form.parentId;
  }

  let saveButtonLabel = t('common.save');
  if (saving) {
    saveButtonLabel = t('common.saving');
  }

  return (
    <>
      <div className="master-card__header" style={{ marginBottom: 16 }}>
        <h3 className="master-card__title" style={{ marginBottom: 0 }}>{t('itSkill.category.listTitle')}</h3>
        <button className="btn btn--primary btn--sm" onClick={openCreate}>
          <IconPlus size={12} />{t('itSkill.addCategoryButton')}
        </button>
      </div>

      <StickyHorizontalScroll className="master-table-wrap">
        <table className="master-table">
          <thead>
            <tr>
              <th style={{ width: 64 }}>{t('itSkill.category.table.level')}</th>
              <th>{t('itSkill.category.table.parent')}</th>
              <th>{t('itSkill.category.table.name')}</th>
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

              {mode === 'create' && (
                <div className="form-group">
                  <label className="form-label">{t('itSkill.form.parentCategoryLabel')}</label>
                  <select
                    className="master-select" style={{ width: '100%' }}
                    value={parentValue}
                    onChange={e => {
                      if (e.target.value === '') {
                        setForm(f => ({ ...f, parentId: null }));
                      } else {
                        setForm(f => ({ ...f, parentId: Number(e.target.value) }));
                      }
                    }}
                  >
                    <option value="">{t('itSkill.category.form.parentNone')}</option>
                    <optgroup label={t('itSkill.category.form.lv1Group')}>
                      {buildCategoryOptions(lv1)}
                    </optgroup>
                    <optgroup label={t('itSkill.category.form.lv2Group')}>
                      {buildCategoryOptions(lv2)}
                    </optgroup>
                  </select>
                  <span style={{ fontSize: 12, color: 'var(--color-text-muted)' }}>
                    {t('itSkill.category.form.parentHint')}
                  </span>
                </div>
              )}

              <div className="form-group">
                <label className="form-label">{t('itSkill.form.categoryNameLabel')} <span className="required">*</span></label>
                <input className="form-input" value={form.name}
                  onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  placeholder={t('itSkill.category.form.namePlaceholder')} maxLength={100} />
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

// ─── Skill tab ───────────────────────────────────────────────────────────────

interface SkillForm {
  lv1Id: number | null;
  lv2Id: number | null;
  lv3Id: number | null;
  name: string;
  description: string;
  sortOrder: string;
  active: boolean;
}

type SkillModalMode = 'create' | 'edit';

/**
 * ITスキル管理タブ。
 *
 * ITスキルの一覧表示・カテゴリフィルタ・新規作成・編集を行う。
 */
function SkillTab({ skills, categories, onReload }: {
  skills: ItSkill[];
  categories: ItSkillCategory[];
  onReload: () => void;
}) {
  const { t } = useTranslation('master');
  const [filterLv1, setFilterLv1] = useState<number | null>(null);
  const [filterLv2, setFilterLv2] = useState<number | null>(null);
  const [filterLv3, setFilterLv3] = useState<number | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [mode, setMode] = useState<SkillModalMode>('create');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<SkillForm>({ lv1Id: null, lv2Id: null, lv3Id: null, name: '', description: '', sortOrder: '0', active: true });
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const catMap = useMemo(() => buildCategoryMap(categories), [categories]);
  const lv1Cats = filterByLevel(categories, 1);
  const lv2Cats = (parentId: number | null) => filterByLevelAndParent(categories, 2, parentId);
  const lv3Cats = (parentId: number | null) => filterByLevelAndParent(categories, 3, parentId);

  const skillCategoryPath = (skill: ItSkill): string => {
    const parts: string[] = [];
    if (skill.category1Name) parts.push(skill.category1Name);
    if (skill.category2Name) parts.push(skill.category2Name);
    if (skill.category3Name) parts.push(skill.category3Name);
    if (parts.length === 0) return `カテゴリID:${skill.categoryId}`;
    return parts.join(' › ');
  };

  const filteredSkills = useMemo(() => {
    const lv3IdsUnderLv2 = new Set<number>();
    if (filterLv2) {
      lv3IdsUnderLv2.add(filterLv2);
      for (const c of filterByLevelAndParent(categories, 3, filterLv2)) {
        lv3IdsUnderLv2.add(c.id);
      }
    }
    const result: ItSkill[] = [];
    for (const s of skills) {
      if (filterLv1 && s.category1Id !== filterLv1) continue;
      if (filterLv2 && !lv3IdsUnderLv2.has(s.categoryId)) continue;
      if (filterLv3 && s.categoryId !== filterLv3) continue;
      result.push(s);
    }
    return result;
  }, [skills, filterLv1, filterLv2, filterLv3, categories]);

  const openCreate = () => {
    setForm({ lv1Id: null, lv2Id: null, lv3Id: null, name: '', description: '', sortOrder: '0', active: true });
    setFormError('');
    setMode('create');
    setEditingId(null);
    setModalOpen(true);
  };

  const openEdit = (s: ItSkill) => {
    const cat = catMap.get(s.categoryId);
    let lv1Id: number | null = null, lv2Id: number | null = null, lv3Id: number | null = null;
    if (cat) {
      if (cat.level === 1) {
        lv1Id = cat.id;
      } else if (cat.level === 2) {
        lv2Id = cat.id;
        lv1Id = cat.parentId;
      } else if (cat.level === 3) {
        lv3Id = cat.id;
        const lv2 = catMap.get(cat.parentId!);
        if (lv2) {
          lv2Id = lv2.id;
          lv1Id = lv2.parentId;
        }
      }
    }
    let description = '';
    if (s.description != null) {
      description = s.description;
    }
    setForm({ lv1Id, lv2Id, lv3Id, name: s.name, description, sortOrder: String(s.sortOrder), active: s.isActive });
    setFormError('');
    setMode('edit');
    setEditingId(s.id);
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    const catId = resolveSelectedCategoryId(form);
    if (!catId) { setFormError(t('itSkill.skill.validation.categoryRequired')); return; }
    if (!form.name.trim()) { setFormError(t('itSkill.skill.validation.nameRequired')); return; }
    setSaving(true); setFormError('');
    try {
      const payload = {
        categoryId: catId,
        name: form.name,
        description: form.description || null,
        sortOrder: Number(form.sortOrder) || 0,
      };
      if (mode === 'create') {
        await createItSkill(payload);
      } else {
        await updateItSkill(editingId!, { ...payload, active: form.active });
      }
      setModalOpen(false);
      onReload();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { detail?: string } } };
      let msg = t('common.saveFailed');
      if (err.response != null && err.response.data != null && err.response.data.detail != null) {
        msg = err.response.data.detail;
      }
      setFormError(msg);
    } finally { setSaving(false); }
  };

  let filterLv1Value: number | string = '';
  if (filterLv1 != null) {
    filterLv1Value = filterLv1;
  }
  let filterLv2Value: number | string = '';
  if (filterLv2 != null) {
    filterLv2Value = filterLv2;
  }
  let filterLv3Value: number | string = '';
  if (filterLv3 != null) {
    filterLv3Value = filterLv3;
  }

  let tableBody: React.ReactNode;
  if (filteredSkills.length === 0) {
    tableBody = <tr><td colSpan={6} className="master-table__empty">{t('common.noData')}</td></tr>;
  } else {
    const rows: React.ReactNode[] = [];
    for (const s of filteredSkills) {
      let descriptionLabel = '—';
      if (s.description != null) {
        descriptionLabel = s.description;
      }
      let statusClassName = 'fy-status fy-status--inactive';
      let statusLabel = t('common.inactiveLabel');
      if (s.isActive) {
        statusClassName = 'fy-status fy-status--active';
        statusLabel = t('common.activeLabel');
      }
      rows.push(
        <tr key={s.id}>
          <td style={{ fontSize: 13, color: 'var(--color-text-muted)' }}>{skillCategoryPath(s)}</td>
          <td>{s.name}</td>
          <td style={{ fontSize: 13, maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {descriptionLabel}
          </td>
          <td style={{ textAlign: 'center' }}>{s.sortOrder}</td>
          <td>
            <span className={statusClassName}>
              {statusLabel}
            </span>
          </td>
          <td>
            <button className="btn btn--secondary btn--sm" onClick={() => openEdit(s)}>
              <IconEdit size={12} />{t('common.edit')}
            </button>
          </td>
        </tr>,
      );
    }
    tableBody = rows;
  }

  let modalTitle = t('itSkill.modalCreate');
  if (mode === 'edit') {
    modalTitle = t('itSkill.modalEdit');
  }

  let formLv1Value: number | string = '';
  if (form.lv1Id != null) {
    formLv1Value = form.lv1Id;
  }
  let formLv2Value: number | string = '';
  if (form.lv2Id != null) {
    formLv2Value = form.lv2Id;
  }
  let formLv3Value: number | string = '';
  if (form.lv3Id != null) {
    formLv3Value = form.lv3Id;
  }

  let saveButtonLabel = t('common.save');
  if (saving) {
    saveButtonLabel = t('common.saving');
  }

  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
        <label className="master-label" style={{ minWidth: 'auto' }}>{t('itSkill.skill.filterLabel')}</label>
        <select className="master-select"
          value={filterLv1Value}
          onChange={e => {
            let v: number | null = null;
            if (e.target.value !== '') {
              v = Number(e.target.value);
            }
            setFilterLv1(v);
            setFilterLv2(null);
            setFilterLv3(null);
          }}>
          <option value="">{t('common.allOption')}</option>
          {buildCategoryOptions(lv1Cats)}
        </select>
        {filterLv1 && lv2Cats(filterLv1).length > 0 && (
          <select className="master-select"
            value={filterLv2Value}
            onChange={e => {
              let v: number | null = null;
              if (e.target.value !== '') {
                v = Number(e.target.value);
              }
              setFilterLv2(v);
              setFilterLv3(null);
            }}>
            <option value="">{t('common.allOption')}</option>
            {buildCategoryOptions(lv2Cats(filterLv1))}
          </select>
        )}
        {filterLv2 && lv3Cats(filterLv2).length > 0 && (
          <select className="master-select"
            value={filterLv3Value}
            onChange={e => {
              if (e.target.value === '') {
                setFilterLv3(null);
              } else {
                setFilterLv3(Number(e.target.value));
              }
            }}>
            <option value="">{t('common.allOption')}</option>
            {buildCategoryOptions(lv3Cats(filterLv2))}
          </select>
        )}
        <button className="btn btn--primary btn--sm" onClick={openCreate} style={{ marginLeft: 'auto' }}>
          <IconPlus size={12} />{t('itSkill.addButton')}
        </button>
      </div>

      <StickyHorizontalScroll className="master-table-wrap">
        <table className="master-table">
          <thead>
            <tr>
              <th>{t('itSkill.form.categoryLabel')}</th>
              <th>{t('itSkill.table.name')}</th>
              <th style={{ width: 200 }}>{t('itSkill.table.description')}</th>
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
                <label className="form-label">{t('itSkill.form.categoryLabel')} <span className="required">*</span></label>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                  <select className="master-select" style={{ width: '100%' }}
                    value={formLv1Value}
                    onChange={e => {
                      let v: number | null = null;
                      if (e.target.value !== '') {
                        v = Number(e.target.value);
                      }
                      setForm(f => ({ ...f, lv1Id: v, lv2Id: null, lv3Id: null }));
                    }}>
                    <option value="">{t('itSkill.skill.form.lv1Select')}</option>
                    {buildCategoryOptions(lv1Cats)}
                  </select>
                  {form.lv1Id && lv2Cats(form.lv1Id).length > 0 && (
                    <select className="master-select" style={{ width: '100%' }}
                      value={formLv2Value}
                      onChange={e => {
                        let v: number | null = null;
                        if (e.target.value !== '') {
                          v = Number(e.target.value);
                        }
                        setForm(f => ({ ...f, lv2Id: v, lv3Id: null }));
                      }}>
                      <option value="">{t('itSkill.skill.form.lv2Select')}</option>
                      {buildCategoryOptions(lv2Cats(form.lv1Id))}
                    </select>
                  )}
                  {form.lv2Id && lv3Cats(form.lv2Id).length > 0 && (
                    <select className="master-select" style={{ width: '100%' }}
                      value={formLv3Value}
                      onChange={e => {
                        let v: number | null = null;
                        if (e.target.value !== '') {
                          v = Number(e.target.value);
                        }
                        setForm(f => ({ ...f, lv3Id: v }));
                      }}>
                      <option value="">{t('itSkill.skill.form.lv3Select')}</option>
                      {buildCategoryOptions(lv3Cats(form.lv2Id))}
                    </select>
                  )}
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">{t('itSkill.form.nameLabel')} <span className="required">*</span></label>
                <input className="form-input" value={form.name}
                  onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  placeholder={t('itSkill.skill.form.namePlaceholder')} maxLength={200} />
              </div>

              <div className="form-group">
                <label className="form-label">{t('itSkill.form.descriptionLabel')}</label>
                <textarea className="form-input" value={form.description}
                  onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                  rows={3} placeholder={t('itSkill.skill.form.descriptionPlaceholder')} style={{ resize: 'vertical' }} />
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

// ─── Promotion tab ───────────────────────────────────────────────────────────

interface PromoteForm {
  lv1Id: number | null;
  lv2Id: number | null;
  lv3Id: number | null;
  name: string;
  description: string;
  sortOrder: string;
}

/**
 * ITスキル昇格タブ。
 *
 * ユーザーが自由入力したカスタムITスキル名を正式マスタとして登録（昇格）する。
 */
function PromotionTab({ categories, onSkillsReload }: {
  categories: ItSkillCategory[];
  onSkillsReload: () => void;
}) {
  const { t } = useTranslation('master');
  const [items, setItems] = useState<CustomUnregisteredItem[]>([]);
  const [tabLoading, setTabLoading] = useState(true);
  const [tabError, setTabError] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [promoting, setPromoting] = useState<CustomUnregisteredItem | null>(null);
  const [form, setForm] = useState<PromoteForm>({ lv1Id: null, lv2Id: null, lv3Id: null, name: '', description: '', sortOrder: '0' });
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const lv1Cats = filterActiveByLevel(categories, 1);
  const lv2Cats = (pid: number | null) => filterActiveByLevelAndParent(categories, 2, pid);
  const lv3Cats = (pid: number | null) => filterActiveByLevelAndParent(categories, 3, pid);

  const loadItems = () => {
    setTabLoading(true);
    getCustomUnregisteredItSkills()
      .then(res => setItems(res.data))
      .catch(() => setTabError(t('common.loadFetchFailed')))
      .finally(() => setTabLoading(false));
  };

  // 初期表示時に未登録カスタムITスキル一覧を取得する
  useEffect(() => { loadItems(); }, []);

  const openPromote = (item: CustomUnregisteredItem) => {
    setPromoting(item);
    setForm({ lv1Id: null, lv2Id: null, lv3Id: null, name: item.customName, description: '', sortOrder: '0' });
    setFormError('');
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    const catId = resolveSelectedCategoryId(form);
    if (!catId) { setFormError(t('itSkill.promotion.validation.categoryRequired')); return; }
    if (!form.name.trim()) { setFormError(t('itSkill.promotion.validation.nameRequired')); return; }
    if (!promoting) return;
    setSaving(true); setFormError('');
    try {
      await promoteItSkill({
        customName: promoting.customName,
        categoryId: catId,
        name: form.name,
        description: form.description || null,
        sortOrder: Number(form.sortOrder) || 0,
      });
      setModalOpen(false);
      loadItems();
      onSkillsReload();
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
        {t('itSkill.promotion.emptyMessage')}
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
              <th>{t('itSkill.promotion.table.customName')}</th>
              <th style={{ width: 110 }}>{t('itSkill.table.usageCount')}</th>
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

  let formLv1Value: number | string = '';
  if (form.lv1Id != null) {
    formLv1Value = form.lv1Id;
  }
  let formLv2Value: number | string = '';
  if (form.lv2Id != null) {
    formLv2Value = form.lv2Id;
  }
  let formLv3Value: number | string = '';
  if (form.lv3Id != null) {
    formLv3Value = form.lv3Id;
  }

  let submitButtonLabel = t('common.registerButton');
  if (saving) {
    submitButtonLabel = t('common.registeringButton');
  }

  return (
    <>
      <div className="master-card__header" style={{ marginBottom: 16 }}>
        <h3 className="master-card__title" style={{ marginBottom: 0 }}>{t('itSkill.promotion.tabTitle')}</h3>
        <span style={{ fontSize: 13, color: 'var(--color-text-muted)' }}>
          {t('itSkill.promotion.hint')}
        </span>
      </div>

      {listBody}

      {modalOpen && createPortal(
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal__header">
              <h3>{t('itSkill.promotion.tabTitle')}</h3>
              <button className="modal__close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal__body">
              {formError && <div className="alert alert--error">{formError}</div>}

              <div className="form-group">
                <label className="form-label">{t('itSkill.promotion.form.originalNameLabel')}</label>
                <div style={{ padding: '6px 0', fontWeight: 500 }}>{originalNameLabel}</div>
              </div>

              <div className="form-group">
                <label className="form-label">{t('itSkill.form.categoryLabel')} <span className="required">*</span></label>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                  <select className="master-select" style={{ width: '100%' }}
                    value={formLv1Value}
                    onChange={e => {
                      let v: number | null = null;
                      if (e.target.value !== '') {
                        v = Number(e.target.value);
                      }
                      setForm(f => ({ ...f, lv1Id: v, lv2Id: null, lv3Id: null }));
                    }}>
                    <option value="">{t('itSkill.skill.form.lv1Select')}</option>
                    {buildCategoryOptions(lv1Cats)}
                  </select>
                  {form.lv1Id && lv2Cats(form.lv1Id).length > 0 && (
                    <select className="master-select" style={{ width: '100%' }}
                      value={formLv2Value}
                      onChange={e => {
                        let v: number | null = null;
                        if (e.target.value !== '') {
                          v = Number(e.target.value);
                        }
                        setForm(f => ({ ...f, lv2Id: v, lv3Id: null }));
                      }}>
                      <option value="">{t('itSkill.skill.form.lv2Select')}</option>
                      {buildCategoryOptions(lv2Cats(form.lv1Id))}
                    </select>
                  )}
                  {form.lv2Id && lv3Cats(form.lv2Id).length > 0 && (
                    <select className="master-select" style={{ width: '100%' }}
                      value={formLv3Value}
                      onChange={e => {
                        let v: number | null = null;
                        if (e.target.value !== '') {
                          v = Number(e.target.value);
                        }
                        setForm(f => ({ ...f, lv3Id: v }));
                      }}>
                      <option value="">{t('itSkill.skill.form.lv3Select')}</option>
                      {buildCategoryOptions(lv3Cats(form.lv2Id))}
                    </select>
                  )}
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">{t('itSkill.promote.masterNameLabel')} <span className="required">*</span></label>
                <input className="form-input" value={form.name}
                  onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  maxLength={200} />
                <span className="form-hint">{t('itSkill.promotion.form.masterNameHint')}</span>
              </div>

              <div className="form-group">
                <label className="form-label">{t('itSkill.form.descriptionLabel')}</label>
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

// ─── Page ────────────────────────────────────────────────────────────────────

type TabKey = 'categories' | 'skills' | 'promote';

/**
 * ITスキルマスタ管理ページ。
 *
 * カテゴリ・スキル・昇格の3タブで構成されるITスキルマスタ管理画面。
 * Excel インポート・エクスポートもサポートする。ADMIN ロールのみアクセス可能。
 */
export default function ItSkillMasterPage() {
  const { t } = useTranslation('master');
  const [activeTab, setActiveTab] = useState<TabKey>('categories');
  const [categories, setCategories] = useState<ItSkillCategory[]>([]);
  const [skills, setSkills] = useState<ItSkill[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [downloading, setDownloading] = useState(false);
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState<MasterImportResult | null>(null);
  const [importErrors, setImportErrors] = useState<MasterImportError[] | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // リロード完了を呼び出し元で await できるように Promise を返す
  const loadAll = () => {
    setLoading(true);
    return Promise.all([getItSkillCategories(), getItSkills()])
      .then(([catRes, skillRes]) => {
        setCategories(catRes.data);
        setSkills(skillRes.data);
      })
      .catch(() => setError(t('common.loadFailed')))
      .finally(() => setLoading(false));
  };

  // 初期表示時にITスキルカテゴリとITスキル一覧を取得する
  useEffect(() => { loadAll(); }, []);

  const handleDownload = async () => {
    setDownloading(true);
    try {
      const res = await downloadItSkillExcel();
      const url = URL.createObjectURL(res.data);
      const a = document.createElement('a');
      a.href = url; a.download = 'ItSkillMaster.xlsx'; a.click();
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
      const res = await uploadItSkillExcel(file);
      await loadAll(); // リロード完了後にモーダルを表示
      setImportResult(res.data);
    } catch (err: unknown) {
      // ネットワークエラー等でエラー配列が取れない場合も適切なメッセージを表示
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
  let skillsTabClassName = 'tab-btn';
  if (activeTab === 'skills') {
    skillsTabClassName += ' active';
  }
  let promoteTabClassName = 'tab-btn';
  if (activeTab === 'promote') {
    promoteTabClassName += ' active';
  }

  let tabContent: React.ReactNode;
  if (activeTab === 'categories') {
    tabContent = <CategoryTab categories={categories} onReload={loadAll} />;
  } else if (activeTab === 'skills') {
    tabContent = <SkillTab skills={skills} categories={categories} onReload={loadAll} />;
  } else {
    tabContent = <PromotionTab categories={categories} onSkillsReload={loadAll} />;
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
            {t('itSkill.tab.categories')}
          </button>
          <button className={skillsTabClassName}
            onClick={() => setActiveTab('skills')}>
            {t('itSkill.tab.skills')}
          </button>
          <button className={promoteTabClassName}
            onClick={() => setActiveTab('promote')}>
            {t('itSkill.tab.promote')}
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
        {/* display:none の代わりに視覚的に隠すことでスクリーンリーダーからアクセス可能にする */}
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
