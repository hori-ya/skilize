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

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final FiscalYearRepository fiscalYearRepository;
    private final InventoryRepository inventoryRepository;
    private final ItSkillDetailRepository itSkillDetailRepository;
    private final QualificationDetailRepository qualificationDetailRepository;
    private final SeminarDetailRepository seminarDetailRepository;

    @GetMapping
    public DashboardResponse getDashboard(@AuthenticationPrincipal User user) {
        UserInfo userInfo = new UserInfo(user.getId(), user.getName(), user.getRole().name());

        FiscalYear currentFy = fiscalYearRepository.findCurrent(LocalDate.now()).orElse(null);
        if (currentFy == null) {
            return new DashboardResponse(userInfo, null, null);
        }

        FiscalYearRef fyRef = new FiscalYearRef(currentFy.getId(), currentFy.getName());

        List<Inventory> inventories = inventoryRepository.findByUserIdWithFiscalYear(user.getId());
        Inventory currentInv = inventories.stream()
                .filter(i -> i.getFiscalYear().getId().equals(currentFy.getId()))
                .findFirst().orElse(null);

        if (currentInv == null) {
            return new DashboardResponse(userInfo, fyRef, null);
        }

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
    public record FiscalYearRef(int id, String name) {}
    public record CurrentInventoryInfo(int id, String status,
                                        int itSkillCount, int qualificationCount, int seminarCount,
                                        String submittedAt, String goalReviewCompletedAt, String goalCompletedAt) {}
}
