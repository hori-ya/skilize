/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * domain.repository.SeminarCategoryRepository の実装クラス。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.persistence.repository;

import com.skilize.master.domain.model.SeminarCategory;
import com.skilize.master.domain.repository.SeminarCategoryRepository;
import com.skilize.master.infrastructure.persistence.entity.SeminarCategoryEntity;
import com.skilize.master.infrastructure.persistence.mapper.SeminarCategoryPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** domain.repository.SeminarCategoryRepository の実装。 */
@Repository
@RequiredArgsConstructor
public class SeminarCategoryRepositoryImpl implements SeminarCategoryRepository {

    private final SeminarCategoryJpaRepository jpaRepository;
    private final SeminarCategoryPersistenceMapper mapper;

    @Override
    public Optional<SeminarCategory> findById(Integer id) {
        Optional<SeminarCategoryEntity> entityOptional = jpaRepository.findById(id);
        if (entityOptional.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapper.toDomain(entityOptional.get()));
    }

    @Override
    public List<SeminarCategory> findByActiveTrueOrderBySortOrderAsc() {
        List<SeminarCategory> categories = new ArrayList<>();
        for (SeminarCategoryEntity entity : jpaRepository.findByActiveTrueOrderBySortOrderAsc()) {
            categories.add(mapper.toDomain(entity));
        }
        return categories;
    }
}
