import { useEffect, useState, useMemo } from 'react';
import NavBar from '../../../app/layouts/NavBar';
import type { ItSkillCategory, ItSkill } from '../../../shared/types/master';
import {
  getItSkillCategories, createItSkillCategory, updateItSkillCategory,
  getItSkills, createItSkill, updateItSkill,
} from '../../../shared/api/masterApi';
import { IconPlus, IconEdit, IconX, IconCheck } from '../../../shared/ui/Icons';

// ─── Category tab ────────────────────────────────────────────────────────────

interface CatForm {
  parentId: number | null;
  name: string;
  sortOrder: string;
  active: boolean;
}

type CatModalMode = 'create' | 'edit';

function CategoryTab({ categories, onReload }: { categories: ItSkillCategory[]; onReload: () => void }) {
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
    if (!form.name.trim()) { setFormError('分類名は必須です'); return; }
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
      setFormError(msg ?? '保存に失敗しました');
    } finally { setSaving(false); }
  };

  return (
    <>
      <div className="master-card__header" style={{ marginBottom: 16 }}>
        <h3 className="master-card__title" style={{ marginBottom: 0 }}>IT分類一覧</h3>
        <button className="btn btn--primary btn--sm" onClick={openCreate}><IconPlus size={12} />分類追加</button>
      </div>

      <div className="master-table-wrap">
        <table className="master-table">
          <thead>
            <tr>
              <th style={{ width: 64 }}>階層</th>
              <th>親分類</th>
              <th>分類名</th>
              <th style={{ width: 64 }}>並順</th>
              <th style={{ width: 72 }}>状態</th>
              <th style={{ width: 72 }}>操作</th>
            </tr>
          </thead>
          <tbody>
            {categories.length === 0 ? (
              <tr><td colSpan={6} className="master-table__empty">データがありません</td></tr>
            ) : (
              categories.map(c => (
                <tr key={c.id}>
                  <td>{levelBadge(c.level)}</td>
                  <td style={{ color: 'var(--color-text-muted)', fontSize: 13 }}>{parentLabel(c)}</td>
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
      </div>

      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal__header">
              <h3>{mode === 'create' ? '分類追加' : '分類編集'}</h3>
              <button className="modal__close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal__body">
              {formError && <div className="alert alert--error">{formError}</div>}

              {mode === 'create' && (
                <div className="form-group">
                  <label className="form-label">親分類</label>
                  <select
                    className="master-select" style={{ width: '100%' }}
                    value={form.parentId ?? ''}
                    onChange={e => setForm(f => ({ ...f, parentId: e.target.value === '' ? null : Number(e.target.value) }))}
                  >
                    <option value="">なし（第1階層）</option>
                    <optgroup label="第1階層（Lv2の親として追加）">
                      {lv1.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                    </optgroup>
                    <optgroup label="第2階層（Lv3の親として追加）">
                      {lv2.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                    </optgroup>
                  </select>
                  <span style={{ fontSize: 12, color: 'var(--color-text-muted)' }}>
                    未選択 = 第1階層、Lv1選択 = 第2階層、Lv2選択 = 第3階層
                  </span>
                </div>
              )}

              <div className="form-group">
                <label className="form-label">分類名 <span className="required">*</span></label>
                <input className="form-input" value={form.name}
                  onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  placeholder="例: プログラミング言語" maxLength={100} />
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
  const [filterLv1, setFilterLv1] = useState<number | null>(null);
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

  const filteredSkills = filterLv1
    ? skills.filter(s => s.category1Id === filterLv1)
    : skills;

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
    if (!catId) { setFormError('分類を選択してください'); return; }
    if (!form.name.trim()) { setFormError('スキル名は必須です'); return; }
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
      setFormError(msg ?? '保存に失敗しました');
    } finally { setSaving(false); }
  };

  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
        <label className="master-label" style={{ minWidth: 'auto' }}>分類フィルタ</label>
        <select className="master-select"
          value={filterLv1 ?? ''}
          onChange={e => setFilterLv1(e.target.value === '' ? null : Number(e.target.value))}>
          <option value="">すべて</option>
          {lv1Cats.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <button className="btn btn--primary btn--sm" onClick={openCreate} style={{ marginLeft: 'auto' }}>
          <IconPlus size={12} />スキル追加
        </button>
      </div>

      <div className="master-table-wrap">
        <table className="master-table">
          <thead>
            <tr>
              <th>スキル名</th>
              <th>分類</th>
              <th style={{ width: 200 }}>説明</th>
              <th style={{ width: 56 }}>並順</th>
              <th style={{ width: 72 }}>状態</th>
              <th style={{ width: 72 }}>操作</th>
            </tr>
          </thead>
          <tbody>
            {filteredSkills.length === 0 ? (
              <tr><td colSpan={6} className="master-table__empty">データがありません</td></tr>
            ) : (
              filteredSkills.map(s => (
                <tr key={s.id}>
                  <td>{s.name}</td>
                  <td style={{ fontSize: 13, color: 'var(--color-text-muted)' }}>{skillCategoryPath(s)}</td>
                  <td style={{ fontSize: 13, maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {s.description ?? '—'}
                  </td>
                  <td style={{ textAlign: 'center' }}>{s.sortOrder}</td>
                  <td>
                    <span className={s.isActive ? 'fy-status fy-status--active' : 'fy-status fy-status--inactive'}>
                      {s.isActive ? '有効' : '無効'}
                    </span>
                  </td>
                  <td><button className="btn btn--secondary btn--sm" onClick={() => openEdit(s)}><IconEdit size={12} />編集</button></td>
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
              <h3>{mode === 'create' ? 'スキル追加' : 'スキル編集'}</h3>
              <button className="modal__close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal__body">
              {formError && <div className="alert alert--error">{formError}</div>}

              <div className="form-group">
                <label className="form-label">分類 <span className="required">*</span></label>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                  <select className="master-select" style={{ width: '100%' }}
                    value={form.lv1Id ?? ''}
                    onChange={e => {
                      const v = e.target.value === '' ? null : Number(e.target.value);
                      setForm(f => ({ ...f, lv1Id: v, lv2Id: null, lv3Id: null }));
                    }}>
                    <option value="">第1階層を選択</option>
                    {lv1Cats.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                  </select>
                  {form.lv1Id && lv2Cats(form.lv1Id).length > 0 && (
                    <select className="master-select" style={{ width: '100%' }}
                      value={form.lv2Id ?? ''}
                      onChange={e => {
                        const v = e.target.value === '' ? null : Number(e.target.value);
                        setForm(f => ({ ...f, lv2Id: v, lv3Id: null }));
                      }}>
                      <option value="">第2階層を選択（任意）</option>
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
                      <option value="">第3階層を選択（任意）</option>
                      {lv3Cats(form.lv2Id).map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                    </select>
                  )}
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">スキル名 <span className="required">*</span></label>
                <input className="form-input" value={form.name}
                  onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  placeholder="例: Spring Boot" maxLength={200} />
              </div>

              <div className="form-group">
                <label className="form-label">説明</label>
                <textarea className="form-input" value={form.description}
                  onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                  rows={3} placeholder="スキルの説明（任意）" style={{ resize: 'vertical' }} />
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

// ─── Page ────────────────────────────────────────────────────────────────────

type TabKey = 'categories' | 'skills';

export default function ItSkillMasterPage() {
  const [activeTab, setActiveTab] = useState<TabKey>('categories');
  const [categories, setCategories] = useState<ItSkillCategory[]>([]);
  const [skills, setSkills] = useState<ItSkill[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadAll = () => {
    setLoading(true);
    Promise.all([getItSkillCategories(), getItSkills()])
      .then(([catRes, skillRes]) => {
        setCategories(catRes.data);
        setSkills(skillRes.data);
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
            IT分類管理（SCR-012）
          </button>
          <button className={`tab-btn${activeTab === 'skills' ? ' active' : ''}`}
            onClick={() => setActiveTab('skills')}>
            ITスキル管理（SCR-009）
          </button>
        </div>

        <section className="master-card">
          {activeTab === 'categories' ? (
            <CategoryTab categories={categories} onReload={loadAll} />
          ) : (
            <SkillTab skills={skills} categories={categories} onReload={loadAll} />
          )}
        </section>
      </main>
    </div>
  );
}
