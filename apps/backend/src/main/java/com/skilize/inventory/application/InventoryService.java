package com.skilize.inventory.application;

import com.skilize.fiscalyear.domain.FiscalYearRepository;
import com.skilize.inventory.domain.*;
import com.skilize.inventory.dto.*;
import com.skilize.inventory.dto.ComparisonResponse.ComparisonItem;
import com.skilize.inventory.dto.GoalReviewResponse.GoalReviewItem;
import com.skilize.master.domain.*;
import com.skilize.shared.domain.exception.AuthException;
import com.skilize.shared.domain.exception.GoalIncompleteException;
import com.skilize.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 棚卸の作成・明細更新・提出・目標管理のビジネスロジック。
 * 明細（ITスキル・資格・セミナー）は全件洗い替え方式（先に全削除→再 INSERT）で更新する。
 * 目標完了時に件数バリデーション（ITスキル/資格 ≥1 件・AD ≥2 件）を行い、InventoryCompletedEvent を発火する。
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ItSkillDetailRepository itSkillDetailRepository;
    private final QualificationDetailRepository qualificationDetailRepository;
    private final SeminarDetailRepository seminarDetailRepository;
    private final InventoryGoalRepository inventoryGoalRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final SkillLevelRepository skillLevelRepository;
    private final ItSkillRepository itSkillRepository;
    private final QualificationRepository qualificationRepository;
    private final AdSeminarRepository adSeminarRepository;
    private final SeminarCategoryRepository seminarCategoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    // --- Inventory header ---

    // readOnly = true: Hibernate の dirty checking（変更検知）を無効化してパフォーマンスを向上させる。
    // 書き込みが発生しないことを保証し、DB が読み取りレプリカへのルーティングをサポートする場合にも有効。
    @Transactional(readOnly = true)
    public List<Inventory> findMine(int userId) {
        return inventoryRepository.findByUserIdWithFiscalYear(userId);
    }

    /**
     * 棚卸を新規作成する。ユーザーと年度の組み合わせで1件のみ作成可能（重複は 409 Conflict）。
     */
    @Transactional
    public Inventory create(User user, int fiscalYearId) {
        fiscalYearRepository.findById(fiscalYearId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "年度が見つかりません"));
        // 同一ユーザー×年度の棚卸がすでにある場合は重複エラーを返す
        inventoryRepository.findByUserIdAndFiscalYearId(user.getId(), fiscalYearId).ifPresent(i -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当該年度の棚卸はすでに作成されています");
        });
        var fy = fiscalYearRepository.findById(fiscalYearId).orElseThrow();
        return inventoryRepository.save(Inventory.create(user, fy));
    }

    /**
     * 棚卸をIDで取得する。アクセス権限チェックも同時に行う。
     * TL/ADMIN は他ユーザーの棚卸も参照可（チーム照会・面談用途）。
     */
    @Transactional(readOnly = true)
    public Inventory findById(int id, User user) {
        // findByIdWithAssociations: user・fiscalYear を JOIN FETCH して N+1 問題を防ぐカスタムクエリ
        Inventory inv = inventoryRepository.findByIdWithAssociations(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "棚卸が見つかりません"));
        checkOwnership(inv, user);
        return inv;
    }

    // --- IT Skill details ---

    @Transactional
    public List<ItSkillDetail> saveItSkillDetails(int inventoryId, User user,
                                                   List<ItSkillDetailItem> items) {
        Inventory inv = findById(inventoryId, user);
        // 全件洗い替え: 差分更新ではなく「全削除 → 全 INSERT」で明細を更新する。
        // 追加・削除・並び替えを一度の PUT で処理でき、差分ロジックを持たなくて済む。
        itSkillDetailRepository.deleteByInventoryId(inventoryId);
        List<ItSkillDetail> saved = items.stream().map(item -> {
            ItSkill skill = item.itSkillId() != null
                    ? itSkillRepository.findById(item.itSkillId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ITスキルが見つかりません"))
                    : null;
            SkillLevel level = skillLevelRepository.findById(item.skillLevelId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "レベルが見つかりません"));
            return ItSkillDetail.create(inv, skill, item.customSkillName(), level, item.remarks());
        }).toList();
        itSkillDetailRepository.saveAll(saved);
        return itSkillDetailRepository.findByInventoryId(inventoryId);
    }

    @Transactional(readOnly = true)
    public List<ItSkillDetail> findItSkillDetails(int inventoryId, User user) {
        findById(inventoryId, user);
        return itSkillDetailRepository.findByInventoryId(inventoryId);
    }

    @Transactional
    public ItSkillDetail updateItSkillDetailRemarks(int inventoryId, int detailId, User user, String remarks) {
        findById(inventoryId, user);
        ItSkillDetail detail = itSkillDetailRepository.findById(detailId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "明細が見つかりません"));
        if (!detail.getInventory().getId().equals(inventoryId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "アクセス権限がありません");
        }
        detail.updateRemarks(remarks);
        return itSkillDetailRepository.save(detail);
    }

    // --- Qualification details ---

    @Transactional
    public List<QualificationDetail> saveQualificationDetails(int inventoryId, User user,
                                                               List<QualificationDetailItem> items) {
        Inventory inv = findById(inventoryId, user);
        // 全件洗い替え（ITスキル明細と同じパターン）
        qualificationDetailRepository.deleteByInventoryId(inventoryId);
        List<QualificationDetail> saved = items.stream().map(item -> {
            Qualification q = item.qualificationId() != null
                    ? qualificationRepository.findById(item.qualificationId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "資格が見つかりません"))
                    : null;
            // LocalDate.parse() はフォーマット未指定の場合 ISO-8601（yyyy-MM-dd）を前提とする
            LocalDate date = item.acquiredYearMonth() != null ? LocalDate.parse(item.acquiredYearMonth()) : null;
            return QualificationDetail.create(inv, q, item.customQualificationName(), date, item.remarks());
        }).toList();
        qualificationDetailRepository.saveAll(saved);
        return qualificationDetailRepository.findByInventoryId(inventoryId);
    }

    @Transactional(readOnly = true)
    public List<QualificationDetail> findQualificationDetails(int inventoryId, User user) {
        findById(inventoryId, user);
        return qualificationDetailRepository.findByInventoryId(inventoryId);
    }

    // --- Seminar details ---

    @Transactional
    public List<SeminarDetail> saveSeminarDetails(int inventoryId, User user,
                                                   List<SeminarDetailItem> items) {
        Inventory inv = findById(inventoryId, user);
        // 全件洗い替え（ITスキル明細と同じパターン）
        seminarDetailRepository.deleteByInventoryId(inventoryId);
        List<SeminarDetail> saved = items.stream().map(item -> {
            AdSeminar ad = item.adSeminarId() != null
                    ? adSeminarRepository.findById(item.adSeminarId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ADセミナーが見つかりません"))
                    : null;
            SeminarCategory cat = (item.seminarCategoryId() != null && item.adSeminarId() == null)
                    ? seminarCategoryRepository.findById(item.seminarCategoryId()).orElse(null)
                    : null;
            LocalDate date = item.attendedYearMonth() != null ? LocalDate.parse(item.attendedYearMonth()) : null;
            return SeminarDetail.create(inv, ad, item.seminarName(), cat, date, item.remarks());
        }).toList();
        seminarDetailRepository.saveAll(saved);
        return seminarDetailRepository.findByInventoryId(inventoryId);
    }

    @Transactional(readOnly = true)
    public List<SeminarDetail> findSeminarDetails(int inventoryId, User user) {
        findById(inventoryId, user);
        return seminarDetailRepository.findByInventoryId(inventoryId);
    }

    // --- Submit ---

    /**
     * 棚卸を提出する。ステータスを DRAFT → PENDING_GOAL に変更し、提出日時を記録する。
     * 提出後は前年度目標の振り返り → 今年度目標設定 の流れに進む。
     */
    @Transactional
    public Inventory submit(int inventoryId, User user) {
        Inventory inv = findById(inventoryId, user);
        // ドメインメソッド submit() でステータス変更と提出日時の記録を一括して行う
        inv.submit();
        return inventoryRepository.save(inv);
    }

    // --- Comparison ---

    /**
     * 前年度との ITスキルレベル比較データを取得する。
     * 前年度棚卸が存在しない場合は hasPrevYear=false で返す。
     * カスタムスキル（マスタ未登録）は itSkill が null のため比較対象外とする。
     */
    @Transactional(readOnly = true)
    public ComparisonResponse getComparison(int inventoryId, User user) {
        Inventory inv = findById(inventoryId, user);
        String currentFy = inv.getFiscalYear().getName();

        List<ItSkillDetail> currentDetails = itSkillDetailRepository.findByInventoryId(inventoryId);

        List<Inventory> allInventories = inventoryRepository.findByUserIdWithFiscalYear(inv.getUser().getId());
        // 現在の棚卸を除外し、現在の年度開始日より前に終了している棚卸を「前年度」とする
        Inventory prevInv = allInventories.stream()
                .filter(i -> !i.getId().equals(inventoryId))
                .filter(i -> i.getFiscalYear().getEndDate().isBefore(inv.getFiscalYear().getStartDate()))
                .findFirst().orElse(null);

        if (prevInv == null) {
            return new ComparisonResponse(inventoryId, currentFy, null, false, List.of());
        }

        List<ItSkillDetail> prevDetails = itSkillDetailRepository.findByInventoryId(prevInv.getId());
        // 前年度明細をスキルID でインデックス化して、O(1) で参照できるようにする
        Map<Integer, ItSkillDetail> prevBySkillId = prevDetails.stream()
                .filter(d -> d.getItSkill() != null)
                .collect(Collectors.toMap(d -> d.getItSkill().getId(), d -> d));

        List<ComparisonItem> items = currentDetails.stream()
                .filter(d -> d.getItSkill() != null)
                .map(d -> {
                    ItSkillDetail prev = prevBySkillId.get(d.getItSkill().getId());
                    Short currentLv = d.getSkillLevel().getLevelValue();
                    Short prevLv = prev != null ? prev.getSkillLevel().getLevelValue() : null;
                    Integer diff = (prevLv != null) ? (int) currentLv - (int) prevLv : null;
                    return new ComparisonItem(
                            d.getItSkill().getId(), d.getItSkill().getName(),
                            d.getId(), (int) currentLv, d.getRemarks(),
                            prevLv != null ? (int) prevLv : null, diff);
                }).toList();

        return new ComparisonResponse(inventoryId, currentFy, prevInv.getFiscalYear().getName(), true, items);
    }

    // --- Goal review ---

    /**
     * 前年度に設定した目標の振り返りデータを取得する。
     * 前年度の目標（InventoryGoal）を取得し、振り返りステータス・コメントを含めて返す。
     * 前年度棚卸が存在しない場合は hasPrevGoals=false で返す。
     */
    @Transactional(readOnly = true)
    public GoalReviewResponse getGoalReview(int inventoryId, User user) {
        Inventory inv = findById(inventoryId, user);

        List<Inventory> allInventories = inventoryRepository.findByUserIdWithFiscalYear(inv.getUser().getId());
        Inventory prevInv = allInventories.stream()
                .filter(i -> !i.getId().equals(inventoryId))
                .filter(i -> i.getFiscalYear().getEndDate().isBefore(inv.getFiscalYear().getStartDate()))
                .findFirst().orElse(null);

        if (prevInv == null) {
            return new GoalReviewResponse(null, false, List.of());
        }

        List<InventoryGoal> prevGoals = inventoryGoalRepository.findByInventoryId(prevInv.getId());
        List<GoalReviewItem> items = prevGoals.stream()
                .map(g -> {
                    String name = resolveGoalName(g);
                    return new GoalReviewItem(
                            g.getId(), g.getGoalCategory().name(), name,
                            g.getTargetPeriod().toString(), g.getReason(),
                            g.getAchievementStatus() != null ? g.getAchievementStatus().name() : null,
                            g.getReviewNote());
                }).toList();

        return new GoalReviewResponse(prevInv.getFiscalYear().getName(), !items.isEmpty(), items);
    }

    /**
     * 前年度目標の振り返り（達成状況・コメント）を保存する。
     * 目標ごとに達成状況（ACHIEVED/PARTIAL/NOT_ACHIEVED）とコメントを個別に更新する。
     */
    @Transactional
    public GoalReviewResponse saveGoalReview(int inventoryId, User user,
                                             List<GoalReviewUpdateItem> items) {
        findById(inventoryId, user);
        items.forEach(item -> {
            InventoryGoal goal = inventoryGoalRepository.findById(item.prevGoalId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "目標が見つかりません"));
            // 達成状況が null（未入力）の場合は null のまま保存する（未振り返りとして扱う）
            // AchievementStatus.valueOf(): 文字列 "ACHIEVED" 等を Enum に変換する
            AchievementStatus status = item.achievementStatus() != null
                    ? AchievementStatus.valueOf(item.achievementStatus()) : null;
            goal.updateReview(status, item.reviewNote());
            inventoryGoalRepository.save(goal);
        });
        // 保存後の最新状態を取得して返す
        return getGoalReview(inventoryId, user);
    }

    /**
     * 目標振り返りを完了する。ステータスは変えず、goal_review_completed_at のみを記録する。
     * 振り返りは任意フロー（完了しなくても次のステップに進める）。
     */
    @Transactional
    public Inventory completeGoalReview(int inventoryId, User user) {
        Inventory inv = findById(inventoryId, user);
        inv.completeGoalReview();
        return inventoryRepository.save(inv);
    }

    // --- Goals ---

    @Transactional(readOnly = true)
    public List<InventoryGoal> findGoals(int inventoryId, User user) {
        findById(inventoryId, user);
        return inventoryGoalRepository.findByInventoryId(inventoryId);
    }

    @Transactional
    public List<InventoryGoal> saveGoals(int inventoryId, User user, List<GoalItem> items) {
        Inventory inv = findById(inventoryId, user);
        // 全件洗い替え（ITスキル明細と同じパターン）
        inventoryGoalRepository.deleteByInventoryId(inventoryId);
        List<InventoryGoal> saved = items.stream().map(item -> {
            ItSkill skill = item.itSkillId() != null
                    ? itSkillRepository.findById(item.itSkillId()).orElse(null) : null;
            Qualification qual = item.qualificationId() != null
                    ? qualificationRepository.findById(item.qualificationId()).orElse(null) : null;
            AdSeminar ad = item.adSeminarId() != null
                    ? adSeminarRepository.findById(item.adSeminarId()).orElse(null) : null;
            LocalDate period = LocalDate.parse(item.targetPeriod());
            return InventoryGoal.create(inv, GoalCategory.valueOf(item.goalCategory()),
                    skill, qual, ad, item.customName(), period, item.reason());
        }).toList();
        inventoryGoalRepository.saveAll(saved);
        return inventoryGoalRepository.findByInventoryId(inventoryId);
    }

    @Transactional
    public Inventory completeGoal(int inventoryId, User user) {
        Inventory inv = findById(inventoryId, user);
        List<InventoryGoal> goals = inventoryGoalRepository.findByInventoryId(inventoryId);

        // 目標完了の条件: ITスキル/資格 ≥1 件 AND AD ≥2 件
        long itOrQual = goals.stream()
                .filter(g -> g.getGoalCategory() == GoalCategory.IT_SKILL
                        || g.getGoalCategory() == GoalCategory.QUALIFICATION)
                .count();
        long ad = goals.stream().filter(g -> g.getGoalCategory() == GoalCategory.AD).count();

        if (itOrQual < 1 || ad < 2) {
            List<GoalIncompleteException.GoalValidationError> errors = new java.util.ArrayList<>();
            if (itOrQual < 1) errors.add(new GoalIncompleteException.GoalValidationError(
                    "itSkillOrQualification", "ITスキル・資格の目標を 1 件以上入力してください"));
            if (ad < 2) errors.add(new GoalIncompleteException.GoalValidationError(
                    "ad", "ADの目標を 2 件すべて入力してください"));
            throw new GoalIncompleteException(errors);
        }

        inv.completeGoal();
        Inventory saved = inventoryRepository.save(inv);
        // ApplicationEventPublisher: Spring のプロセス内イベントバス。
        // publishEvent() を呼ぶと、同一 JVM 内の @TransactionalEventListener が受け取る。
        // イベントリスナーは AFTER_COMMIT フェーズで動作するため、このトランザクションの確定後に AI 分析が始まる。
        eventPublisher.publishEvent(new InventoryCompletedEvent(saved.getUser().getId(), saved.getFiscalYear().getId()));
        return saved;
    }

    private void checkOwnership(Inventory inv, User user) {
        if (!inv.getUser().getId().equals(user.getId())) {
            // TL/ADMIN は他ユーザーの棚卸を参照可（チーム照会・面談用途）。GENERAL は自分のみ。
            String role = user.getRole().name();
            if (!"TL".equals(role) && !"ADMIN".equals(role)) {
                throw new AuthException("FORBIDDEN", "アクセス権限がありません");
            }
        }
    }

    private String resolveGoalName(InventoryGoal g) {
        if (g.getItSkill() != null) return g.getItSkill().getName();
        if (g.getQualification() != null) return g.getQualification().getName();
        if (g.getAdSeminar() != null) return g.getAdSeminar().getName();
        return g.getCustomName();
    }

}
