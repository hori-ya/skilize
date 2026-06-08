/*******************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ---------------------------------------------------------------------------
 * 機能概要：
 * スキルレベルマスタ管理ページ。スキルレベルの一覧表示・新規作成・編集を行う。
 * ADMIN ロールのみアクセス可能。
 * ---------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ---------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 *******************************************************************************/
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import NavBar from '../../../app/layouts/NavBar';
import type { SkillLevel } from '../../../shared/types/master';
import { getSkillLevels, createSkillLevel, updateSkillLevel } from '../../../shared/api/masterApi';
import { IconPlus, IconEdit, IconX, IconCheck } from '../../../shared/ui/Icons';
import StickyHorizontalScroll from '../../../shared/ui/StickyHorizontalScroll';

type ModalMode = 'create' | 'edit';

interface FormState {
  levelValue: string;
  description: string;
  scoreWeight: string;
  active: boolean;
}

const emptyForm = (): FormState => ({ levelValue: '', description: '', scoreWeight: '0', active: true });

/**
 * スキルレベルマスタ管理ページ。
 *
 * スキルレベルの一覧表示・新規作成・編集を行う。
 * ADMIN ロールのみアクセス可能。
 */
export default function SkillLevelMasterPage() {
  const { t } = useTranslation('master');
  const [levels, setLevels] = useState<SkillLevel[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [modalOpen, setModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState<ModalMode>('create');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<FormState>(emptyForm());
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  // 初期表示時にスキルレベル一覧を取得する
  useEffect(() => {
    getSkillLevels()
      .then(res => setLevels(res.data))
      .catch(() => setError(t('common.loadFailed')))
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
      scoreWeight: String(level.scoreWeight),
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
      setFormError(t('skillLevel.validation.levelValueRange'));
      return;
    }
    if (!form.description.trim()) {
      setFormError(t('skillLevel.validation.descriptionRequired'));
      return;
    }

    const sw = Number(form.scoreWeight);
    if (form.scoreWeight === '' || isNaN(sw) || sw < 0) {
      setFormError(t('skillLevel.validation.scoreWeightRange'));
      return;
    }

    setSaving(true);
    setFormError('');
    try {
      if (modalMode === 'create') {
        const res = await createSkillLevel({ levelValue: lv, description: form.description, scoreWeight: sw });
        setLevels(prev => [...prev, res.data].sort((a, b) => a.levelValue - b.levelValue));
      } else {
        const res = await updateSkillLevel(editingId!, {
          levelValue: lv,
          description: form.description,
          active: form.active,
          scoreWeight: sw,
        });
        setLevels(prev =>
          prev.map(l => (l.id === editingId ? res.data : l))
            .sort((a, b) => a.levelValue - b.levelValue)
        );
      }
      setModalOpen(false);
    } catch (e: unknown) {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setFormError(msg ?? t('common.saveFailed'));
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="loading-screen"><span>{t('loading')}</span></div>;

  return (
    <div className="master-page">
      <NavBar />

      <main className="master-main">
        {error && <div className="alert alert--error">{error}</div>}

        <section className="master-card">
          <div className="master-card__header">
            <h2 className="master-card__title">{t('skillLevel.listTitle')}</h2>
            <button className="btn btn--primary btn--sm" onClick={openCreate}>
              <IconPlus size={12} />{t('skillLevel.addButton')}
            </button>
          </div>

          <StickyHorizontalScroll className="master-table-wrap">
            <table className="master-table">
              <thead>
                <tr>
                  <th style={{ width: 100 }}>{t('skillLevel.table.levelValue')}</th>
                  <th>{t('skillLevel.table.description')}</th>
                  <th style={{ width: 100 }}>{t('skillLevel.table.scoreWeight')}</th>
                  <th style={{ width: 80 }}>{t('common.status')}</th>
                  <th style={{ width: 80 }}>{t('common.actions')}</th>
                </tr>
              </thead>
              <tbody>
                {levels.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="master-table__empty">{t('common.noData')}</td>
                  </tr>
                ) : (
                  levels.map(level => (
                    <tr key={level.id}>
                      <td style={{ textAlign: 'center', fontWeight: 600 }}>{level.levelValue}</td>
                      <td>{level.description}</td>
                      <td style={{ textAlign: 'center' }}>{level.scoreWeight}</td>
                      <td>
                        <span className={level.isActive ? 'fy-status fy-status--active' : 'fy-status fy-status--inactive'}>
                          {level.isActive ? t('common.activeLabel') : t('common.inactiveLabel')}
                        </span>
                      </td>
                      <td>
                        <button className="btn btn--secondary btn--sm" onClick={() => openEdit(level)}>
                          <IconEdit size={12} />{t('common.edit')}
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </StickyHorizontalScroll>
        </section>
      </main>

      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal__header">
              <h3>{modalMode === 'create' ? t('skillLevel.modalCreate') : t('skillLevel.modalEdit')}</h3>
              <button className="modal__close" onClick={() => setModalOpen(false)}>×</button>
            </div>
            <div className="modal__body">
              {formError && <div className="alert alert--error">{formError}</div>}

              <div className="form-group">
                <label className="form-label">{t('skillLevel.form.levelValueLabel')} <span className="required">*</span></label>
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
                <label className="form-label">{t('skillLevel.form.descriptionLabel')} <span className="required">*</span></label>
                <input
                  className="form-input"
                  value={form.description}
                  onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                  placeholder={t('skillLevel.form.descriptionPlaceholder')}
                  maxLength={200}
                />
              </div>

              <div className="form-group">
                <label className="form-label">{t('skillLevel.form.scoreWeightLabel')} <span className="required">*</span></label>
                <input
                  type="number"
                  className="form-input"
                  value={form.scoreWeight}
                  onChange={e => setForm(f => ({ ...f, scoreWeight: e.target.value }))}
                  min={0}
                  style={{ width: 120 }}
                />
                <p className="form-hint">{t('skillLevel.form.scoreWeightHint')}</p>
              </div>

              {modalMode === 'edit' && (
                <div className="form-group">
                  <label className="form-check">
                    <input
                      type="checkbox"
                      checked={form.active}
                      onChange={e => setForm(f => ({ ...f, active: e.target.checked }))}
                    />
                    <span>{t('skillLevel.form.activeLabel')}</span>
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
        </div>
      )}
    </div>
  );
}
