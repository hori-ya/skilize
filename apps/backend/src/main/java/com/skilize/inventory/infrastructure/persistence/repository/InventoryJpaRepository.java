/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 棚卸の永続化を担う Spring Data JPA リポジトリ。
 * JOIN FETCH を用いた N+1 回避クエリを提供する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.InventoryRepository から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.infrastructure.persistence.repository;

import com.skilize.inventory.infrastructure.persistence.entity.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 棚卸 Spring Data JPA リポジトリ。@Query で JOIN FETCH を使い N+1 問題を回避する。
 * 他featureが直接JPA関連（@ManyToOne等）で棚卸エンティティを参照する必要がある場合（未移行feature側）は、
 * このインターフェースを直接injectしてよい。
 */
public interface InventoryJpaRepository extends JpaRepository<InventoryEntity, Integer> {

    /** 棚卸を年度・ユーザーと一緒に取得する。InventoryService.findById() で使用。 */
    @Query("SELECT i FROM InventoryEntity i JOIN FETCH i.fiscalYear JOIN FETCH i.user WHERE i.id = :id")
    Optional<InventoryEntity> findByIdWithAssociations(@Param("id") int id);

    /** 指定ユーザーの全棚卸を年度情報付きで新しい順に返す。 */
    @Query("SELECT i FROM InventoryEntity i JOIN FETCH i.fiscalYear WHERE i.user.id = :userId ORDER BY i.fiscalYear.startDate DESC")
    List<InventoryEntity> findByUserIdWithFiscalYear(@Param("userId") int userId);

    /** 指定ユーザーの指定年度の棚卸を返す。今年度棚卸の存在確認に使用。 */
    @Query("SELECT i FROM InventoryEntity i JOIN FETCH i.fiscalYear WHERE i.user.id = :userId AND i.fiscalYear.id = :fiscalYearId")
    Optional<InventoryEntity> findByUserIdAndFiscalYearId(@Param("userId") int userId, @Param("fiscalYearId") int fiscalYearId);
}
