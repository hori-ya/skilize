/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * domain.repository.SeminarDetailRepository の実装クラス。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.infrastructure.persistence.repository;

import com.skilize.inventory.domain.model.SeminarDetail;
import com.skilize.inventory.domain.repository.SeminarDetailRepository;
import com.skilize.inventory.infrastructure.persistence.entity.SeminarDetailEntity;
import com.skilize.inventory.infrastructure.persistence.mapper.SeminarDetailPersistenceMapper;
import com.skilize.master.infrastructure.persistence.entity.AdSeminarEntity;
import com.skilize.master.infrastructure.persistence.entity.SeminarCategoryEntity;
import com.skilize.master.infrastructure.persistence.repository.AdSeminarJpaRepository;
import com.skilize.master.infrastructure.persistence.repository.SeminarCategoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/** domain.repository.SeminarDetailRepository の実装。 */
@Repository
@RequiredArgsConstructor
public class SeminarDetailRepositoryImpl implements SeminarDetailRepository {

    private final SeminarDetailJpaRepository jpaRepository;
    private final InventoryJpaRepository inventoryJpaRepository;
    private final AdSeminarJpaRepository adSeminarJpaRepository;
    private final SeminarCategoryJpaRepository seminarCategoryJpaRepository;
    private final SeminarDetailPersistenceMapper mapper;

    @Override
    public List<SeminarDetail> saveAll(List<SeminarDetail> details) {
        List<SeminarDetailEntity> entities = new ArrayList<>();
        for (SeminarDetail detail : details) {
            AdSeminarEntity adSeminarEntity = null;
            if (detail.getAdSeminar() != null) {
                adSeminarEntity = adSeminarJpaRepository.getReferenceById(detail.getAdSeminar().getId());
            }
            SeminarCategoryEntity seminarCategoryEntity = null;
            if (detail.getSeminarCategory() != null) {
                seminarCategoryEntity = seminarCategoryJpaRepository.getReferenceById(detail.getSeminarCategory().getId());
            }
            entities.add(SeminarDetailEntity.create(inventoryJpaRepository.getReferenceById(detail.getInventoryId()),
                    adSeminarEntity, detail.getSeminarName(), seminarCategoryEntity,
                    detail.getAttendedYearMonth(), detail.getRemarks()));
        }
        List<SeminarDetail> saved = new ArrayList<>();
        for (SeminarDetailEntity entity : jpaRepository.saveAll(entities)) {
            saved.add(mapper.toDomain(entity));
        }
        return saved;
    }

    @Override
    public List<SeminarDetail> findByInventoryId(int inventoryId) {
        List<SeminarDetail> details = new ArrayList<>();
        for (SeminarDetailEntity entity : jpaRepository.findByInventoryId(inventoryId)) {
            details.add(mapper.toDomain(entity));
        }
        return details;
    }

    @Override
    public void deleteByInventoryId(int inventoryId) {
        jpaRepository.deleteByInventoryId(inventoryId);
    }
}
