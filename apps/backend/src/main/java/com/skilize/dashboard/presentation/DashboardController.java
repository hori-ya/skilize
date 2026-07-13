/**************************************************************************************************************
 * 機能ID      ：DSH
 * 機能名      ：ダッシュボード
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ダッシュボード情報を提供する REST API コントローラー。
 * ログインユーザーの今年度棚卸サマリー（ステータス・明細件数・各種完了日時）を返す。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.dashboard.presentation;

import com.skilize.dashboard.application.DashboardService;
import com.skilize.dashboard.application.query.DashboardQueryResult;
import com.skilize.dashboard.presentation.response.DashboardResponse;
import com.skilize.fiscalyear.domain.model.FiscalYear;
import com.skilize.inventory.domain.model.Inventory;
import com.skilize.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ダッシュボード情報の REST API コントローラー。
 * ログインユーザーの今年度棚卸サマリー（ステータス・明細件数・各種完了日時）を返す。
 * 年度なし・棚卸なしの場合は null フィールドを含むレスポンスを返す（エラーにはしない）。
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * ダッシュボード情報を返す。取得できない情報は null として返す（段階的な null チェック）。
     */
    @GetMapping
    public DashboardResponse getDashboard(@AuthenticationPrincipal(expression = "user") User user) {
        DashboardResponse.UserInfo userInfo = new DashboardResponse.UserInfo(
                user.getId(), user.getName(), user.getRole().name());

        DashboardQueryResult result = dashboardService.getDashboard(user.getId());
        FiscalYear currentFy = result.fiscalYear();
        if (currentFy == null) {
            // 有効年度なし → 棚卸情報も年度情報も null で返す
            return new DashboardResponse(userInfo, null, null);
        }

        String inputStartDate = null;
        if (currentFy.getInputStartDate() != null) {
            inputStartDate = currentFy.getInputStartDate().toString();
        }
        String inputEndDate = null;
        if (currentFy.getInputEndDate() != null) {
            inputEndDate = currentFy.getInputEndDate().toString();
        }
        DashboardResponse.FiscalYearRef fyRef = new DashboardResponse.FiscalYearRef(
                currentFy.getId(), currentFy.getName(), inputStartDate, inputEndDate);

        Inventory currentInv = result.inventory();
        if (currentInv == null) {
            // 今年度の棚卸が未作成 → 棚卸情報は null で返す
            return new DashboardResponse(userInfo, fyRef, null);
        }

        String submittedAt = null;
        if (currentInv.getSubmittedAt() != null) {
            submittedAt = currentInv.getSubmittedAt().toString();
        }
        String goalReviewCompletedAt = null;
        if (currentInv.getGoalReviewCompletedAt() != null) {
            goalReviewCompletedAt = currentInv.getGoalReviewCompletedAt().toString();
        }
        String goalCompletedAt = null;
        if (currentInv.getGoalCompletedAt() != null) {
            goalCompletedAt = currentInv.getGoalCompletedAt().toString();
        }
        DashboardResponse.CurrentInventoryInfo invInfo = new DashboardResponse.CurrentInventoryInfo(
                currentInv.getId(), currentInv.getStatus().name(),
                result.itSkillCount(), result.qualificationCount(), result.seminarCount(),
                submittedAt, goalReviewCompletedAt, goalCompletedAt);

        return new DashboardResponse(userInfo, fyRef, invInfo);
    }
}
