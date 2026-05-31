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

// ─── Category tab ────────────────────────────────────────────────────────────

interface CatForm {
  parentId: number | null;
  name: string;
  sortOrder: string;
  active: boolean;
}

type CatModalMode = 'create' | 'edit';

function CategoryTab({ categories, onReload }: { categories: ItSkillCategory[]; onReload: () => void }) {
  const { t } = useTranslation('master');
  const [modalOpen, setModalOpen] = useState(false);
  const [mode, setMode] = useState<CatModalMode>('create');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<CatForm>({ parentId: null, name: '', sortOrder: '0', active: true });
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const catMap = useMemo(() => new Map(categories.map(c => [c.id, c])), [categories]);
  const lv1 = categories.filter(c => c.level === 1);
  const lv2 = categories.filter(c => c.level === 2);

  const parentLabel = (c: ItSkillCategory) => {
    if (c.level === 1) return '—';
    const parent = catMap.get(c.parentId!);
    return parent ? parent.name : `ID:${c.parentId}`;
  };

  const levelBadge = (level: number) => {
    const cls = ['', 'cat-lv cat-lv--1', 'cat-lv cat-lv--2', 'cat-lv cat-lv--3'][level] ?? '';
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
      const msg = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail;
      setFormError(msg ?? t('common.saveFailed'));
    } finally { setSaving(false); }
  };

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
            {categories.length === 0 ? (
              <tr><td colSpan={6} className="master-table__empty">{t('common.noData')}</td></tr>
            ) : (
              categories.map(c => (
                <tr key={c.id}>
                  <td>{levelBadge(c.level)}</td>
                  <td style={{ color: 'var(--color-text-muted)', fontSize: 13 }}>{parentLabel(c)}</td>
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
              <h3>{mode === 'create' ? t('itSkill.modalCategoryCreate') : t('itSkill.modalCategoryEdit')}</h3>
              <button className="modal__close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal__body">
              {formError && <div className="alert alert--error">{formError}</div>}

              {mode === 'create' && (
                <div className="form-group">
                  <label className="form-label">{t('itSkill.form.parentCategoryLabel')}</label>
                  <select
                    className="master-select" style={{ width: '100%' }}
                    value={form.parentId ?? ''}
                    onChange={e => setForm(f => ({ ...f, parentId: e.target.value === '' ? null : Number(e.target.value) }))}
                  >
                    <option value="">{t('itSkill.category.form.parentNone')}</option>
                    <optgroup label={t('itSkill.category.form.lv1Group')}>
                      {lv1.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                    </optgroup>
                    <optgroup label={t('itSkill.category.form.lv2Group')}>
                      {lv2.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
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

  const catMap = useMemo(() => new Map(categories.map(c => [c.id, c])), [categories]);
  const lv1Cats = categories.filter(c => c.level === 1);
  const lv2Cats = (parentId: number | null) => categories.filter(c => c.level === 2 && c.parentId === parentId);
  const lv3Cats = (parentId: number | null) => categories.filter(c => c.level === 3 && c.parentId === parentId);

  const selectedCategoryId = (): number | null => form.lv3Id ?? form.lv2Id ?? form.lv1Id;

  const skillCategoryPath = (skill: ItSkill): string => {
    const parts: string[] = [];
    if (skill.category1Name) parts.push(skill.category1Name);
    if (skill.category2Name) parts.push(skill.category2Name);
    if (skill.category3Name) parts.push(skill.category3Name);
    return parts.join(' › ') || `カテゴリID:${skill.categoryId}`;
  };

  const filteredSkills = useMemo(() => {
    let result = skills;
    if (filterLv1) result = result.filter(s => s.category1Id === filterLv1);
    if (filterLv2) {
      const ids = new Set<number>([
        filterLv2,
        ...categories.filter(c => c.level === 3 && c.parentId === filterLv2).map(c => c.id),
      ]);
      result = result.filter(s => ids.has(s.categoryId));
    }
    if (filterLv3) result = result.filter(s => s.categoryId === filterLv3);
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
      if (cat.level === 1) lv1Id = cat.id;
      else if (cat.level === 2) { lv2Id = cat.id; lv1Id = cat.parentId ?? null; }
      else if (cat.level === 3) {
        lv3Id = cat.id;
        const lv2 = catMap.get(cat.parentId!);
        if (lv2) { lv2Id = lv2.id; lv1Id = lv2.parentId ?? null; }
      }
    }
    setForm({ lv1Id, lv2Id, lv3Id, name: s.name, description: s.description ?? '', sortOrder: String(s.sortOrder), active: s.isActive });
    setFormError('');
    setMode('edit');
    setEditingId(s.id);
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    const catId = selectedCategoryId();
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
      const msg = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail;
      setFormError(msg ?? t('common.saveFailed'));
    } finally { setSaving(false); }
  };

  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
        <label className="master-label" style={{ minWidth: 'auto' }}>{t('itSkill.skill.filterLabel')}</label>
        <select className="master-select"
          value={filterLv1 ?? ''}
          onChange={e => {
            const v = e.target.value === '' ? null : Number(e.target.value);
            setFilterLv1(v);
            setFilterLv2(null);
            setFilterLv3(null);
          }}>
          <option value="">{t('common.allOption')}</option>
          {lv1Cats.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        {filterLv1 && lv2Cats(filterLv1).length > 0 && (
          <select className="master-select"
            value={filterLv2 ?? ''}
            onChange={e => {
              const v = e.target.value === '' ? null : Number(e.target.value);
              setFilterLv2(v);
              setFilterLv3(null);
            }}>
            <option value="">{t('common.allOption')}</option>
            {lv2Cats(filterLv1).map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        )}
        {filterLv2 && lv3Cats(filterLv2).length > 0 && (
          <select className="master-select"
            value={filterLv3 ?? ''}
            onChange={e => setFilterLv3(e.target.value === '' ? null : Number(e.target.value))}>
            <option value="">{t('common.allOption')}</option>
            {lv3Cats(filterLv2).map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
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
            {filteredSkills.length === 0 ? (
              <tr><td colSpan={6} className="master-table__empty">{t('common.noData')}</td></tr>
            ) : (
              filteredSkills.map(s => (
                <tr key={s.id}>
                  <td style={{ fontSize: 13, color: 'var(--color-text-muted)' }}>{skillCategoryPath(s)}</td>
                  <td>{s.name}</td>
                  <td style={{ fontSize: 13, maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {s.description ?? '—'}
                  </td>
                  <td style={{ textAlign: 'center' }}>{s.sortOrder}</td>
                  <td>
                    <span className={s.isActive ? 'fy-status fy-status--active' : 'fy-status fy-status--inactive'}>
                      {s.isActive ? t('common.activeLabel') : t('common.inactiveLabel')}
                    </span>
                  </td>
                  <td>
                    <button className="btn btn--secondary btn--sm" onClick={() => openEdit(s)}>
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
              <h3>{mode === 'create' ? t('itSkill.modalCreate') : t('itSkill.modalEdit')}</h3>
              <button className="modal__close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal__body">
              {formError && <div className="alert alert--error">{formError}</div>}

              <div className="form-group">
                <label className="form-label">{t('itSkill.form.categoryLabel')} <span className="required">*</span></label>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                  <select className="master-select" style={{ width: '100%' }}
                    value={form.lv1Id ?? ''}
                    onChange={e => {
                      const v = e.target.value === '' ? null : Number(e.target.value);
                      setForm(f => ({ ...f, lv1Id: v, lv2Id: null, lv3Id: null }));
                    }}>
                    <option value="">{t('itSkill.skill.form.lv1Select')}</option>
                    {lv1Cats.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                  </select>
                  {form.lv1Id && lv2Cats(form.lv1Id).length > 0 && (
                    <select className="master-select" style={{ width: '100%' }}
                      value={form.lv2Id ?? ''}
                      onChange={e => {
                        const v = e.target.value === '' ? null : Number(e.target.value);
                        setForm(f => ({ ...f, lv2Id: v, lv3Id: null }));
                      }}>
                      <option value="">{t('itSkill.skill.form.lv2Select')}</option>
                      {lv2Cats(form.lv1Id).map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                    </select>
                  )}
                  {form.lv2Id && lv3Cats(form.lv2Id).length > 0 && (
                    <select className="master-select" style={{ width: '100%' }}
                      value={form.lv3Id ?? ''}
                      onChange={e => {
                        const v = e.target.value === '' ? null : Number(e.target.value);
                        setForm(f => ({ ...f, lv3Id: v }));
                      }}>
                      <option value="">{t('itSkill.skill.form.lv3Select')}</option>
                      {lv3Cats(form.lv2Id).map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
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

// ─── Promotion tab ───────────────────────────────────────────────────────────

interface PromoteForm {
  lv1Id: number | null;
  lv2Id: number | null;
  lv3Id: number | null;
  name: string;
  description: string;
  sortOrder: string;
}

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

  const lv1Cats = categories.filter(c => c.level === 1 && c.isActive);
  const lv2Cats = (pid: number | null) => categories.filter(c => c.level === 2 && c.parentId === pid && c.isActive);
  const lv3Cats = (pid: number | null) => categories.filter(c => c.level === 3 && c.parentId === pid && c.isActive);
  const selectedCategoryId = (): number | null => form.lv3Id ?? form.lv2Id ?? form.lv1Id;

  const loadItems = () => {
    setTabLoading(true);
    getCustomUnregisteredItSkills()
      .then(res => setItems(res.data))
      .catch(() => setTabError(t('common.loadFetchFailed')))
      .finally(() => setTabLoading(false));
  };

  useEffect(() => { loadItems(); }, []);

  const openPromote = (item: CustomUnregisteredItem) => {
    setPromoting(item);
    setForm({ lv1Id: null, lv2Id: null, lv3Id: null, name: item.customName, description: '', sortOrder: '0' });
    setFormError('');
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    const catId = selectedCategoryId();
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

  return (
    <>
      <div className="master-card__header" style={{ marginBottom: 16 }}>
        <h3 className="master-card__title" style={{ marginBottom: 0 }}>{t('itSkill.promotion.tabTitle')}</h3>
        <span style={{ fontSize: 13, color: 'var(--color-text-muted)' }}>
          {t('itSkill.promotion.hint')}
        </span>
      </div>

      {items.length === 0 ? (
        <div style={{ padding: '32px 0', textAlign: 'center', color: 'var(--color-text-muted)', fontSize: 14 }}>
          {t('itSkill.promotion.emptyMessage')}
        </div>
      ) : (
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
              <h3>{t('itSkill.promotion.tabTitle')}</h3>
              <button className="modal__close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal__body">
              {formError && <div className="alert alert--error">{formError}</div>}

              <div className="form-group">
                <label className="form-label">{t('itSkill.promotion.form.originalNameLabel')}</label>
                <div style={{ padding: '6px 0', fontWeight: 500 }}>{promoting?.customName}</div>
              </div>

              <div className="form-group">
                <label className="form-label">{t('itSkill.form.categoryLabel')} <span className="required">*</span></label>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                  <select className="master-select" style={{ width: '100%' }}
                    value={form.lv1Id ?? ''}
                    onChange={e => {
                      const v = e.target.value === '' ? null : Number(e.target.value);
                      setForm(f => ({ ...f, lv1Id: v, lv2Id: null, lv3Id: null }));
                    }}>
                    <option value="">{t('itSkill.skill.form.lv1Select')}</option>
                    {lv1Cats.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                  </select>
                  {form.lv1Id && lv2Cats(form.lv1Id).length > 0 && (
                    <select className="master-select" style={{ width: '100%' }}
                      value={form.lv2Id ?? ''}
                      onChange={e => {
                        const v = e.target.value === '' ? null : Number(e.target.value);
                        setForm(f => ({ ...f, lv2Id: v, lv3Id: null }));
                      }}>
                      <option value="">{t('itSkill.skill.form.lv2Select')}</option>
                      {lv2Cats(form.lv1Id).map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                    </select>
                  )}
                  {form.lv2Id && lv3Cats(form.lv2Id).length > 0 && (
                    <select className="master-select" style={{ width: '100%' }}
                      value={form.lv3Id ?? ''}
                      onChange={e => {
                        const v = e.target.value === '' ? null : Number(e.target.value);
                        setForm(f => ({ ...f, lv3Id: v }));
                      }}>
                      <option value="">{t('itSkill.skill.form.lv3Select')}</option>
                      {lv3Cats(form.lv2Id).map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
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

// ─── Page ────────────────────────────────────────────────────────────────────

type TabKey = 'categories' | 'skills' | 'promote';

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

  // ⑥: Promise を返してリロード完了を呼び出し元で await できるようにする
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

  useEffect(() => { loadAll(); }, []);

  // ⑫: エラーハンドリングを追加
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
    const file = e.target.files?.[0];
    if (!file) return;
    e.target.value = '';
    setImporting(true);
    try {
      const res = await uploadItSkillExcel(file);
      await loadAll(); // ⑥: リロード完了後にモーダルを表示
      setImportResult(res.data);
    } catch (err: unknown) {
      // ⑤: ネットワークエラー等でエラー配列が取れない場合も適切なメッセージを表示
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
            {t('itSkill.tab.categories')}
          </button>
          <button className={`tab-btn${activeTab === 'skills' ? ' active' : ''}`}
            onClick={() => setActiveTab('skills')}>
            {t('itSkill.tab.skills')}
          </button>
          <button className={`tab-btn${activeTab === 'promote' ? ' active' : ''}`}
            onClick={() => setActiveTab('promote')}>
            {t('itSkill.tab.promote')}
          </button>
        </div>

        <section className="master-card">
          {activeTab === 'categories' ? (
            <CategoryTab categories={categories} onReload={loadAll} />
          ) : activeTab === 'skills' ? (
            <SkillTab skills={skills} categories={categories} onReload={loadAll} />
          ) : (
            <PromotionTab categories={categories} onSkillsReload={loadAll} />
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
        {/* ⑬: display:none の代わりに視覚的に隠すことでスクリーンリーダーからアクセス可能にする */}
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
