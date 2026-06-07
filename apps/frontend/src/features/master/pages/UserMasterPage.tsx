import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import NavBar from '../../../app/layouts/NavBar';
import type { UserAdmin, Role } from '../../auth/types/index';
import { getUsers, createUser, updateUser, resetUserPassword } from '../../user/api/userApi';
import { IconPlus, IconEdit, IconKey, IconX, IconCheck, IconReset } from '../../../shared/ui/Icons';
import StickyHorizontalScroll from '../../../shared/ui/StickyHorizontalScroll';

type ModalMode = 'create' | 'edit';

const ROLE_KEY: Record<string, string> = {
  GENERAL: 'user.role.general',
  TL: 'user.role.tl',
  ADMIN: 'user.role.admin',
};
const ROLE_VALUES: Role[] = ['GENERAL', 'TL', 'ADMIN'];

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
  const { t } = useTranslation('master');
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

  const getRoleLabel = (role: string) => t(ROLE_KEY[role] ?? role);

  const loadUsers = () => {
    setLoading(true);
    getUsers()
      .then(res => setUsers(res.data))
      .catch(() => setError(t('user.loadFailed')))
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
    if (!createForm.userId.trim()) { setFormError(t('user.validation.userIdRequired')); return; }
    if (!createForm.name.trim())   { setFormError(t('user.validation.nameRequired')); return; }
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
      setFormError(msg ?? t('common.createFailed'));
    } finally { setSaving(false); }
  };

  const handleEdit = async () => {
    if (!editForm.name.trim()) { setFormError(t('user.validation.nameRequired')); return; }
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
      setFormError(msg ?? t('common.updateFailed'));
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
      setResetResult(t('user.resetPassword.errorMessage'));
    } finally { setResetting(false); }
  };

  if (loading) return <div className="loading-screen"><span>{t('loading')}</span></div>;

  return (
    <div className="master-page">
      <NavBar />

      <main className="master-main">
        {error && <div className="alert alert--error">{error}</div>}

        <section className="master-card">
          <div className="master-card__header">
            <h2 className="master-card__title">{t('user.listTitle')}</h2>
            <button className="btn btn--primary btn--sm" onClick={openCreate}>
              <IconPlus size={12} />{t('user.addButton')}
            </button>
          </div>

          <StickyHorizontalScroll className="master-table-wrap">
            <table className="master-table">
              <thead>
                <tr>
                  <th>{t('user.table.userId')}</th>
                  <th>{t('user.table.name')}</th>
                  <th>{t('user.table.email')}</th>
                  <th style={{ width: 80 }}>{t('user.table.role')}</th>
                  <th style={{ width: 120 }}>{t('user.table.tl')}</th>
                  <th style={{ width: 72 }}>{t('common.status')}</th>
                  <th style={{ width: 72 }}>{t('user.table.initialPw')}</th>
                  <th style={{ width: 140 }}>{t('common.actions')}</th>
                </tr>
              </thead>
              <tbody>
                {users.length === 0 ? (
                  <tr><td colSpan={8} className="master-table__empty">{t('common.noData')}</td></tr>
                ) : (
                  users.map(u => (
                    <tr key={u.id}>
                      <td style={{ fontFamily: 'monospace' }}>{u.userId}</td>
                      <td>{u.name}</td>
                      <td style={{ fontSize: 13, color: 'var(--color-text-muted)' }}>{u.email ?? '—'}</td>
                      <td>
                        <span className={`role-badge role-badge--${u.role.toLowerCase()}`}>
                          {getRoleLabel(u.role)}
                        </span>
                      </td>
                      <td style={{ fontSize: 13, color: 'var(--color-text-muted)' }}>{u.tlName ?? '—'}</td>
                      <td>
                        <span className={u.isActive ? 'fy-status fy-status--active' : 'fy-status fy-status--inactive'}>
                          {u.isActive ? t('common.activeLabel') : t('common.inactiveLabel')}
                        </span>
                      </td>
                      <td style={{ textAlign: 'center' }}>
                        {u.isInitialPassword && (
                          <span className="fy-status fy-status--planned">{t('user.table.pwNotChanged')}</span>
                        )}
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: 6 }}>
                          <button className="btn btn--secondary btn--sm" onClick={() => openEdit(u)}>
                            <IconEdit size={12} />{t('common.edit')}
                          </button>
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
              <h3>{modalMode === 'create' ? t('user.modalCreate') : t('user.modalEdit')}</h3>
              <button className="modal__close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal__body">
              {formError && <div className="alert alert--error">{formError}</div>}

              {modalMode === 'create' ? (
                <>
                  <div className="form-group">
                    <label className="form-label">{t('user.form.userIdLabel')} <span className="required">*</span></label>
                    <input className="form-input" value={createForm.userId}
                      onChange={e => setCreateForm(f => ({ ...f, userId: e.target.value }))}
                      placeholder={t('user.form.userIdPlaceholder')} maxLength={50} />
                    <p className="form-hint">{t('user.form.initialPasswordHint')}</p>
                  </div>
                  <div className="form-group">
                    <label className="form-label">{t('user.form.nameLabel')} <span className="required">*</span></label>
                    <input className="form-input" value={createForm.name}
                      onChange={e => setCreateForm(f => ({ ...f, name: e.target.value }))}
                      placeholder={t('user.form.namePlaceholder')} maxLength={100} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">{t('user.form.emailLabel')}</label>
                    <input type="email" className="form-input" value={createForm.email}
                      onChange={e => setCreateForm(f => ({ ...f, email: e.target.value }))}
                      placeholder={t('user.form.emailPlaceholder')} />
                  </div>
                  <div className="form-row">
                    <div className="form-group">
                      <label className="form-label">{t('user.form.roleLabel')}</label>
                      <select className="master-select" style={{ width: '100%' }}
                        value={createForm.role}
                        onChange={e => setCreateForm(f => ({ ...f, role: e.target.value as Role }))}>
                        {ROLE_VALUES.map(r => <option key={r} value={r}>{t(ROLE_KEY[r])}</option>)}
                      </select>
                    </div>
                    <div className="form-group">
                      <label className="form-label">{t('user.form.tlLabel')}</label>
                      <select className="master-select" style={{ width: '100%' }}
                        value={createForm.tlUserId}
                        onChange={e => setCreateForm(f => ({ ...f, tlUserId: e.target.value }))}>
                        <option value="">{t('user.form.tlNone')}</option>
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
                    <label className="form-label">{t('user.form.nameLabel')} <span className="required">*</span></label>
                    <input className="form-input" value={editForm.name}
                      onChange={e => setEditForm(f => ({ ...f, name: e.target.value }))}
                      maxLength={100} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">{t('user.form.emailLabel')}</label>
                    <input type="email" className="form-input" value={editForm.email}
                      onChange={e => setEditForm(f => ({ ...f, email: e.target.value }))} />
                  </div>
                  <div className="form-row">
                    <div className="form-group">
                      <label className="form-label">{t('user.form.roleLabel')}</label>
                      <select className="master-select" style={{ width: '100%' }}
                        value={editForm.role}
                        onChange={e => setEditForm(f => ({ ...f, role: e.target.value as Role }))}>
                        {ROLE_VALUES.map(r => <option key={r} value={r}>{t(ROLE_KEY[r])}</option>)}
                      </select>
                    </div>
                    <div className="form-group">
                      <label className="form-label">{t('user.form.tlLabel')}</label>
                      <select className="master-select" style={{ width: '100%' }}
                        value={editForm.tlUserId}
                        onChange={e => setEditForm(f => ({ ...f, tlUserId: e.target.value }))}>
                        <option value="">{t('user.form.tlNone')}</option>
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
                      <span>{t('user.form.activeLabel')}</span>
                    </label>
                  </div>
                </>
              )}
            </div>
            <div className="modal__footer">
              <button className="btn btn--secondary" onClick={() => setModalOpen(false)}>
                <IconX size={13} />{t('common.cancel')}
              </button>
              <button className="btn btn--primary"
                onClick={modalMode === 'create' ? handleCreate : handleEdit}
                disabled={saving}>
                <IconCheck size={13} />{saving ? t('common.saving') : t('common.save')}
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
              <h3>{t('user.resetPasswordTitle')}</h3>
              <button className="modal__close" onClick={() => { setResetTargetId(null); setResetResult(''); }}>×</button>
            </div>
            <div className="modal__body">
              {resetResult ? (
                <div className="reset-result">
                  <p>{t('user.resetPassword.successMessage')}</p>
                  <p>{t('user.resetPassword.tempPasswordMessage')}</p>
                  <div className="reset-password-box">{resetResult}</div>
                </div>
              ) : (
                <p>
                  <strong>{resetTargetName}</strong>{t('user.resetPassword.confirmMessage')}
                </p>
              )}
            </div>
            <div className="modal__footer">
              <button className="btn btn--secondary" onClick={() => { setResetTargetId(null); setResetResult(''); }}>
                <IconX size={13} />{resetResult ? t('user.resetPassword.closeButton') : t('common.cancel')}
              </button>
              {!resetResult && (
                <button className="btn btn--danger" onClick={handleReset} disabled={resetting}>
                  <IconReset size={13} />{resetting ? t('user.resetPassword.resettingButton') : t('user.resetPassword.resetButton')}
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
