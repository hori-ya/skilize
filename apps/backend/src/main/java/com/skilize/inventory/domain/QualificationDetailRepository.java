package com.skilize.inventory.domain;

import com.skilize.master.domain.Qualification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 資格明細リポジトリ。全件洗い替えパターン（deleteByInventoryId → saveAll）で使用する。
 */
public interface QualificationDetailRepository extends JpaRepository<QualificationDetail, Integer> {

    /** 資格・資格分類を JOIN FETCH で一括取得する（N+1 回避）。 */
    @Query("SELECT d FROM QualificationDetail d LEFT JOIN FETCH d.qualification q LEFT JOIN FETCH q.category WHERE d.inventory.id = :inventoryId")
    List<QualificationDetail> findByInventoryId(@Param("inventoryId") int inventoryId);

    /** 指定棚卸の資格明細を全件削除する。全件洗い替え時の DELETE に使用する。 */
    @Modifying
    @Query("DELETE FROM QualificationDetail d WHERE d.inventory.id = :inventoryId")
    void deleteByInventoryId(@Param("inventoryId") int inventoryId);

    /** カスタム資格名のうち qualifications マスタに未登録のものを使用件数付きで返す。 */
    @Query("SELECT d.customQualificationName, COUNT(d) FROM QualificationDetail d " +
           "WHERE d.qualification IS NULL AND d.customQualificationName IS NOT NULL " +
           "AND NOT EXISTS (SELECT q FROM Qualification q WHERE q.name = d.customQualificationName) " +
           "GROUP BY d.customQualificationName ORDER BY COUNT(d) DESC")
    List<Object[]> findCustomUnregisteredQualificationNames();

    /** 昇格後、同名カスタム資格明細をマスタ資格へ紐付ける。 */
    @Modifying
    @Query("UPDATE QualificationDetail d SET d.qualification = :qualification, d.customQualificationName = null " +
           "WHERE d.customQualificationName = :customName AND d.qualification IS NULL")
    void linkToMasterQualification(@Param("customName") String customName,
                                    @Param("qualification") Qualification qualification);
}
