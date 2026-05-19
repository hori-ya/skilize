import { useEffect, useState } from 'react';
import NavBar from '../../../app/layouts/NavBar';
import type { UserAdmin, Role } from '../../auth/types/index';
import { getUsers, createUser, updateUser, resetUserPassword } from '../../team/api/userApi';
import { IconPlus, IconEdit, IconKey, IconX, IconCheck, IconReset } from '../../../shared/ui/Icons';
import StickyHorizontalScroll from '../../../shared/ui/StickyHorizontalScroll';

type ModalMode = 'create' | 'edit';

const ROLES: { value: Role; label: string }[] = [
  { value: 'GENERAL', label: '一般' },
  { value: 'TL',      label: 'TL' },
  { value: 'ADMIN',   label: '管理者' },
];

const roleLabel = (role: string) => ROLES.find(r => r.value === role)?.label ?? role;

interface CreateForm {
  userId: string;
  name: string;
  email: string;
  role: Role;
  tlUserId: string;
}

interface EditForm {
  name: string;
  email: string;
  role: Role;
  tlUserId: string;
  active: boolean;
}

export default function UserMasterPage() {
  const [users, setUsers] = useState<UserAdmin[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [modalMode, setModalMode] = useState<ModalMode>('create');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  const [createForm, setCreateForm] = useState<CreateForm>({
    userId: '', name: '', email: '', role: 'GENERAL', tlUserId: '',
  });
  const [editForm, setEditForm] = useState<EditForm>({
    name: '', email: '', role: 'GENERAL', tlUserId: '', active: true,
  });

  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const [resetTargetId, setResetTargetId] = useState<number | null>(null);
  const [resetTargetName, setResetTargetName] = useState('');
  const [resetResult, setResetResult] = useState('');
  const [resetting, setResetting] = useState(false);

  const loadUsers = () => {
    setLoading(true);
    getUsers()
      .then(res => setUsers(res.data))
      .catch(() => setError('ユーザー一覧の取得に失敗しました'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { loadUsers(); }, []);

  const tlCandidates = users.filter(u => u.isActive);

  const openCreate = () => {
    setCreateForm({ userId: '', name: '', email: '', role: 'GENERAL', tlUserId: '' });
    setFormError('');
    setModalMode('create');
    setModalOpen(true);
  };

  const openEdit = (u: UserAdmin) => {
    setEditForm({
      name: u.name,
      email: u.email ?? '',
      role: u.role,
      tlUserId: u.tlUserId != null ? String(u.tlUserId) : '',
      active: u.isActive,
    });
    setFormError('');
    setModalMode('edit');
    setEditingId(u.id);
    setModalOpen(true);
  };

  const handleCreate = async () => {
    if (!createForm.userId.trim()) { setFormError('ユーザーIDは必須です'); return; }
    if (!createForm.name.trim())   { setFormError('名前は必須です'); return; }
    setSaving(true); setFormError('');
    try {
      await createUser({
        userId: createForm.userId,
        name: createForm.name,
        email: createForm.email || null,
        role: createForm.role,
        tlUserId: createForm.tlUserId ? Number(createForm.tlUserId) : null,
      });
      setModalOpen(false);
      loadUsers();
    } catch (e: unknown) {
      const msg = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail;
      setFormError(msg ?? '作成に失敗しました');
    } finally { setSaving(false); }
  };

  const handleEdit = async () => {
    if (!editForm.name.trim()) { setFormError('名前は必須です'); return; }
    setSaving(true); setFormError('');
    try {
      await updateUser(editingId!, {
        name: editForm.name,
        email: editForm.email || null,
        role: editForm.role,
        tlUserId: editForm.tlUserId ? Number(editForm.tlUserId) : null,
        active: editForm.active,
      });
      setModalOpen(false);
      loadUsers();
    } catch (e: unknown) {
      const msg = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail;
      setFormError(msg ?? '更新に失敗しました');
    } finally { setSaving(false); }
  };

  const openReset = (u: UserAdmin) => {
    setResetTargetId(u.id);
    setResetTargetName(u.name);
    setResetResult('');
  };

  const handleReset = async () => {
    if (!resetTargetId) return;
    setResetting(true);
    try {
      const res = await resetUserPassword(resetTargetId);
      setResetResult(res.data.temporaryPassword);
      loadUsers();
    } catch {
      setResetResult('エラーが発生しました');
    } finally { setResetting(false); }
  };

  if (loading) return <div className="loading-screen"><span>読み込み中...</span></div>;

  return (
    <div className="master-page">
      <NavBar />

      <main className="master-main">
        {error && <div className="alert alert--error">{error}</div>}

        <section className="master-card">
          <div className="master-card__header">
            <h2 className="master-card__title">ユーザー一覧</h2>
            <button className="btn btn--primary btn--sm" onClick={openCreate}><IconPlus size={12} />ユーザー追加</button>
          </div>

          <StickyHorizontalScroll className="master-table-wrap">
            <table className="master-table">
              <thead>
                <tr>
                  <th>ユーザーID</th>
                  <th>名前</th>
                  <th>メール</th>
                  <th style={{ width: 80 }}>権限</th>
                  <th style={{ width: 120 }}>TL</th>
                  <th style={{ width: 72 }}>状態</th>
                  <th style={{ width: 72 }}>初期PW</th>
                  <th style={{ width: 140 }}>操作</th>
                </tr>
              </thead>
              <tbody>
                {users.length === 0 ? (
                  <tr><td colSpan={8} className="master-table__empty">データがありません</td></tr>
                ) : (
                  users.map(u => (
                    <tr key={u.id}>
                      <td style={{ fontFamily: 'monospace' }}>{u.userId}</td>
                      <td>{u.name}</td>
                      <td style={{ fontSize: 13, color: 'var(--color-text-muted)' }}>{u.email ?? '—'}</td>
                      <td>
                        <span className={`role-badge role-badge--${u.role.toLowerCase()}`}>
                          {roleLabel(u.role)}
                        </span>
                      </td>
                      <td style={{ fontSize: 13, color: 'var(--color-text-muted)' }}>{u.tlName ?? '—'}</td>
                      <td>
                        <span className={u.isActive ? 'fy-status fy-status--active' : 'fy-status fy-status--inactive'}>
                          {u.isActive ? '有効' : '無効'}
                        </span>
                      </td>
                      <td style={{ textAlign: 'center' }}>
                        {u.isInitialPassword && <span className="fy-status fy-status--planned">未変更</span>}
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: 6 }}>
                          <button className="btn btn--secondary btn--sm" onClick={() => openEdit(u)}><IconEdit size={12} />編集</button>
                          <button className="btn btn--secondary btn--sm" onClick={() => openReset(u)}>
                            <IconKey size={12} />PW reset
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </StickyHorizontalScroll>
        </section>
      </main>

      {/* Create / Edit modal */}
      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal" style={{ width: 560 }} onClick={e => e.stopPropagation()}>
            <div className="modal__header">
              <h3>{modalMode === 'create' ? 'ユーザー追加' : 'ユーザー編集'}</h3>
              <button className="modal__close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal__body">
              {formError && <div className="alert alert--error">{formError}</div>}

              {modalMode === 'create' ? (
                <>
                  <div className="form-group">
                    <label className="form-label">ユーザーID <span className="required">*</span></label>
                    <input className="form-input" value={createForm.userId}
                      onChange={e => setCreateForm(f => ({ ...f, userId: e.target.value }))}
                      placeholder="例: yamada_taro" maxLength={50} />
                    <p className="form-hint">初期パスワードはユーザーIDと同じになります</p>
                  </div>
                  <div className="form-group">
                    <label className="form-label">名前 <span className="required">*</span></label>
                    <input className="form-input" value={createForm.name}
                      onChange={e => setCreateForm(f => ({ ...f, name: e.target.value }))}
                      placeholder="例: 山田 太郎" maxLength={100} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">メールアドレス</label>
                    <input type="email" className="form-input" value={createForm.email}
                      onChange={e => setCreateForm(f => ({ ...f, email: e.target.value }))}
                      placeholder="例: yamada@example.com" />
                  </div>
                  <div className="form-row">
                    <div className="form-group">
                      <label className="form-label">権限</label>
                      <select className="master-select" style={{ width: '100%' }}
                        value={createForm.role}
                        onChange={e => setCreateForm(f => ({ ...f, role: e.target.value as Role }))}>
                        {ROLES.map(r => <option key={r.value} value={r.value}>{r.label}</option>)}
                      </select>
                    </div>
                    <div className="form-group">
                      <label className="form-label">TL（上長）</label>
                      <select className="master-select" style={{ width: '100%' }}
                        value={createForm.tlUserId}
                        onChange={e => setCreateForm(f => ({ ...f, tlUserId: e.target.value }))}>
                        <option value="">なし</option>
                        {tlCandidates.map(u => (
                          <option key={u.id} value={u.id}>{u.name}（{u.userId}）</option>
                        ))}
                      </select>
                    </div>
                  </div>
                </>
              ) : (
                <>
                  <div className="form-group">
                    <label className="form-label">名前 <span className="required">*</span></label>
                    <input className="form-input" value={editForm.name}
                      onChange={e => setEditForm(f => ({ ...f, name: e.target.value }))}
                      maxLength={100} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">メールアドレス</label>
                    <input type="email" className="form-input" value={editForm.email}
                      onChange={e => setEditForm(f => ({ ...f, email: e.target.value }))} />
                  </div>
                  <div className="form-row">
                    <div className="form-group">
                      <label className="form-label">権限</label>
                      <select className="master-select" style={{ width: '100%' }}
                        value={editForm.role}
                        onChange={e => setEditForm(f => ({ ...f, role: e.target.value as Role }))}>
                        {ROLES.map(r => <option key={r.value} value={r.value}>{r.label}</option>)}
                      </select>
                    </div>
                    <div className="form-group">
                      <label className="form-label">TL（上長）</label>
                      <select className="master-select" style={{ width: '100%' }}
                        value={editForm.tlUserId}
                        onChange={e => setEditForm(f => ({ ...f, tlUserId: e.target.value }))}>
                        <option value="">なし</option>
                        {tlCandidates
                          .filter(u => u.id !== editingId)
                          .map(u => (
                            <option key={u.id} value={u.id}>{u.name}（{u.userId}）</option>
                          ))}
                      </select>
                    </div>
                  </div>
                  <div className="form-group">
                    <label className="form-check">
                      <input type="checkbox" checked={editForm.active}
                        onChange={e => setEditForm(f => ({ ...f, active: e.target.checked }))} />
                      <span>有効</span>
                    </label>
                  </div>
                </>
              )}
            </div>
            <div className="modal__footer">
              <button className="btn btn--secondary" onClick={() => setModalOpen(false)}><IconX size={13} />キャンセル</button>
              <button className="btn btn--primary"
                onClick={modalMode === 'create' ? handleCreate : handleEdit}
                disabled={saving}>
                <IconCheck size={13} />{saving ? '保存中...' : '保存'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Reset password dialog */}
      {resetTargetId !== null && (
        <div className="modal-overlay" onClick={() => { setResetTargetId(null); setResetResult(''); }}>
          <div className="modal" style={{ width: 420 }} onClick={e => e.stopPropagation()}>
            <div className="modal__header">
              <h3>パスワードリセット</h3>
              <button className="modal__close" onClick={() => { setResetTargetId(null); setResetResult(''); }}>×</button>
            </div>
            <div className="modal__body">
              {resetResult ? (
                <div className="reset-result">
                  <p>パスワードをリセットしました。</p>
                  <p>仮パスワードをユーザーに通知してください：</p>
                  <div className="reset-password-box">{resetResult}</div>
                </div>
              ) : (
                <p>
                  <strong>{resetTargetName}</strong> のパスワードをリセットします。<br />
                  ユーザーは次回ログイン時に新しいパスワードへ変更が必要になります。
                </p>
              )}
            </div>
            <div className="modal__footer">
              <button className="btn btn--secondary" onClick={() => { setResetTargetId(null); setResetResult(''); }}>
                <IconX size={13} />{resetResult ? '閉じる' : 'キャンセル'}
              </button>
              {!resetResult && (
                <button className="btn btn--danger" onClick={handleReset} disabled={resetting}>
                  <IconReset size={13} />{resetting ? '処理中...' : 'リセットする'}
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
