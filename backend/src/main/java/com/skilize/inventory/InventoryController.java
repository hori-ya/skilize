package com.skilize.inventory;

import com.skilize.common.exception.GoalIncompleteException;
import com.skilize.domain.inventory.*;
import com.skilize.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // --- Header ---

    @GetMapping("/mine")
    public List<InventorySummaryDto> mine(@AuthenticationPrincipal User user) {
        return inventoryService.findMine(user.getId())
                .stream().map(InventorySummaryDto::from).toList();
    }

    @PostMapping
    public ResponseEntity<InventorySummaryDto> create(@AuthenticationPrincipal User user,
                                                       @RequestBody CreateInventoryRequest req) {
        Inventory inv = inventoryService.create(user, req.fiscalYearId());
        return ResponseEntity.status(HttpStatus.CREATED).body(InventorySummaryDto.from(inv));
    }

    @GetMapping("/{id}")
    public InventoryDetailDto getById(@PathVariable int id, @AuthenticationPrincipal User user) {
        Inventory inv = inventoryService.findById(id, user);
        return InventoryDetailDto.from(inv);
    }

    // --- IT Skill details ---

    @GetMapping("/{id}/it-skill-details")
    public ItSkillDetailsResponse getItSkillDetails(@PathVariable int id, @AuthenticationPrincipal User user) {
        return new ItSkillDetailsResponse(
                inventoryService.findItSkillDetails(id, user).stream().map(ItSkillDetailDto::from).toList());
    }

    @PutMapping("/{id}/it-skill-details")
    public ItSkillDetailsResponse saveItSkillDetails(@PathVariable int id,
                                                      @AuthenticationPrincipal User user,
                                                      @RequestBody ItSkillDetailsRequest req) {
        List<ItSkillDetail> saved = inventoryService.saveItSkillDetails(id, user, req.items());
        return new ItSkillDetailsResponse(saved.stream().map(ItSkillDetailDto::from).toList());
    }

    @PatchMapping("/{id}/it-skill-details/{detailId}")
    public RemarksPatchResponse patchItSkillRemarks(@PathVariable int id, @PathVariable int detailId,
                                                     @AuthenticationPrincipal User user,
                                                     @RequestBody RemarksPatchRequest req) {
        ItSkillDetail detail = inventoryService.updateItSkillDetailRemarks(id, detailId, user, req.remarks());
        return new RemarksPatchResponse(detail.getId(), detail.getRemarks());
    }

    // --- Qualification details ---

    @GetMapping("/{id}/qualification-details")
    public QualificationDetailsResponse getQualificationDetails(@PathVariable int id, @AuthenticationPrincipal User user) {
        return new QualificationDetailsResponse(
                inventoryService.findQualificationDetails(id, user).stream().map(QualificationDetailDto::from).toList());
    }

    @PutMapping("/{id}/qualification-details")
    public QualificationDetailsResponse saveQualificationDetails(@PathVariable int id,
                                                                   @AuthenticationPrincipal User user,
                                                                   @RequestBody QualificationDetailsRequest req) {
        List<QualificationDetail> saved = inventoryService.saveQualificationDetails(id, user, req.items());
        return new QualificationDetailsResponse(saved.stream().map(QualificationDetailDto::from).toList());
    }

    // --- Seminar details ---

    @GetMapping("/{id}/seminar-details")
    public SeminarDetailsResponse getSeminarDetails(@PathVariable int id, @AuthenticationPrincipal User user) {
        return new SeminarDetailsResponse(
                inventoryService.findSeminarDetails(id, user).stream().map(SeminarDetailDto::from).toList());
    }

    @PutMapping("/{id}/seminar-details")
    public SeminarDetailsResponse saveSeminarDetails(@PathVariable int id,
                                                      @AuthenticationPrincipal User user,
                                                      @RequestBody SeminarDetailsRequest req) {
        List<SeminarDetail> saved = inventoryService.saveSeminarDetails(id, user, req.items());
        return new SeminarDetailsResponse(saved.stream().map(SeminarDetailDto::from).toList());
    }

    // --- Submit ---

    @PostMapping("/{id}/submit")
    public SubmitResponse submit(@PathVariable int id, @AuthenticationPrincipal User user) {
        Inventory inv = inventoryService.submit(id, user);
        return new SubmitResponse(inv.getId(), inv.getStatus().name(),
                inv.getSubmittedAt() != null ? inv.getSubmittedAt().toString() : null);
    }

    // --- Comparison ---

    @GetMapping("/{id}/comparison")
    public ComparisonResponse getComparison(@PathVariable int id, @AuthenticationPrincipal User user) {
        return inventoryService.getComparison(id, user);
    }

    // --- Goal review ---

    @GetMapping("/{id}/goal-review")
    public GoalReviewResponse getGoalReview(@PathVariable int id, @AuthenticationPrincipal User user) {
        return inventoryService.getGoalReview(id, user);
    }

    @PutMapping("/{id}/goal-review")
    public GoalReviewResponse saveGoalReview(@PathVariable int id, @AuthenticationPrincipal User user,
                                              @RequestBody GoalReviewUpdateRequest req) {
        return inventoryService.saveGoalReview(id, user, req.items());
    }

    @PostMapping("/{id}/goal-review/complete")
    public GoalReviewCompleteResponse completeGoalReview(@PathVariable int id, @AuthenticationPrincipal User user) {
        Inventory inv = inventoryService.completeGoalReview(id, user);
        return new GoalReviewCompleteResponse(inv.getId(),
                inv.getGoalReviewCompletedAt() != null ? inv.getGoalReviewCompletedAt().toString() : null);
    }

    // --- Goals ---

    @GetMapping("/{id}/goals")
    public GoalsResponse getGoals(@PathVariable int id, @AuthenticationPrincipal User user) {
        List<InventoryGoal> goals = inventoryService.findGoals(id, user);
        return new GoalsResponse(goals.stream().map(GoalDto::from).toList());
    }

    @PutMapping("/{id}/goals")
    public GoalsResponse saveGoals(@PathVariable int id, @AuthenticationPrincipal User user,
                                    @RequestBody GoalsRequest req) {
        List<InventoryGoal> saved = inventoryService.saveGoals(id, user, req.items());
        return new GoalsResponse(saved.stream().map(GoalDto::from).toList());
    }

    @PostMapping("/{id}/goals/complete")
    public GoalCompleteResponse completeGoal(@PathVariable int id, @AuthenticationPrincipal User user) {
        Inventory inv = inventoryService.completeGoal(id, user);
        return new GoalCompleteResponse(inv.getId(), inv.getStatus().name(),
                inv.getGoalCompletedAt() != null ? inv.getGoalCompletedAt().toString() : null);
    }

    // ===== DTOs and Records =====

    public record CreateInventoryRequest(int fiscalYearId) {}

    public record InventorySummaryDto(int id, FiscalYearRef fiscalYear, String status,
                                       String submittedAt, String goalCompletedAt) {
        static InventorySummaryDto from(Inventory i) {
            return new InventorySummaryDto(i.getId(),
                    new FiscalYearRef(i.getFiscalYear().getId(), i.getFiscalYear().getName()),
                    i.getStatus().name(),
                    i.getSubmittedAt() != null ? i.getSubmittedAt().toString() : null,
                    i.getGoalCompletedAt() != null ? i.getGoalCompletedAt().toString() : null);
        }
    }

    public record InventoryDetailDto(int id, int userId, FiscalYearRef fiscalYear, String status,
                                      String submittedAt, String goalReviewCompletedAt,
                                      String goalCompletedAt, String createdAt, String updatedAt) {
        static InventoryDetailDto from(Inventory i) {
            return new InventoryDetailDto(i.getId(), i.getUser().getId(),
                    new FiscalYearRef(i.getFiscalYear().getId(), i.getFiscalYear().getName()),
                    i.getStatus().name(),
                    i.getSubmittedAt() != null ? i.getSubmittedAt().toString() : null,
                    i.getGoalReviewCompletedAt() != null ? i.getGoalReviewCompletedAt().toString() : null,
                    i.getGoalCompletedAt() != null ? i.getGoalCompletedAt().toString() : null,
                    i.getCreatedAt() != null ? i.getCreatedAt().toString() : null,
                    i.getUpdatedAt() != null ? i.getUpdatedAt().toString() : null);
        }
    }

    public record FiscalYearRef(int id, String name) {}

    // IT Skill details
    public record ItSkillDetailsRequest(List<ItSkillDetailItem> items) {}
    public record ItSkillDetailItem(Integer id, Integer itSkillId, String customSkillName,
                                     int skillLevelId, String remarks) {}
    public record ItSkillDetailsResponse(List<ItSkillDetailDto> items) {}
    public record ItSkillDetailDto(int id, Integer itSkillId, String itSkillName,
                                    String customSkillName, int skillLevelId,
                                    short levelValue, String remarks) {
        static ItSkillDetailDto from(ItSkillDetail d) {
            return new ItSkillDetailDto(d.getId(),
                    d.getItSkill() != null ? d.getItSkill().getId() : null,
                    d.getItSkill() != null ? d.getItSkill().getName() : null,
                    d.getCustomSkillName(),
                    d.getSkillLevel().getId(), d.getSkillLevel().getLevelValue(), d.getRemarks());
        }
    }

    public record RemarksPatchRequest(String remarks) {}
    public record RemarksPatchResponse(int id, String remarks) {}

    // Qualification details
    public record QualificationDetailsRequest(List<QualificationDetailItem> items) {}
    public record QualificationDetailItem(Integer id, Integer qualificationId,
                                           String customQualificationName,
                                           String acquiredYearMonth, String remarks) {}
    public record QualificationDetailsResponse(List<QualificationDetailDto> items) {}
    public record QualificationDetailDto(int id, Integer qualificationId, String qualificationName,
                                          String qualificationCategoryName,
                                          String customQualificationName,
                                          String acquiredYearMonth, String remarks) {
        static QualificationDetailDto from(QualificationDetail d) {
            return new QualificationDetailDto(d.getId(),
                    d.getQualification() != null ? d.getQualification().getId() : null,
                    d.getQualification() != null ? d.getQualification().getName() : null,
                    d.getQualification() != null && d.getQualification().getCategory() != null
                            ? d.getQualification().getCategory().getName() : null,
                    d.getCustomQualificationName(),
                    d.getAcquiredYearMonth() != null ? d.getAcquiredYearMonth().toString() : null,
                    d.getRemarks());
        }
    }

    // Seminar details
    public record SeminarDetailsRequest(List<SeminarDetailItem> items) {}
    public record SeminarDetailItem(Integer id, Integer adSeminarId, String seminarName,
                                     Integer seminarCategoryId, String attendedYearMonth, String remarks) {}
    public record SeminarDetailsResponse(List<SeminarDetailDto> items) {}
    public record SeminarDetailDto(int id, Integer adSeminarId, String adSeminarName,
                                    Integer adSeminarCategoryId, String adSeminarCategoryName,
                                    String seminarName, Integer seminarCategoryId, String seminarCategoryName,
                                    String attendedYearMonth, String remarks) {
        static SeminarDetailDto from(SeminarDetail d) {
            return new SeminarDetailDto(d.getId(),
                    d.getAdSeminar() != null ? d.getAdSeminar().getId() : null,
                    d.getAdSeminar() != null ? d.getAdSeminar().getName() : null,
                    d.getAdSeminar() != null && d.getAdSeminar().getCategory() != null ? d.getAdSeminar().getCategory().getId() : null,
                    d.getAdSeminar() != null && d.getAdSeminar().getCategory() != null ? d.getAdSeminar().getCategory().getName() : null,
                    d.getSeminarName(),
                    d.getSeminarCategory() != null ? d.getSeminarCategory().getId() : null,
                    d.getSeminarCategory() != null ? d.getSeminarCategory().getName() : null,
                    d.getAttendedYearMonth() != null ? d.getAttendedYearMonth().toString() : null,
                    d.getRemarks());
        }
    }

    // Submit
    public record SubmitResponse(int id, String status, String submittedAt) {}

    // Comparison
    public record ComparisonResponse(int inventoryId, String currentFiscalYear, String prevFiscalYear,
                                      boolean hasPrevYear, List<ComparisonItem> items) {}
    public record ComparisonItem(Integer itSkillId, String skillName, int currentDetailId,
                                  int currentLevelValue, String currentRemarks,
                                  Integer prevLevelValue, Integer diff) {}

    // Goal review
    public record GoalReviewResponse(String prevFiscalYear, boolean hasPrevGoals, List<GoalReviewItem> items) {}
    public record GoalReviewItem(int prevGoalId, String goalCategory, String goalName,
                                  String targetPeriod, String reason,
                                  String achievementStatus, String reviewNote) {}
    public record GoalReviewUpdateRequest(List<GoalReviewUpdateItem> items) {}
    public record GoalReviewUpdateItem(int prevGoalId, String achievementStatus, String reviewNote) {}
    public record GoalReviewCompleteResponse(int id, String goalReviewCompletedAt) {}

    // Goals
    public record GoalsRequest(List<GoalItem> items) {}
    public record GoalItem(Integer id, String goalCategory, Integer itSkillId,
                            Integer qualificationId, Integer adSeminarId,
                            String customName, String targetPeriod, String reason) {}
    public record GoalsResponse(List<GoalDto> items) {}
    public record GoalDto(int id, String goalCategory,
                           Integer itSkillId, String itSkillName,
                           Integer qualificationId, String qualificationName,
                           Integer adSeminarId, String adSeminarName,
                           String customName, String targetPeriod, String reason) {
        static GoalDto from(InventoryGoal g) {
            return new GoalDto(g.getId(), g.getGoalCategory().name(),
                    g.getItSkill() != null ? g.getItSkill().getId() : null,
                    g.getItSkill() != null ? g.getItSkill().getName() : null,
                    g.getQualification() != null ? g.getQualification().getId() : null,
                    g.getQualification() != null ? g.getQualification().getName() : null,
                    g.getAdSeminar() != null ? g.getAdSeminar().getId() : null,
                    g.getAdSeminar() != null ? g.getAdSeminar().getName() : null,
                    g.getCustomName(),
                    g.getTargetPeriod().toString(), g.getReason());
        }
    }
    public record GoalCompleteResponse(int id, String status, String goalCompletedAt) {}
}
