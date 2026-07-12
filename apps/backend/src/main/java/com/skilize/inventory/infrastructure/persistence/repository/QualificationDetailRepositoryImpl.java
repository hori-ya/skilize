/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * domain.repository.QualificationDetailRepository の実装クラス。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（Domain/Infrastructure再構成）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.infrastructure.persistence.repository;

import com.skilize.inventory.domain.model.QualificationDetail;
import com.skilize.inventory.domain.repository.QualificationDetailRepository;
import com.skilize.inventory.infrastructure.persistence.entity.QualificationDetailEntity;
import com.skilize.inventory.infrastructure.persistence.mapper.QualificationDetailPersistenceMapper;
import com.skilize.master.domain.model.Qualification;
import com.skilize.master.infrastructure.persistence.entity.QualificationEntity;
import com.skilize.master.infrastructure.persistence.repository.QualificationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/** domain.repository.QualificationDetailRepository の実装。 */
@Repository
@RequiredArgsConstructor
public class QualificationDetailRepositoryImpl implements QualificationDetailRepository {

    private final QualificationDetailJpaRepository jpaRepository;
    private final InventoryJpaRepository inventoryJpaRepository;
    private final QualificationJpaRepository qualificationJpaRepository;
    private final QualificationDetailPersistenceMapper mapper;

    @Override
    public List<QualificationDetail> saveAll(List<QualificationDetail> details) {
        List<QualificationDetailEntity> entities = details.stream().map(detail -> {
            QualificationEntity qualificationEntity = detail.getQualification() != null
                    ? qualificationJpaRepository.getReferenceById(detail.getQualification().getId()) : null;
            return QualificationDetailEntity.create(inventoryJpaRepository.getReferenceById(detail.getInventoryId()),
                    qualificationEntity, detail.getCustomQualificationName(),
                    detail.getAcquiredYearMonth(), detail.getRemarks());
        }).toList();
        return jpaRepository.saveAll(entities).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<QualificationDetail> findByInventoryId(int inventoryId) {
        return jpaRepository.findByInventoryId(inventoryId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteByInventoryId(int inventoryId) {
        jpaRepository.deleteByInventoryId(inventoryId);
    }

    @Override
    public List<Object[]> findCustomUnregisteredQualificationNames() {
        return jpaRepository.findCustomUnregisteredQualificationNames();
    }

    @Override
    public void linkToMasterQualification(String customName, Qualification qualification) {
        jpaRepository.linkToMasterQualification(customName, qualificationJpaRepository.getReferenceById(qualification.getId()));
    }
}
