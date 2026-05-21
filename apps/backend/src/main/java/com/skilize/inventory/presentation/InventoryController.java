package com.skilize.inventory.presentation;

import com.skilize.inventory.application.InventoryService;
import com.skilize.inventory.application.mapper.InventoryApplicationMapper;
import com.skilize.inventory.application.query.ComparisonQueryResult;
import com.skilize.inventory.application.query.GoalReviewQueryResult;
import com.skilize.inventory.domain.*;
import com.skilize.inventory.presentation.request.*;
import com.skilize.inventory.presentation.response.*;
import com.skilize.user.domain.User;
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
    private final InventoryApplicationMapper mapper;

    // --- Header ---

    @GetMapping("/mine")
    public List<InventorySummaryResponse> mine(@AuthenticationPrincipal User user) {
        return inventoryService.findMine(user.getId())
                .stream().map(InventorySummaryResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<InventorySummaryResponse> create(@AuthenticationPrincipal User user,
                                                            @RequestBody CreateInventoryRequest req) {
        Inventory inv = inventoryService.create(user, req.fiscalYearId());
        return ResponseEntity.status(HttpStatus.CREATED).body(InventorySummaryResponse.from(inv));
    }

    @GetMapping("/{id}")
    public InventoryDetailResponse getById(@PathVariable int id, @AuthenticationPrincipal User user) {
        return InventoryDetailResponse.from(inventoryService.findById(id, user));
    }

    // --- IT Skill details ---

    @GetMapping("/{id}/it-skill-details")
    public ItSkillDetailsResponse getItSkillDetails(@PathVariable int id, @AuthenticationPrincipal User user) {
        return new ItSkillDetailsResponse(
                inventoryService.findItSkillDetails(id, user).stream().map(ItSkillDetailResponse::from).toList());
    }

    @PutMapping("/{id}/it-skill-details")
    public ItSkillDetailsResponse saveItSkillDetails(@PathVariable int id,
                                                      @AuthenticationPrincipal User user,
                                                      @RequestBody ItSkillDetailsRequest req) {
        List<ItSkillDetail> saved = inventoryService.saveItSkillDetails(id, user, mapper.toCommands(req.items()));
        return new ItSkillDetailsResponse(saved.stream().map(ItSkillDetailResponse::from).toList());
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
                inventoryService.findQualificationDetails(id, user).stream().map(QualificationDetailResponse::from).toList());
    }

    @PutMapping("/{id}/qualification-details")
    public QualificationDetailsResponse saveQualificationDetails(@PathVariable int id,
                                                                   @AuthenticationPrincipal User user,
                                                                   @RequestBody QualificationDetailsRequest req) {
        List<QualificationDetail> saved = inventoryService.saveQualificationDetails(id, user, mapper.toQualificationCommands(req.items()));
        return new QualificationDetailsResponse(saved.stream().map(QualificationDetailResponse::from).toList());
    }

    // --- Seminar details ---

    @GetMapping("/{id}/seminar-details")
    public SeminarDetailsResponse getSeminarDetails(@PathVariable int id, @AuthenticationPrincipal User user) {
        return new SeminarDetailsResponse(
                inventoryService.findSeminarDetails(id, user).stream().map(SeminarDetailResponse::from).toList());
    }

    @PutMapping("/{id}/seminar-details")
    public SeminarDetailsResponse saveSeminarDetails(@PathVariable int id,
                                                      @AuthenticationPrincipal User user,
                                                      @RequestBody SeminarDetailsRequest req) {
        List<SeminarDetail> saved = inventoryService.saveSeminarDetails(id, user, mapper.toSeminarCommands(req.items()));
        return new SeminarDetailsResponse(saved.stream().map(SeminarDetailResponse::from).toList());
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
    public ComparisonQueryResult getComparison(@PathVariable int id, @AuthenticationPrincipal User user) {
        return inventoryService.getComparison(id, user);
    }

    // --- Goal review ---

    @GetMapping("/{id}/goal-review")
    public GoalReviewQueryResult getGoalReview(@PathVariable int id, @AuthenticationPrincipal User user) {
        return inventoryService.getGoalReview(id, user);
    }

    @PutMapping("/{id}/goal-review")
    public GoalReviewQueryResult saveGoalReview(@PathVariable int id, @AuthenticationPrincipal User user,
                                                @RequestBody GoalReviewUpdateRequest req) {
        return inventoryService.saveGoalReview(id, user, mapper.toGoalReviewUpdateCommands(req.items()));
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
        return new GoalsResponse(goals.stream().map(GoalResponse::from).toList());
    }

    @PutMapping("/{id}/goals")
    public GoalsResponse saveGoals(@PathVariable int id, @AuthenticationPrincipal User user,
                                    @RequestBody GoalsRequest req) {
        List<InventoryGoal> saved = inventoryService.saveGoals(id, user, mapper.toGoalCommands(req.items()));
        return new GoalsResponse(saved.stream().map(GoalResponse::from).toList());
    }

    @PostMapping("/{id}/goals/complete")
    public GoalCompleteResponse completeGoal(@PathVariable int id, @AuthenticationPrincipal User user) {
        Inventory inv = inventoryService.completeGoal(id, user);
        return new GoalCompleteResponse(inv.getId(), inv.getStatus().name(),
                inv.getGoalCompletedAt() != null ? inv.getGoalCompletedAt().toString() : null);
    }
}
