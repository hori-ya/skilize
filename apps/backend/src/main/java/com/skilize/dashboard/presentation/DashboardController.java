package com.skilize.dashboard.presentation;

import com.skilize.fiscalyear.domain.FiscalYear;
import com.skilize.fiscalyear.domain.FiscalYearRepository;
import com.skilize.inventory.domain.*;
import com.skilize.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * ダッシュボード情報の REST API コントローラー。
 * ログインユーザーの今年度棚卸サマリー（ステータス・明細件数・各種完了日時）を返す。
 * 年度なし・棚卸なしの場合は null フィールドを含むレスポンスを返す（エラーにはしない）。
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final FiscalYearRepository fiscalYearRepository;
    private final InventoryRepository inventoryRepository;
    private final ItSkillDetailRepository itSkillDetailRepository;
    private final QualificationDetailRepository qualificationDetailRepository;
    private final SeminarDetailRepository seminarDetailRepository;

    /**
     * ダッシュボード情報を返す。取得できない情報は null として返す（段階的な null チェック）。
     * 今日の日付 → 現在年度 → 今年度の棚卸 の順に検索し、途中で見つからなければその後の情報は null になる。
     */
    @GetMapping
    public DashboardResponse getDashboard(@AuthenticationPrincipal User user) {
        UserInfo userInfo = new UserInfo(user.getId(), user.getName(), user.getRole().name());

        // 今日の日付を基準に現在の有効年度を取得する
        FiscalYear currentFy = fiscalYearRepository.findCurrent(LocalDate.now()).orElse(null);
        if (currentFy == null) {
            // 有効年度なし → 棚卸情報も年度情報も null で返す
            return new DashboardResponse(userInfo, null, null);
        }

        FiscalYearRef fyRef = new FiscalYearRef(
                currentFy.getId(), currentFy.getName(),
                currentFy.getInputStartDate() != null ? currentFy.getInputStartDate().toString() : null,
                currentFy.getInputEndDate()   != null ? currentFy.getInputEndDate().toString()   : null);

        // 全棚卸を取得し、今年度分をフィルタリングする
        List<Inventory> inventories = inventoryRepository.findByUserIdWithFiscalYear(user.getId());
        Inventory currentInv = inventories.stream()
                .filter(i -> i.getFiscalYear().getId().equals(currentFy.getId()))
                .findFirst().orElse(null);

        if (currentInv == null) {
            // 今年度の棚卸が未作成 → 棚卸情報は null で返す
            return new DashboardResponse(userInfo, fyRef, null);
        }

        // 各明細の件数を取得する（size() でカウント。N+1 ではなく棚卸IDで直接引く）
        int itCount = itSkillDetailRepository.findByInventoryId(currentInv.getId()).size();
        int qualCount = qualificationDetailRepository.findByInventoryId(currentInv.getId()).size();
        int semCount = seminarDetailRepository.findByInventoryId(currentInv.getId()).size();

        CurrentInventoryInfo invInfo = new CurrentInventoryInfo(
                currentInv.getId(), currentInv.getStatus().name(),
                itCount, qualCount, semCount,
                currentInv.getSubmittedAt() != null ? currentInv.getSubmittedAt().toString() : null,
                currentInv.getGoalReviewCompletedAt() != null ? currentInv.getGoalReviewCompletedAt().toString() : null,
                currentInv.getGoalCompletedAt() != null ? currentInv.getGoalCompletedAt().toString() : null);

        return new DashboardResponse(userInfo, fyRef, invInfo);
    }

    public record DashboardResponse(UserInfo user, FiscalYearRef currentFiscalYear,
                                     CurrentInventoryInfo currentInventory) {}
    public record UserInfo(int id, String name, String role) {}
    public record FiscalYearRef(int id, String name, String inputStartDate, String inputEndDate) {}
    public record CurrentInventoryInfo(int id, String status,
                                        int itSkillCount, int qualificationCount, int seminarCount,
                                        String submittedAt, String goalReviewCompletedAt, String goalCompletedAt) {}
}
