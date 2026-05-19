import { useEffect, useState } from 'react';
import NavBar from '../../../app/layouts/NavBar';
import type { Qualification, QualificationCategory, CustomUnregisteredItem } from '../../../shared/types/master';
import {
  getQualifications, createQualification, updateQualification,
  getQualificationCategories, createQualificationCategory, updateQualificationCategory,
  getCustomUnregisteredQualifications, promoteQualification,
} from '../../../shared/api/masterApi';
import { IconPlus, IconEdit, IconX, IconCheck } from '../../../shared/ui/Icons';
import StickyHorizontalScroll from '../../../shared/ui/StickyHorizontalScroll';

type ModalMode = 'create' | 'edit';

// ─── Category tab ─────────────────────────────────────────────────────────────

interface CatForm { name: string; sortOrder: string; active: boolean }

function CategoryTab({ categories, onReload }: { categories: QualificationCategory[]; onReload: () => void }) {
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
    if (!form.name.trim()) { setFormError('カテゴリ名は必須です'); return; }
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
      setFormError('保存に失敗しました');
    } finally { setSaving(false); }
  };

  return (
    <>
      <div className="master-card__header" style={{ marginBottom: 16 }}>
        <h3 className="master-card__title" style={{ marginBottom: 0 }}>資格カテゴリ一覧</h3>
        <button className="btn btn--primary btn--sm" onClick={openCreate}><IconPlus size={12} />カテゴリ追加</button>
      </div>

      <StickyHorizontalScroll className="master-table-wrap">
        <table className="master-table">
          <thead>
            <tr>
              <th>カテゴリ名</th>
              <th style={{ width: 64 }}>並順</th>
              <th style={{ width: 72 }}>状態</th>
              <th style={{ width: 72 }}>操作</th>
            </tr>
          </thead>
          <tbody>
            {categories.length === 0 ? (
              <tr><td colSpan={4} className="master-table__empty">データがありません</td></tr>
            ) : (
              categories.map(c => (
                <tr key={c.id}>
                  <td>{c.name}</td>
                  <td style={{ textAlign: 'center' }}>{c.sortOrder}</td>
                  <td>
                    <span className={c.isActive ? 'fy-status fy-status--active' : 'fy-status fy-status--inactive'}>
                      {c.isActive ? '有効' : '無効'}
                    </span>
                  </td>
                  <td>
                    <button className="btn btn--secondary btn--sm" onClick={() => openEdit(c)}><IconEdit size={12} />編集</button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </StickyHorizontalScroll>

      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal__header">
              <h3>{mode === 'create' ? 'カテゴリ追加' : 'カテゴリ編集'}</h3>
              <button className="modal__close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal__body">
              {formError && <div className="alert alert--error">{formError}</div>}
              <div className="form-group">
                <label className="form-label">カテゴリ名 <span className="required">*</span></label>
                <input className="form-input" value={form.name}
                  onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  placeholder="例: ベンダー資格" maxLength={100} />
              </div>
              <div className="form-group">
                <label className="form-label">並順</label>
                <input type="number" className="form-input" value={form.sortOrder}
                  onChange={e => setForm(f => ({ ...f, sortOrder: e.target.value }))}
                  style={{ width: 100 }} />
              </div>
              {mode === 'edit' && (
                <div className="form-group">
                  <label className="form-check">
                    <input type="checkbox" checked={form.active}
                      onChange={e => setForm(f => ({ ...f, active: e.target.checked }))} />
                    <span>有効</span>
                  </label>
                </div>
              )}
            </div>
            <div className="modal__footer">
              <button className="btn btn--secondary" onClick={() => setModalOpen(false)}><IconX size={13} />キャンセル</button>
              <button className="btn btn--primary" onClick={handleSubmit} disabled={saving}>
                <IconCheck size={13} />{saving ? '保存中...' : '保存'}
              </button>
            </div>
          </div>
        </div>
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

function QualificationTab({ qualifications, categories, onReload }: {
  qualifications: Qualification[];
  categories: QualificationCategory[];
  onReload: () => void;
}) {
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
    if (!form.name.trim()) { setFormError('資格名は必須です'); return; }
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
      setFormError('保存に失敗しました');
    } finally { setSaving(false); }
  };

  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
        <label className="master-label" style={{ minWidth: 'auto' }}>カテゴリフィルタ</label>
        <select className="master-select"
          value={filterCatId ?? ''}
          onChange={e => setFilterCatId(e.target.value === '' ? null : Number(e.target.value))}>
          <option value="">すべて</option>
          {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <button className="btn btn--primary btn--sm" onClick={openCreate} style={{ marginLeft: 'auto' }}>
          <IconPlus size={12} />資格追加
        </button>
      </div>

      <StickyHorizontalScroll className="master-table-wrap">
        <table className="master-table">
          <thead>
            <tr>
              <th>資格名</th>
              <th style={{ width: 160 }}>カテゴリ</th>
              <th style={{ width: 200 }}>説明</th>
              <th style={{ width: 56 }}>並順</th>
              <th style={{ width: 72 }}>状態</th>
              <th style={{ width: 72 }}>操作</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr><td colSpan={6} className="master-table__empty">データがありません</td></tr>
            ) : (
              filtered.map(q => (
                <tr key={q.id}>
                  <td>{q.name}</td>
                  <td style={{ fontSize: 13, color: 'var(--color-text-muted)' }}>{q.categoryName ?? '—'}</td>
                  <td style={{ fontSize: 13, maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {q.description ?? '—'}
                  </td>
                  <td style={{ textAlign: 'center' }}>{q.sortOrder}</td>
                  <td>
                    <span className={q.isActive ? 'fy-status fy-status--active' : 'fy-status fy-status--inactive'}>
                      {q.isActive ? '有効' : '無効'}
                    </span>
                  </td>
                  <td><button className="btn btn--secondary btn--sm" onClick={() => openEdit(q)}><IconEdit size={12} />編集</button></td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </StickyHorizontalScroll>

      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal__header">
              <h3>{mode === 'create' ? '資格追加' : '資格編集'}</h3>
              <button className="modal__close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal__body">
              {formError && <div className="alert alert--error">{formError}</div>}
              <div className="form-group">
                <label className="form-label">カテゴリ</label>
                <select className="master-select" style={{ width: '100%' }}
                  value={form.categoryId ?? ''}
                  onChange={e => setForm(f => ({ ...f, categoryId: e.target.value === '' ? null : Number(e.target.value) }))}>
                  <option value="">カテゴリなし</option>
                  {categories.filter(c => c.isActive).map(c => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">資格名 <span className="required">*</span></label>
                <input className="form-input" value={form.name}
                  onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  placeholder="例: 応用情報技術者" maxLength={200} />
              </div>
              <div className="form-group">
                <label className="form-label">説明</label>
                <textarea className="form-input" value={form.description}
                  onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                  rows={3} placeholder="資格の説明（任意）" style={{ resize: 'vertical' }} />
              </div>
              <div className="form-group">
                <label className="form-label">並順</label>
                <input type="number" className="form-input" value={form.sortOrder}
                  onChange={e => setForm(f => ({ ...f, sortOrder: e.target.value }))}
                  style={{ width: 100 }} />
              </div>
              {mode === 'edit' && (
                <div className="form-group">
                  <label className="form-check">
                    <input type="checkbox" checked={form.active}
                      onChange={e => setForm(f => ({ ...f, active: e.target.checked }))} />
                    <span>有効</span>
                  </label>
                </div>
              )}
            </div>
            <div className="modal__footer">
              <button className="btn btn--secondary" onClick={() => setModalOpen(false)}><IconX size={13} />キャンセル</button>
              <button className="btn btn--primary" onClick={handleSubmit} disabled={saving}>
                <IconCheck size={13} />{saving ? '保存中...' : '保存'}
              </button>
            </div>
          </div>
        </div>
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

function QualPromotionTab({ categories, onQualReload }: {
  categories: QualificationCategory[];
  onQualReload: () => void;
}) {
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
      .catch(() => setTabError('取得に失敗しました'))
      .finally(() => setTabLoading(false));
  };

  useEffect(() => { loadItems(); }, []);

  const openPromote = (item: CustomUnregisteredItem) => {
    setPromoting(item);
    setForm({ categoryId: null, name: item.customName, description: '', sortOrder: '0' });
    setFormError('');
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    if (!form.name.trim()) { setFormError('資格名は必須です'); return; }
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
      setFormError('昇格に失敗しました');
    } finally { setSaving(false); }
  };

  if (tabLoading) return <div className="chart-loading">読み込み中...</div>;
  if (tabError) return <div className="alert alert--error">{tabError}</div>;

  return (
    <>
      <div className="master-card__header" style={{ marginBottom: 16 }}>
        <h3 className="master-card__title" style={{ marginBottom: 0 }}>カスタム資格昇格</h3>
        <span style={{ fontSize: 13, color: 'var(--color-text-muted)' }}>
          ユーザーが入力したカスタム資格をマスタに登録できます
        </span>
      </div>

      {items.length === 0 ? (
        <div style={{ padding: '32px 0', textAlign: 'center', color: 'var(--color-text-muted)', fontSize: 14 }}>
          昇格待ちのカスタム資格はありません
        </div>
      ) : (
        <StickyHorizontalScroll className="master-table-wrap">
          <table className="master-table">
            <thead>
              <tr>
                <th>カスタム資格名</th>
                <th style={{ width: 110 }}>使用件数</th>
                <th style={{ width: 80 }}>操作</th>
              </tr>
            </thead>
            <tbody>
              {items.map(item => (
                <tr key={item.customName}>
                  <td>{item.customName}</td>
                  <td style={{ textAlign: 'center' }}>{item.usageCount}件</td>
                  <td>
                    <button className="btn btn--primary btn--sm" onClick={() => openPromote(item)}>昇格</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </StickyHorizontalScroll>
      )}

      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal__header">
              <h3>カスタム資格昇格</h3>
              <button className="modal__close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal__body">
              {formError && <div className="alert alert--error">{formError}</div>}

              <div className="form-group">
                <label className="form-label">カスタム資格名（元）</label>
                <div style={{ padding: '6px 0', fontWeight: 500 }}>{promoting?.customName}</div>
              </div>

              <div className="form-group">
                <label className="form-label">カテゴリ</label>
                <select className="master-select" style={{ width: '100%' }}
                  value={form.categoryId ?? ''}
                  onChange={e => setForm(f => ({ ...f, categoryId: e.target.value === '' ? null : Number(e.target.value) }))}>
                  <option value="">カテゴリなし</option>
                  {categories.filter(c => c.isActive).map(c => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">マスタ登録名 <span className="required">*</span></label>
                <input className="form-input" value={form.name}
                  onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  maxLength={200} />
                <span className="form-hint">カスタム名から変更して正式名称に統一できます</span>
              </div>

              <div className="form-group">
                <label className="form-label">説明</label>
                <textarea className="form-input" value={form.description}
                  onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                  rows={3} style={{ resize: 'vertical' }} />
              </div>

              <div className="form-group">
                <label className="form-label">並順</label>
                <input type="number" className="form-input" value={form.sortOrder}
                  onChange={e => setForm(f => ({ ...f, sortOrder: e.target.value }))}
                  style={{ width: 100 }} />
              </div>
            </div>
            <div className="modal__footer">
              <button className="btn btn--secondary" onClick={() => setModalOpen(false)}><IconX size={13} />キャンセル</button>
              <button className="btn btn--primary" onClick={handleSubmit} disabled={saving}>
                <IconCheck size={13} />{saving ? '登録中...' : 'マスタに登録'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

type TabKey = 'categories' | 'qualifications' | 'promote';

export default function QualificationMasterPage() {
  const [activeTab, setActiveTab] = useState<TabKey>('categories');
  const [categories, setCategories] = useState<QualificationCategory[]>([]);
  const [qualifications, setQualifications] = useState<Qualification[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadAll = () => {
    setLoading(true);
    Promise.all([getQualificationCategories(), getQualifications()])
      .then(([catRes, qualRes]) => {
        setCategories(catRes.data);
        setQualifications(qualRes.data);
      })
      .catch(() => setError('データの取得に失敗しました'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { loadAll(); }, []);

  if (loading) return <div className="loading-screen"><span>読み込み中...</span></div>;

  return (
    <div className="master-page">
      <NavBar />

      <main className="master-main">
        {error && <div className="alert alert--error">{error}</div>}

        <div className="tab-bar">
          <button className={`tab-btn${activeTab === 'categories' ? ' active' : ''}`}
            onClick={() => setActiveTab('categories')}>
            カテゴリ管理
          </button>
          <button className={`tab-btn${activeTab === 'qualifications' ? ' active' : ''}`}
            onClick={() => setActiveTab('qualifications')}>
            資格管理
          </button>
          <button className={`tab-btn${activeTab === 'promote' ? ' active' : ''}`}
            onClick={() => setActiveTab('promote')}>
            カスタム昇格
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
    </div>
  );
}
