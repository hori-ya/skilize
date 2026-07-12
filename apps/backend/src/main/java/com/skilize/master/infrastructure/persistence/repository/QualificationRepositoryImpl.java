/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * domain.repository.QualificationRepository の実装クラス。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.persistence.repository;

import com.skilize.master.domain.model.Qualification;
import com.skilize.master.domain.repository.QualificationRepository;
import com.skilize.master.infrastructure.persistence.entity.QualificationCategoryEntity;
import com.skilize.master.infrastructure.persistence.entity.QualificationEntity;
import com.skilize.master.infrastructure.persistence.mapper.QualificationPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** domain.repository.QualificationRepository の実装。 */
@Repository
@RequiredArgsConstructor
public class QualificationRepositoryImpl implements QualificationRepository {

    private final QualificationJpaRepository jpaRepository;
    private final QualificationCategoryJpaRepository categoryJpaRepository;
    private final QualificationPersistenceMapper mapper;

    @Override
    public Optional<Qualification> findById(Integer id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Qualification save(Qualification qualification) {
        QualificationCategoryEntity categoryEntity = qualification.getCategory() != null
                ? categoryJpaRepository.findById(qualification.getCategory().getId())
                        .orElseThrow(() -> new IllegalStateException("QualificationCategory not found: id=" + qualification.getCategory().getId()))
                : null;
        QualificationEntity entity;
        if (qualification.getId() == null) {
            entity = QualificationEntity.create(categoryEntity, qualification.getName(), qualification.getDescription(), qualification.getSortOrder());
        } else {
            entity = jpaRepository.findById(qualification.getId())
                    .orElseThrow(() -> new IllegalStateException("Qualification not found: id=" + qualification.getId()));
            entity.update(categoryEntity, qualification.getName(), qualification.getDescription(), qualification.getSortOrder(), qualification.isActive());
        }
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<Qualification> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Qualification> findAllWithCategory() {
        return jpaRepository.findAllWithCategory().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Qualification> findAllActiveWithCategory() {
        return jpaRepository.findAllActiveWithCategory().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Qualification> findAllWithCategoryByActive(boolean active) {
        return jpaRepository.findAllWithCategoryByActive(active).stream().map(mapper::toDomain).toList();
    }
}
