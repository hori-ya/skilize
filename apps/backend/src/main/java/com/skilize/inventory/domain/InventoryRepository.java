package com.skilize.inventory.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 棚卸リポジトリ。@Query で JOIN FETCH を使い N+1 問題を回避する。
 * JOIN FETCH: 関連エンティティを1クエリで一緒に取得する JPQL 構文（LazyLoading による追加クエリを防ぐ）。
 */
public interface InventoryRepository extends JpaRepository<Inventory, Integer> {

    /** 棚卸を年度・ユーザーと一緒に取得する。InventoryService.findById() で使用。 */
    @Query("SELECT i FROM Inventory i JOIN FETCH i.fiscalYear JOIN FETCH i.user WHERE i.id = :id")
    Optional<Inventory> findByIdWithAssociations(@Param("id") int id);

    /** 指定ユーザーの全棚卸を年度情報付きで新しい順に返す。 */
    @Query("SELECT i FROM Inventory i JOIN FETCH i.fiscalYear WHERE i.user.id = :userId ORDER BY i.fiscalYear.startDate DESC")
    List<Inventory> findByUserIdWithFiscalYear(@Param("userId") int userId);

    /** 指定ユーザーの指定年度の棚卸を返す。今年度棚卸の存在確認に使用。 */
    @Query("SELECT i FROM Inventory i JOIN FETCH i.fiscalYear WHERE i.user.id = :userId AND i.fiscalYear.id = :fiscalYearId")
    Optional<Inventory> findByUserIdAndFiscalYearId(@Param("userId") int userId, @Param("fiscalYearId") int fiscalYearId);
}
