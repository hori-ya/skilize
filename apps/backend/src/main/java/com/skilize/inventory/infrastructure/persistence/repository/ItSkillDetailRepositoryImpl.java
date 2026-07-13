/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * domain.repository.ItSkillDetailRepository の実装クラス。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.infrastructure.persistence.repository;

import com.skilize.inventory.domain.model.ItSkillDetail;
import com.skilize.inventory.domain.repository.ItSkillDetailRepository;
import com.skilize.inventory.infrastructure.persistence.entity.ItSkillDetailEntity;
import com.skilize.inventory.infrastructure.persistence.mapper.ItSkillDetailPersistenceMapper;
import com.skilize.master.domain.model.ItSkill;
import com.skilize.master.infrastructure.persistence.entity.ItSkillEntity;
import com.skilize.master.infrastructure.persistence.entity.SkillLevelEntity;
import com.skilize.master.infrastructure.persistence.repository.ItSkillJpaRepository;
import com.skilize.master.infrastructure.persistence.repository.SkillLevelJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** domain.repository.ItSkillDetailRepository の実装。 */
@Repository
@RequiredArgsConstructor
public class ItSkillDetailRepositoryImpl implements ItSkillDetailRepository {

    private final ItSkillDetailJpaRepository jpaRepository;
    private final InventoryJpaRepository inventoryJpaRepository;
    private final ItSkillJpaRepository itSkillJpaRepository;
    private final SkillLevelJpaRepository skillLevelJpaRepository;
    private final ItSkillDetailPersistenceMapper mapper;

    @Override
    public Optional<ItSkillDetail> findById(Integer id) {
        Optional<ItSkillDetailEntity> entityOptional = jpaRepository.findById(id);
        if (entityOptional.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapper.toDomain(entityOptional.get()));
    }

    @Override
    public ItSkillDetail save(ItSkillDetail detail) {
        return mapper.toDomain(jpaRepository.save(toEntity(detail)));
    }

    @Override
    public List<ItSkillDetail> saveAll(List<ItSkillDetail> details) {
        List<ItSkillDetailEntity> entities = new ArrayList<>();
        for (ItSkillDetail detail : details) {
            entities.add(toEntity(detail));
        }
        List<ItSkillDetail> saved = new ArrayList<>();
        for (ItSkillDetailEntity entity : jpaRepository.saveAll(entities)) {
            saved.add(mapper.toDomain(entity));
        }
        return saved;
    }

    private ItSkillDetailEntity toEntity(ItSkillDetail detail) {
        ItSkillEntity itSkillEntity = null;
        if (detail.getItSkill() != null) {
            itSkillEntity = itSkillJpaRepository.getReferenceById(detail.getItSkill().getId());
        }
        SkillLevelEntity skillLevelEntity = skillLevelJpaRepository.getReferenceById(detail.getSkillLevel().getId());
        if (detail.getId() == null) {
            return ItSkillDetailEntity.create(inventoryJpaRepository.getReferenceById(detail.getInventoryId()),
                    itSkillEntity, detail.getCustomSkillName(), skillLevelEntity, detail.getRemarks());
        }
        Optional<ItSkillDetailEntity> entityOptional = jpaRepository.findById(detail.getId());
        if (entityOptional.isEmpty()) {
            throw new IllegalStateException("ItSkillDetail not found: id=" + detail.getId());
        }
        ItSkillDetailEntity entity = entityOptional.get();
        entity.updateRemarks(detail.getRemarks());
        return entity;
    }

    @Override
    public List<ItSkillDetail> findByInventoryId(int inventoryId) {
        List<ItSkillDetail> details = new ArrayList<>();
        for (ItSkillDetailEntity entity : jpaRepository.findByInventoryId(inventoryId)) {
            details.add(mapper.toDomain(entity));
        }
        return details;
    }

    @Override
    public List<ItSkillDetail> findByInventoryIdWithCategories(int inventoryId) {
        List<ItSkillDetail> details = new ArrayList<>();
        for (ItSkillDetailEntity entity : jpaRepository.findByInventoryIdWithCategories(inventoryId)) {
            details.add(mapper.toDomain(entity));
        }
        return details;
    }

    @Override
    public void deleteByInventoryId(int inventoryId) {
        jpaRepository.deleteByInventoryId(inventoryId);
    }

    @Override
    public List<Object[]> findCustomUnregisteredSkillNames() {
        return jpaRepository.findCustomUnregisteredSkillNames();
    }

    @Override
    public void linkToMasterSkill(String customName, ItSkill skill) {
        jpaRepository.linkToMasterSkill(customName, itSkillJpaRepository.getReferenceById(skill.getId()));
    }
}
