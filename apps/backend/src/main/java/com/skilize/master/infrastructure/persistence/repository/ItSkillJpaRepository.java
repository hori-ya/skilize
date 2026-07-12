/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ITスキルマスタの Spring Data JPA リポジトリ。
 * マスタ管理画面向けの階層ソート付き全件取得と、棚卸入力画面向けの有効スキル取得を提供する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya Domain/Infrastructure再構成: domain.ItSkillRepository から分離
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.persistence.repository;

import com.skilize.master.infrastructure.persistence.entity.ItSkillEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

/**
 * ITスキル Spring Data JPA リポジトリ。
 *
 * マスタ管理画面向けの全件・無効フィルタ取得では、ItSkillCategoryEntity.parent 関連（読み取り専用）を
 * LEFT JOIN で辿り「分類1 → 分類2 → 分類3 → 並順」の4段階ソートを JPQL で実現する。
 * JOIN FETCH s.category で category を同一クエリでロードし LazyInitializationException を回避する。
 *
 * 棚卸入力画面向けの有効のみ取得は従来の JOIN FETCH 方式を維持する。
 */
public interface ItSkillJpaRepository extends JpaRepository<ItSkillEntity, Integer> {

    /**
     * 全ITスキルをマスタ管理画面向けソート順（分類1→分類2→分類3→並順）で取得する（有効・無効含む全件）。
     * p1 = categoryの親（L2ならL1、L3ならL2）、p2 = categoryの祖父母（L3ならL1）。
     */
    @Query("""
            SELECT s FROM ItSkillEntity s
            JOIN FETCH s.category c
            LEFT JOIN c.parent p1
            LEFT JOIN p1.parent p2
            ORDER BY
                COALESCE(p2.sortOrder, p1.sortOrder, c.sortOrder) ASC,
                CASE WHEN c.level = 2 THEN c.sortOrder
                     WHEN c.level = 3 THEN p1.sortOrder
                     ELSE NULL END ASC NULLS LAST,
                CASE WHEN c.level = 3 THEN c.sortOrder
                     ELSE NULL END ASC NULLS LAST,
                s.sortOrder ASC
            """)
    List<ItSkillEntity> findAllOrderByHierarchy();

    /** 有効なITスキルのみをカテゴリ付きで取得する。棚卸入力画面の選択肢に使用（JOIN FETCH で N+1 回避）。 */
    @Query("SELECT s FROM ItSkillEntity s JOIN FETCH s.category WHERE s.active = true ORDER BY s.category.sortOrder ASC, s.sortOrder ASC")
    List<ItSkillEntity> findAllActiveWithCategory();

    /**
     * 無効なITスキルをマスタ管理画面向けソート順（分類1→分類2→分類3→並順）で取得する。
     */
    @Query("""
            SELECT s FROM ItSkillEntity s
            JOIN FETCH s.category c
            LEFT JOIN c.parent p1
            LEFT JOIN p1.parent p2
            WHERE s.active = false
            ORDER BY
                COALESCE(p2.sortOrder, p1.sortOrder, c.sortOrder) ASC,
                CASE WHEN c.level = 2 THEN c.sortOrder
                     WHEN c.level = 3 THEN p1.sortOrder
                     ELSE NULL END ASC NULLS LAST,
                CASE WHEN c.level = 3 THEN c.sortOrder
                     ELSE NULL END ASC NULLS LAST,
                s.sortOrder ASC
            """)
    List<ItSkillEntity> findByActiveFalseOrderByHierarchy();
}
