/**************************************************************************************************************
 * 機能ID      ：DSH
 * 機能名      ：ダッシュボード
 * 作成日      ：2026/07/12
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ダッシュボード情報（今年度棚卸サマリー）の参照ビジネスロジックを提供するアプリケーションサービス。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/07/12 hori-ya 初版作成（DashboardControllerのRepository直接参照をこちらへ移行し、トランザクション境界をapplication層に統一）
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.dashboard.application;

import com.skilize.dashboard.application.query.DashboardQueryResult;
import com.skilize.fiscalyear.domain.model.FiscalYear;
import com.skilize.fiscalyear.domain.repository.FiscalYearRepository;
import com.skilize.inventory.domain.model.Inventory;
import com.skilize.inventory.domain.repository.InventoryRepository;
import com.skilize.inventory.domain.repository.ItSkillDetailRepository;
import com.skilize.inventory.domain.repository.QualificationDetailRepository;
import com.skilize.inventory.domain.repository.SeminarDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * ダッシュボード情報（今年度棚卸サマリー）の参照ビジネスロジック。
 * 今日の日付 → 現在年度 → 今年度の棚卸 の順に検索し、途中で見つからなければその後の情報は null になる。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final FiscalYearRepository fiscalYearRepository;
    private final InventoryRepository inventoryRepository;
    private final ItSkillDetailRepository itSkillDetailRepository;
    private final QualificationDetailRepository qualificationDetailRepository;
    private final SeminarDetailRepository seminarDetailRepository;

    /** 指定ユーザーのダッシュボード情報（現在年度・今年度棚卸・各明細件数）を返す。 */
    @Transactional(readOnly = true)
    public DashboardQueryResult getDashboard(int userId) {
        // 今日の日付を基準に現在の有効年度を取得する
        FiscalYear currentFy = fiscalYearRepository.findCurrent(LocalDate.now()).orElse(null);
        if (currentFy == null) {
            return new DashboardQueryResult(null, null, 0, 0, 0);
        }

        // 全棚卸を取得し、今年度分をフィルタリングする
        List<Inventory> inventories = inventoryRepository.findByUserIdWithFiscalYear(userId);
        Inventory currentInv = inventories.stream()
                .filter(i -> i.getFiscalYear().getId().equals(currentFy.getId()))
                .findFirst().orElse(null);

        if (currentInv == null) {
            return new DashboardQueryResult(currentFy, null, 0, 0, 0);
        }

        // 各明細の件数を取得する（size() でカウント。N+1 ではなく棚卸IDで直接引く）
        int itCount = itSkillDetailRepository.findByInventoryId(currentInv.getId()).size();
        int qualCount = qualificationDetailRepository.findByInventoryId(currentInv.getId()).size();
        int semCount = seminarDetailRepository.findByInventoryId(currentInv.getId()).size();

        return new DashboardQueryResult(currentFy, currentInv, itCount, qualCount, semCount);
    }
}
