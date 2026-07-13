/**************************************************************************************************************
 * 機能ID      ：IVW
 * 機能名      ：面談メモ
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * domain.repository.InventoryInterviewRepository の実装クラス。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.interview.infrastructure.persistence.repository;

import com.skilize.interview.domain.model.InventoryInterview;
import com.skilize.interview.domain.repository.InventoryInterviewRepository;
import com.skilize.interview.infrastructure.persistence.entity.InventoryInterviewEntity;
import com.skilize.interview.infrastructure.persistence.mapper.InventoryInterviewPersistenceMapper;
import com.skilize.inventory.infrastructure.persistence.repository.InventoryJpaRepository;
import com.skilize.user.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** domain.repository.InventoryInterviewRepository の実装。 */
@Repository
@RequiredArgsConstructor
public class InventoryInterviewRepositoryImpl implements InventoryInterviewRepository {

    private final InventoryInterviewJpaRepository jpaRepository;
    private final InventoryJpaRepository inventoryJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final InventoryInterviewPersistenceMapper mapper;

    @Override
    public InventoryInterview save(InventoryInterview interview) {
        InventoryInterviewEntity entity;
        if (interview.getId() == null) {
            entity = InventoryInterviewEntity.create(
                    inventoryJpaRepository.getReferenceById(interview.getInventoryId()),
                    userJpaRepository.getReferenceById(interview.getInterviewer().getId()),
                    interview.getGeneralNote());
        } else {
            Optional<InventoryInterviewEntity> entityOptional = jpaRepository.findById(interview.getId());
            if (entityOptional.isEmpty()) {
                throw new IllegalStateException("InventoryInterview not found: id=" + interview.getId());
            }
            entity = entityOptional.get();
            entity.update(interview.getGeneralNote());
        }
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<InventoryInterview> findByInventoryIdAndInterviewerId(int inventoryId, int interviewerId) {
        Optional<InventoryInterviewEntity> entityOptional =
                jpaRepository.findByInventoryIdAndInterviewerId(inventoryId, interviewerId);
        if (entityOptional.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapper.toDomain(entityOptional.get()));
    }
}
