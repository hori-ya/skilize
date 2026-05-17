package com.skilize.inventory.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * ITスキル明細リポジトリ。全件洗い替えパターン（deleteByInventoryId → saveAll）で使用する。
 */
public interface ItSkillDetailRepository extends JpaRepository<ItSkillDetail, Integer> {

    /** ITスキル・スキルレベルを JOIN FETCH で一括取得する（N+1 回避）。 */
    @Query("SELECT d FROM ItSkillDetail d LEFT JOIN FETCH d.itSkill LEFT JOIN FETCH d.skillLevel WHERE d.inventory.id = :inventoryId")
    List<ItSkillDetail> findByInventoryId(@Param("inventoryId") int inventoryId);

    /**
     * 指定棚卸のITスキル明細を全件削除する。
     * @Modifying: SELECT 以外（INSERT/UPDATE/DELETE）のクエリに必須。
     * 全件洗い替え時の DELETE に使用する。
     */
    @Modifying
    @Query("DELETE FROM ItSkillDetail d WHERE d.inventory.id = :inventoryId")
    void deleteByInventoryId(@Param("inventoryId") int inventoryId);
}
