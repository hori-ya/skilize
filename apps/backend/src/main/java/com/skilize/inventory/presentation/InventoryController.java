/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * スキル棚卸の REST API コントローラー。
 * 棚卸ヘッダー・明細（ITスキル/資格/セミナー）・提出・前年比較・目標振り返り・目標設定の各エンドポイントを提供する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.presentation;

import com.skilize.inventory.application.InventoryService;
import com.skilize.inventory.application.mapper.InventoryApplicationMapper;
import com.skilize.inventory.application.query.ComparisonQueryResult;
import com.skilize.inventory.application.query.GoalReviewQueryResult;
import com.skilize.inventory.domain.model.*;
import com.skilize.inventory.presentation.request.*;
import com.skilize.inventory.presentation.response.*;
import com.skilize.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * スキル棚卸の REST API コントローラー。
 * ヘッダー・ITスキル明細・資格明細・セミナー明細・提出・前年比較・目標振り返り・目標設定の各操作を提供する。
 * 権限チェックは InventoryService.checkOwnership() で行い、本人・TL・ADMIN のみ参照可。
 */
@RestController
@RequestMapping("/api/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryApplicationMapper mapper;

    // --- Header ---

    /** 自分の棚卸一覧（年度別サマリー）を返す。 */
    @GetMapping("/mine")
    public List<InventorySummaryResponse> mine(@AuthenticationPrincipal(expression = "user") User user) {
        return inventoryService.findMine(user.getId())
                .stream().map(InventorySummaryResponse::from).toList();
    }

    /** 棚卸を新規作成する。同年度に既存棚卸がある場合は 409 CONFLICT を返す。 */
    @PostMapping
    public ResponseEntity<InventorySummaryResponse> create(@AuthenticationPrincipal(expression = "user") User user,
                                                            @RequestBody CreateInventoryRequest req) {
        Inventory inv = inventoryService.create(user, req.fiscalYearId());
        return ResponseEntity.status(HttpStatus.CREATED).body(InventorySummaryResponse.from(inv));
    }

    /** ID で棚卸ヘッダー情報を返す。権限チェックは Service で行う。 */
    @GetMapping("/{id}")
    public InventoryDetailResponse getById(@PathVariable int id, @AuthenticationPrincipal(expression = "user") User user) {
        return InventoryDetailResponse.from(inventoryService.findById(id, user));
    }

    // --- IT Skill details ---

    /** 棚卸のITスキル明細一覧を返す。 */
    @GetMapping("/{id}/it-skill-details")
    public ItSkillDetailsResponse getItSkillDetails(@PathVariable int id, @AuthenticationPrincipal(expression = "user") User user) {
        return new ItSkillDetailsResponse(
                inventoryService.findItSkillDetails(id, user).stream().map(ItSkillDetailResponse::from).toList());
    }

    /** ITスキル明細を全件洗い替えで保存する（既存明細を削除→再 INSERT）。 */
    @PutMapping("/{id}/it-skill-details")
    public ItSkillDetailsResponse saveItSkillDetails(@PathVariable int id,
                                                      @AuthenticationPrincipal(expression = "user") User user,
                                                      @RequestBody ItSkillDetailsRequest req) {
        List<ItSkillDetail> saved = inventoryService.saveItSkillDetails(id, user, mapper.toCommands(req.items()));
        return new ItSkillDetailsResponse(saved.stream().map(ItSkillDetailResponse::from).toList());
    }

    /** ITスキル明細の備考のみを部分更新する。 */
    @PatchMapping("/{id}/it-skill-details/{detailId}")
    public RemarksPatchResponse patchItSkillRemarks(@PathVariable int id, @PathVariable int detailId,
                                                     @AuthenticationPrincipal(expression = "user") User user,
                                                     @RequestBody RemarksPatchRequest req) {
        ItSkillDetail detail = inventoryService.updateItSkillDetailRemarks(id, detailId, user, req.remarks());
        return new RemarksPatchResponse(detail.getId(), detail.getRemarks());
    }

    // --- Qualification details ---

    /** 棚卸の資格明細一覧を返す。 */
    @GetMapping("/{id}/qualification-details")
    public QualificationDetailsResponse getQualificationDetails(@PathVariable int id, @AuthenticationPrincipal(expression = "user") User user) {
        return new QualificationDetailsResponse(
                inventoryService.findQualificationDetails(id, user).stream().map(QualificationDetailResponse::from).toList());
    }

    /** 資格明細を全件洗い替えで保存する。 */
    @PutMapping("/{id}/qualification-details")
    public QualificationDetailsResponse saveQualificationDetails(@PathVariable int id,
                                                                   @AuthenticationPrincipal(expression = "user") User user,
                                                                   @RequestBody QualificationDetailsRequest req) {
        List<QualificationDetail> saved = inventoryService.saveQualificationDetails(id, user, mapper.toQualificationCommands(req.items()));
        return new QualificationDetailsResponse(saved.stream().map(QualificationDetailResponse::from).toList());
    }

    // --- Seminar details ---

    /** 棚卸のセミナー明細一覧を返す。 */
    @GetMapping("/{id}/seminar-details")
    public SeminarDetailsResponse getSeminarDetails(@PathVariable int id, @AuthenticationPrincipal(expression = "user") User user) {
        return new SeminarDetailsResponse(
                inventoryService.findSeminarDetails(id, user).stream().map(SeminarDetailResponse::from).toList());
    }

    /** セミナー明細を全件洗い替えで保存する。 */
    @PutMapping("/{id}/seminar-details")
    public SeminarDetailsResponse saveSeminarDetails(@PathVariable int id,
                                                      @AuthenticationPrincipal(expression = "user") User user,
                                                      @RequestBody SeminarDetailsRequest req) {
        List<SeminarDetail> saved = inventoryService.saveSeminarDetails(id, user, mapper.toSeminarCommands(req.items()));
        return new SeminarDetailsResponse(saved.stream().map(SeminarDetailResponse::from).toList());
    }

    // --- Submit ---

    /** 棚卸を提出する。提出後のステータスは PENDING_GOAL（目標設定待ち）になる。 */
    @PostMapping("/{id}/submit")
    public SubmitResponse submit(@PathVariable int id, @AuthenticationPrincipal(expression = "user") User user) {
        Inventory inv = inventoryService.submit(id, user);
        return new SubmitResponse(inv.getId(), inv.getStatus().name(),
                inv.getSubmittedAt() != null ? inv.getSubmittedAt().toString() : null);
    }

    // --- Comparison ---

    /** 今年度と前年度のITスキルレベルを比較したデータを返す。前年度がなければ空リストを返す。 */
    @GetMapping("/{id}/comparison")
    public ComparisonQueryResult getComparison(@PathVariable int id, @AuthenticationPrincipal(expression = "user") User user) {
        return inventoryService.getComparison(id, user);
    }

    // --- Goal review ---

    /** 前年度に設定した目標の達成状況レビューデータを返す。前年度の目標がなければ空リストを返す。 */
    @GetMapping("/{id}/goal-review")
    public GoalReviewQueryResult getGoalReview(@PathVariable int id, @AuthenticationPrincipal(expression = "user") User user) {
        return inventoryService.getGoalReview(id, user);
    }

    /** 前年度の目標に達成状況・振り返りメモを保存する。 */
    @PutMapping("/{id}/goal-review")
    public GoalReviewQueryResult saveGoalReview(@PathVariable int id, @AuthenticationPrincipal(expression = "user") User user,
                                                @RequestBody GoalReviewUpdateRequest req) {
        return inventoryService.saveGoalReview(id, user, mapper.toGoalReviewUpdateCommands(req.items()));
    }

    /** 目標振り返りを完了させる（goal_review_completed_at を設定）。 */
    @PostMapping("/{id}/goal-review/complete")
    public GoalReviewCompleteResponse completeGoalReview(@PathVariable int id, @AuthenticationPrincipal(expression = "user") User user) {
        Inventory inv = inventoryService.completeGoalReview(id, user);
        return new GoalReviewCompleteResponse(inv.getId(),
                inv.getGoalReviewCompletedAt() != null ? inv.getGoalReviewCompletedAt().toString() : null);
    }

    // --- Goals ---

    /** 今年度の目標一覧を返す。 */
    @GetMapping("/{id}/goals")
    public GoalsResponse getGoals(@PathVariable int id, @AuthenticationPrincipal(expression = "user") User user) {
        List<InventoryGoal> goals = inventoryService.findGoals(id, user);
        return new GoalsResponse(goals.stream().map(GoalResponse::from).toList());
    }

    /** 目標を全件洗い替えで保存する。 */
    @PutMapping("/{id}/goals")
    public GoalsResponse saveGoals(@PathVariable int id, @AuthenticationPrincipal(expression = "user") User user,
                                    @RequestBody GoalsRequest req) {
        List<InventoryGoal> saved = inventoryService.saveGoals(id, user, mapper.toGoalCommands(req.items()));
        return new GoalsResponse(saved.stream().map(GoalResponse::from).toList());
    }

    /** 目標設定を完了させる。ITスキル/資格1件以上・AD2件が不足している場合は 422 エラーを返す。 */
    @PostMapping("/{id}/goals/complete")
    public GoalCompleteResponse completeGoal(@PathVariable int id, @AuthenticationPrincipal(expression = "user") User user) {
        Inventory inv = inventoryService.completeGoal(id, user);
        return new GoalCompleteResponse(inv.getId(), inv.getStatus().name(),
                inv.getGoalCompletedAt() != null ? inv.getGoalCompletedAt().toString() : null);
    }
}
