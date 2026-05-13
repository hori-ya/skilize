import { useEffect, useState } from 'react';
import NavBar from '../../components/NavBar';
import type { Qualification, QualificationCategory } from '../../types/master';
import {
  getQualifications, createQualification, updateQualification,
  getQualificationCategories, createQualificationCategory, updateQualificationCategory,
} from '../../api/master';

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
        <button className="btn btn--primary btn--sm" onClick={openCreate}>+ カテゴリ追加</button>
      </div>

      <div className="master-table-wrap">
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
                    <button className="btn btn--secondary btn--sm" onClick={() => openEdit(c)}>編集</button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

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
              <button className="btn btn--secondary" onClick={() => setModalOpen(false)}>キャンセル</button>
              <button className="btn btn--primary" onClick={handleSubmit} disabled={saving}>
                {saving ? '保存中...' : '保存'}
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
          + 資格追加
        </button>
      </div>

      <div className="master-table-wrap">
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
                  <td><button className="btn btn--secondary btn--sm" onClick={() => openEdit(q)}>編集</button></td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

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
              <button className="btn btn--secondary" onClick={() => setModalOpen(false)}>キャンセル</button>
              <button className="btn btn--primary" onClick={handleSubmit} disabled={saving}>
                {saving ? '保存中...' : '保存'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

type TabKey = 'categories' | 'qualifications';

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
        </div>

        <section className="master-card">
          {activeTab === 'categories' ? (
            <CategoryTab categories={categories} onReload={loadAll} />
          ) : (
            <QualificationTab qualifications={qualifications} categories={categories} onReload={loadAll} />
          )}
        </section>
      </main>
    </div>
  );
}
