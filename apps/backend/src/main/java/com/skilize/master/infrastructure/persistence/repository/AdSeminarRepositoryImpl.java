/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * domain.repository.AdSeminarRepository の実装クラス。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.persistence.repository;

import com.skilize.master.domain.model.AdSeminar;
import com.skilize.master.domain.repository.AdSeminarRepository;
import com.skilize.master.infrastructure.persistence.entity.AdSeminarCategoryEntity;
import com.skilize.master.infrastructure.persistence.entity.AdSeminarEntity;
import com.skilize.master.infrastructure.persistence.mapper.AdSeminarPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** domain.repository.AdSeminarRepository の実装。 */
@Repository
@RequiredArgsConstructor
public class AdSeminarRepositoryImpl implements AdSeminarRepository {

    private final AdSeminarJpaRepository jpaRepository;
    private final AdSeminarCategoryJpaRepository categoryJpaRepository;
    private final AdSeminarPersistenceMapper mapper;

    @Override
    public Optional<AdSeminar> findById(Integer id) {
        Optional<AdSeminarEntity> entityOptional = jpaRepository.findById(id);
        if (entityOptional.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapper.toDomain(entityOptional.get()));
    }

    @Override
    public AdSeminar save(AdSeminar adSeminar) {
        AdSeminarCategoryEntity categoryEntity = null;
        if (adSeminar.getCategory() != null) {
            Optional<AdSeminarCategoryEntity> categoryOptional =
                    categoryJpaRepository.findById(adSeminar.getCategory().getId());
            if (categoryOptional.isEmpty()) {
                throw new IllegalStateException("AdSeminarCategory not found: id=" + adSeminar.getCategory().getId());
            }
            categoryEntity = categoryOptional.get();
        }
        AdSeminarEntity entity;
        if (adSeminar.getId() == null) {
            entity = AdSeminarEntity.create(categoryEntity, adSeminar.getName(), adSeminar.getDescription(), adSeminar.getSortOrder());
        } else {
            Optional<AdSeminarEntity> entityOptional = jpaRepository.findById(adSeminar.getId());
            if (entityOptional.isEmpty()) {
                throw new IllegalStateException("AdSeminar not found: id=" + adSeminar.getId());
            }
            entity = entityOptional.get();
            entity.update(categoryEntity, adSeminar.getName(), adSeminar.getDescription(), adSeminar.getSortOrder(), adSeminar.isActive());
        }
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<AdSeminar> findAll() {
        List<AdSeminar> seminars = new ArrayList<>();
        for (AdSeminarEntity entity : jpaRepository.findAll()) {
            seminars.add(mapper.toDomain(entity));
        }
        return seminars;
    }

    @Override
    public List<AdSeminar> findAllWithCategory() {
        List<AdSeminar> seminars = new ArrayList<>();
        for (AdSeminarEntity entity : jpaRepository.findAllWithCategory()) {
            seminars.add(mapper.toDomain(entity));
        }
        return seminars;
    }

    @Override
    public List<AdSeminar> findAllActiveWithCategory() {
        List<AdSeminar> seminars = new ArrayList<>();
        for (AdSeminarEntity entity : jpaRepository.findAllActiveWithCategory()) {
            seminars.add(mapper.toDomain(entity));
        }
        return seminars;
    }

    @Override
    public List<AdSeminar> findAllWithCategoryByActive(boolean active) {
        List<AdSeminar> seminars = new ArrayList<>();
        for (AdSeminarEntity entity : jpaRepository.findAllWithCategoryByActive(active)) {
            seminars.add(mapper.toDomain(entity));
        }
        return seminars;
    }
}
