import { useEffect, useState } from 'react';
import NavBar from '../../components/NavBar';
import type { SkillLevel } from '../../types/master';
import { getSkillLevels, createSkillLevel, updateSkillLevel } from '../../api/master';

type ModalMode = 'create' | 'edit';

interface FormState {
  levelValue: string;
  description: string;
  active: boolean;
}

const emptyForm = (): FormState => ({ levelValue: '', description: '', active: true });

export default function SkillLevelMasterPage() {
  const [levels, setLevels] = useState<SkillLevel[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState<ModalMode>('create');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<FormState>(emptyForm());
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    getSkillLevels()
      .then(res => setLevels(res.data))
      .catch(() => setError('データの取得に失敗しました'))
      .finally(() => setLoading(false));
  }, []);

  const openCreate = () => {
    setForm(emptyForm());
    setFormError('');
    setModalMode('create');
    setEditingId(null);
    setModalOpen(true);
  };

  const openEdit = (level: SkillLevel) => {
    setForm({
      levelValue: String(level.levelValue),
      description: level.description,
      active: level.isActive,
    });
    setFormError('');
    setModalMode('edit');
    setEditingId(level.id);
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    const lv = Number(form.levelValue);
    if (!form.levelValue || isNaN(lv) || lv < 1) {
      setFormError('レベル値は1以上の整数を入力してください');
      return;
    }
    if (!form.description.trim()) {
      setFormError('説明は必須です');
      return;
    }

    setSaving(true);
    setFormError('');
    try {
      if (modalMode === 'create') {
        const res = await createSkillLevel({ levelValue: lv, description: form.description });
        setLevels(prev => [...prev, res.data].sort((a, b) => a.levelValue - b.levelValue));
      } else {
        const res = await updateSkillLevel(editingId!, {
          levelValue: lv,
          description: form.description,
          active: form.active,
        });
        setLevels(prev =>
          prev.map(l => (l.id === editingId ? res.data : l))
            .sort((a, b) => a.levelValue - b.levelValue)
        );
      }
      setModalOpen(false);
    } catch (e: unknown) {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setFormError(msg ?? '保存に失敗しました');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="loading-screen"><span>読み込み中...</span></div>;

  return (
    <div className="master-page">
      <NavBar />

      <main className="master-main">
        {error && <div className="alert alert--error">{error}</div>}

        <section className="master-card">
          <div className="master-card__header">
            <h2 className="master-card__title">スキルレベル一覧</h2>
            <button className="btn btn--primary btn--sm" onClick={openCreate}>+ レベル追加</button>
          </div>

          <div className="master-table-wrap">
            <table className="master-table">
              <thead>
                <tr>
                  <th style={{ width: 100 }}>レベル値</th>
                  <th>説明</th>
                  <th style={{ width: 80 }}>状態</th>
                  <th style={{ width: 80 }}>操作</th>
                </tr>
              </thead>
              <tbody>
                {levels.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="master-table__empty">データがありません</td>
                  </tr>
                ) : (
                  levels.map(level => (
                    <tr key={level.id}>
                      <td style={{ textAlign: 'center', fontWeight: 600 }}>{level.levelValue}</td>
                      <td>{level.description}</td>
                      <td>
                        <span className={level.isActive ? 'fy-status fy-status--active' : 'fy-status fy-status--inactive'}>
                          {level.isActive ? '有効' : '無効'}
                        </span>
                      </td>
                      <td>
                        <button className="btn btn--secondary btn--sm" onClick={() => openEdit(level)}>
                          編集
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>
      </main>

      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal__header">
              <h3>{modalMode === 'create' ? 'レベル追加' : 'レベル編集'}</h3>
              <button className="modal__close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal__body">
              {formError && <div className="alert alert--error">{formError}</div>}

              <div className="form-group">
                <label className="form-label">レベル値 <span className="required">*</span></label>
                <input
                  type="number"
                  className="form-input"
                  value={form.levelValue}
                  onChange={e => setForm(f => ({ ...f, levelValue: e.target.value }))}
                  min={1}
                  max={99}
                  style={{ width: 120 }}
                />
              </div>

              <div className="form-group">
                <label className="form-label">説明 <span className="required">*</span></label>
                <input
                  className="form-input"
                  value={form.description}
                  onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                  placeholder="例: 独力で実務に適用できる"
                  maxLength={200}
                />
              </div>

              {modalMode === 'edit' && (
                <div className="form-group">
                  <label className="form-check">
                    <input
                      type="checkbox"
                      checked={form.active}
                      onChange={e => setForm(f => ({ ...f, active: e.target.checked }))}
                    />
                    <span>有効</span>
                  </label>
                </div>
              )}
            </div>
            <div className="modal__footer">
              <button className="btn btn--secondary" onClick={() => setModalOpen(false)}>
                キャンセル
              </button>
              <button className="btn btn--primary" onClick={handleSubmit} disabled={saving}>
                {saving ? '保存中...' : '保存'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
