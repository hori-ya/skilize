package com.skilize.domain.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Integer> {

    @Query("SELECT i FROM Inventory i JOIN FETCH i.fiscalYear JOIN FETCH i.user WHERE i.id = :id")
    Optional<Inventory> findByIdWithAssociations(@Param("id") int id);

    @Query("SELECT i FROM Inventory i JOIN FETCH i.fiscalYear WHERE i.user.id = :userId ORDER BY i.fiscalYear.startDate DESC")
    List<Inventory> findByUserIdWithFiscalYear(@Param("userId") int userId);

    @Query("SELECT i FROM Inventory i JOIN FETCH i.fiscalYear WHERE i.user.id = :userId AND i.fiscalYear.id = :fiscalYearId")
    Optional<Inventory> findByUserIdAndFiscalYearId(@Param("userId") int userId, @Param("fiscalYearId") int fiscalYearId);
}
