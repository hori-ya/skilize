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

import java.util.ArrayList;
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
        Optional<QualificationEntity> entityOptional = jpaRepository.findById(id);
        if (entityOptional.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapper.toDomain(entityOptional.get()));
    }

    @Override
    public Qualification save(Qualification qualification) {
        QualificationCategoryEntity categoryEntity = null;
        if (qualification.getCategory() != null) {
            Optional<QualificationCategoryEntity> categoryOptional =
                    categoryJpaRepository.findById(qualification.getCategory().getId());
            if (categoryOptional.isEmpty()) {
                throw new IllegalStateException("QualificationCategory not found: id=" + qualification.getCategory().getId());
            }
            categoryEntity = categoryOptional.get();
        }
        QualificationEntity entity;
        if (qualification.getId() == null) {
            entity = QualificationEntity.create(categoryEntity, qualification.getName(), qualification.getDescription(), qualification.getSortOrder());
        } else {
            Optional<QualificationEntity> entityOptional = jpaRepository.findById(qualification.getId());
            if (entityOptional.isEmpty()) {
                throw new IllegalStateException("Qualification not found: id=" + qualification.getId());
            }
            entity = entityOptional.get();
            entity.update(categoryEntity, qualification.getName(), qualification.getDescription(), qualification.getSortOrder(), qualification.isActive());
        }
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<Qualification> findAll() {
        List<Qualification> qualifications = new ArrayList<>();
        for (QualificationEntity entity : jpaRepository.findAll()) {
            qualifications.add(mapper.toDomain(entity));
        }
        return qualifications;
    }

    @Override
    public List<Qualification> findAllWithCategory() {
        List<Qualification> qualifications = new ArrayList<>();
        for (QualificationEntity entity : jpaRepository.findAllWithCategory()) {
            qualifications.add(mapper.toDomain(entity));
        }
        return qualifications;
    }

    @Override
    public List<Qualification> findAllActiveWithCategory() {
        List<Qualification> qualifications = new ArrayList<>();
        for (QualificationEntity entity : jpaRepository.findAllActiveWithCategory()) {
            qualifications.add(mapper.toDomain(entity));
        }
        return qualifications;
    }

    @Override
    public List<Qualification> findAllWithCategoryByActive(boolean active) {
        List<Qualification> qualifications = new ArrayList<>();
        for (QualificationEntity entity : jpaRepository.findAllWithCategoryByActive(active)) {
            qualifications.add(mapper.toDomain(entity));
        }
        return qualifications;
    }
}
