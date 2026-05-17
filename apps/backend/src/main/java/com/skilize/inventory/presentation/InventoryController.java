package com.skilize.inventory.presentation;

import com.skilize.inventory.application.InventoryService;
import com.skilize.inventory.domain.*;
import com.skilize.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 棚卸（スキル棚卸ヘッダー・明細・提出・目標）の REST API コントローラー。
 * 認証済みユーザーなら全エンドポイントにアクセスできるが、Service 層で所有者確認を行う。
 */
@RestController
@RequestMapping("/api/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // --- Header ---

    /** 自分の棚卸一覧を取得する。全年度分を返す。 */
    @GetMapping("/mine")
    public List<InventorySummaryDto> mine(@AuthenticationPrincipal User user) {
        return inventoryService.findMine(user.getId())
                .stream().map(InventorySummaryDto::from).toList();
    }

    /** 棚卸を新規作成する。同一年度の棚卸が既に存在する場合は Service 層でエラーになる。 */
    @PostMapping
    public ResponseEntity<InventorySummaryDto> create(@AuthenticationPrincipal User user,
                                                       @RequestBody CreateInventoryRequest req) {
        Inventory inv = inventoryService.create(user, req.fiscalYearId());
        return ResponseEntity.status(HttpStatus.CREATED).body(InventorySummaryDto.from(inv));
    }

    /** 棚卸ヘッダーを取得する。他ユーザーの棚卸へのアクセスは Service 層で 403 をスローする。 */
    @GetMapping("/{id}")
    public InventoryDetailDto getById(@PathVariable int id, @AuthenticationPrincipal User user) {
        Inventory inv = inventoryService.findById(id, user);
        return InventoryDetailDto.from(inv);
    }

    // --- IT Skill details ---

    /** ITスキル明細一覧を取得する。 */
    @GetMapping("/{id}/it-skill-details")
    public ItSkillDetailsResponse getItSkillDetails(@PathVariable int id, @AuthenticationPrincipal User user) {
        return new ItSkillDetailsResponse(
                inventoryService.findItSkillDetails(id, user).stream().map(ItSkillDetailDto::from).toList());
    }

    /**
     * ITスキル明細を一括保存する（全件洗い替え）。
     * PUT のため既存明細を全削除してリクエスト内容で再登録する。
     */
    @PutMapping("/{id}/it-skill-details")
    public ItSkillDetailsResponse saveItSkillDetails(@PathVariable int id,
                                                      @AuthenticationPrincipal User user,
                                                      @RequestBody ItSkillDetailsRequest req) {
        List<ItSkillDetail> saved = inventoryService.saveItSkillDetails(id, user, req.items());
        return new ItSkillDetailsResponse(saved.stream().map(ItSkillDetailDto::from).toList());
    }

    /**
     * ITスキル明細の備考のみを部分更新する（PATCH）。
     * スキルレベルは変更せず備考欄だけ更新したい場合に使用する。
     */
    @PatchMapping("/{id}/it-skill-details/{detailId}")
    public RemarksPatchResponse patchItSkillRemarks(@PathVariable int id, @PathVariable int detailId,
                                                     @AuthenticationPrincipal User user,
                                                     @RequestBody RemarksPatchRequest req) {
        ItSkillDetail detail = inventoryService.updateItSkillDetailRemarks(id, detailId, user, req.remarks());
        return new RemarksPatchResponse(detail.getId(), detail.getRemarks());
    }

    // --- Qualification details ---

    /** 資格明細一覧を取得する。 */
    @GetMapping("/{id}/qualification-details")
    public QualificationDetailsResponse getQualificationDetails(@PathVariable int id, @AuthenticationPrincipal User user) {
        return new QualificationDetailsResponse(
                inventoryService.findQualificationDetails(id, user).stream().map(QualificationDetailDto::from).toList());
    }

    /** 資格明細を一括保存する（全件洗い替え）。 */
    @PutMapping("/{id}/qualification-details")
    public QualificationDetailsResponse saveQualificationDetails(@PathVariable int id,
                                                                   @AuthenticationPrincipal User user,
                                                                   @RequestBody QualificationDetailsRequest req) {
        List<QualificationDetail> saved = inventoryService.saveQualificationDetails(id, user, req.items());
        return new QualificationDetailsResponse(saved.stream().map(QualificationDetailDto::from).toList());
    }

    // --- Seminar details ---

    /** セミナー明細一覧を取得する。 */
    @GetMapping("/{id}/seminar-details")
    public SeminarDetailsResponse getSeminarDetails(@PathVariable int id, @AuthenticationPrincipal User user) {
        return new SeminarDetailsResponse(
                inventoryService.findSeminarDetails(id, user).stream().map(SeminarDetailDto::from).toList());
    }

    /** セミナー明細を一括保存する（全件洗い替え）。 */
    @PutMapping("/{id}/seminar-details")
    public SeminarDetailsResponse saveSeminarDetails(@PathVariable int id,
                                                      @AuthenticationPrincipal User user,
                                                      @RequestBody SeminarDetailsRequest req) {
        List<SeminarDetail> saved = inventoryService.saveSeminarDetails(id, user, req.items());
        return new SeminarDetailsResponse(saved.stream().map(SeminarDetailDto::from).toList());
    }

    // --- Submit ---

    /**
     * 棚卸を提出する。ステータスが SUBMITTED に変わり、AI分析がトリガーされる。
     * 提出後は明細の編集が不可になる（Service 層でガード）。
     */
    @PostMapping("/{id}/submit")
    public SubmitResponse submit(@PathVariable int id, @AuthenticationPrincipal User user) {
        Inventory inv = inventoryService.submit(id, user);
        return new SubmitResponse(inv.getId(), inv.getStatus().name(),
                inv.getSubmittedAt() != null ? inv.getSubmittedAt().toString() : null);
    }

    // --- Comparison ---

    /** 前年度との比較データを取得する。前年度の棚卸がない場合は空のリストを返す。 */
    @GetMapping("/{id}/comparison")
    public InventoryService.ComparisonResponse getComparison(@PathVariable int id, @AuthenticationPrincipal User user) {
        return inventoryService.getComparison(id, user);
    }

    // --- Goal review ---

    /** 前年度目標の振り返り情報を取得する（目標ごとの達成状況）。 */
    @GetMapping("/{id}/goal-review")
    public InventoryService.GoalReviewResponse getGoalReview(@PathVariable int id, @AuthenticationPrincipal User user) {
        return inventoryService.getGoalReview(id, user);
    }

    /** 前年度目標の振り返り（達成状況）を保存する。 */
    @PutMapping("/{id}/goal-review")
    public InventoryService.GoalReviewResponse saveGoalReview(@PathVariable int id, @AuthenticationPrincipal User user,
                                              @RequestBody GoalReviewUpdateRequest req) {
        return inventoryService.saveGoalReview(id, user, req.items());
    }

    /** 前年度目標振り返りを完了する。完了日時が記録され、目標設定ステップへ進める。 */
    @PostMapping("/{id}/goal-review/complete")
    public GoalReviewCompleteResponse completeGoalReview(@PathVariable int id, @AuthenticationPrincipal User user) {
        Inventory inv = inventoryService.completeGoalReview(id, user);
        return new GoalReviewCompleteResponse(inv.getId(),
                inv.getGoalReviewCompletedAt() != null ? inv.getGoalReviewCompletedAt().toString() : null);
    }

    // --- Goals ---

    /** 今年度の目標一覧を取得する。 */
    @GetMapping("/{id}/goals")
    public GoalsResponse getGoals(@PathVariable int id, @AuthenticationPrincipal User user) {
        List<InventoryGoal> goals = inventoryService.findGoals(id, user);
        return new GoalsResponse(goals.stream().map(GoalDto::from).toList());
    }

    /** 今年度の目標を一括保存する（全件洗い替え）。 */
    @PutMapping("/{id}/goals")
    public GoalsResponse saveGoals(@PathVariable int id, @AuthenticationPrincipal User user,
                                    @RequestBody GoalsRequest req) {
        List<InventoryGoal> saved = inventoryService.saveGoals(id, user, req.items());
        return new GoalsResponse(saved.stream().map(GoalDto::from).toList());
    }

    /** 目標設定を完了する。ステータスが GOAL_COMPLETED に変わる。 */
    @PostMapping("/{id}/goals/complete")
    public GoalCompleteResponse completeGoal(@PathVariable int id, @AuthenticationPrincipal User user) {
        Inventory inv = inventoryService.completeGoal(id, user);
        return new GoalCompleteResponse(inv.getId(), inv.getStatus().name(),
                inv.getGoalCompletedAt() != null ? inv.getGoalCompletedAt().toString() : null);
    }

    // ===== DTOs =====

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
    public record ItSkillDetailsRequest(List<InventoryService.ItSkillDetailItem> items) {}
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
    public record QualificationDetailsRequest(List<InventoryService.QualificationDetailItem> items) {}
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
    public record SeminarDetailsRequest(List<InventoryService.SeminarDetailItem> items) {}
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

    // Goal review
    public record GoalReviewUpdateRequest(List<InventoryService.GoalReviewUpdateItem> items) {}
    public record GoalReviewCompleteResponse(int id, String goalReviewCompletedAt) {}

    // Goals
    public record GoalsRequest(List<InventoryService.GoalItem> items) {}
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
