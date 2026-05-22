package com.skilize.inventory.application;

import com.skilize.fiscalyear.domain.FiscalYearRepository;
import com.skilize.inventory.application.command.*;
import com.skilize.inventory.application.query.ComparisonQueryResult;
import com.skilize.inventory.application.query.ComparisonQueryResult.ComparisonItem;
import com.skilize.inventory.application.query.GoalReviewQueryResult;
import com.skilize.inventory.application.query.GoalReviewQueryResult.GoalReviewItem;
import com.skilize.inventory.domain.*;
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
 * スキル棚卸のビジネスロジック。
 * 棚卸ヘッダー・ITスキル/資格/セミナー明細・目標・前年比較・目標振り返りを管理する。
 * 明細の更新は全件洗い替え（deleteBy〜 → saveAll）で行う。
 * checkOwnership() で本人・TL・ADMIN のみが棚卸にアクセスできるよう制御する。
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

    /** 指定ユーザーの棚卸一覧を年度情報付きで返す（年度降順）。 */
    @Transactional(readOnly = true)
    public List<Inventory> findMine(int userId) {
        return inventoryRepository.findByUserIdWithFiscalYear(userId);
    }

    /** 棚卸を新規作成する。同年度に既存棚卸がある場合は 409 CONFLICT をスローする。 */
    @Transactional
    public Inventory create(User user, int fiscalYearId) {
        fiscalYearRepository.findById(fiscalYearId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "年度が見つかりません"));
        inventoryRepository.findByUserIdAndFiscalYearId(user.getId(), fiscalYearId).ifPresent(i -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当該年度の棚卸はすでに作成されています");
        });
        var fy = fiscalYearRepository.findById(fiscalYearId).orElseThrow();
        return inventoryRepository.save(Inventory.create(user, fy));
    }

    /** 棚卸を ID で取得し、所有権チェックを行う。 */
    @Transactional(readOnly = true)
    public Inventory findById(int id, User user) {
        Inventory inv = inventoryRepository.findByIdWithAssociations(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "棚卸が見つかりません"));
        checkOwnership(inv, user);
        return inv;
    }

    // --- IT Skill details ---

    /**
     * ITスキル明細を全件洗い替えで保存する。
     * 既存の明細を全削除してから新規 INSERT するため、エラー時はトランザクション rollback により整合性を保つ。
     */
    @Transactional
    public List<ItSkillDetail> saveItSkillDetails(int inventoryId, User user,
                                                   List<ItSkillDetailCommand> commands) {
        Inventory inv = findById(inventoryId, user);
        itSkillDetailRepository.deleteByInventoryId(inventoryId);
        List<ItSkillDetail> saved = commands.stream().map(cmd -> {
            ItSkill skill = cmd.itSkillId() != null
                    ? itSkillRepository.findById(cmd.itSkillId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ITスキルが見つかりません"))
                    : null;
            SkillLevel level = skillLevelRepository.findById(cmd.skillLevelId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "レベルが見つかりません"));
            return ItSkillDetail.create(inv, skill, cmd.customSkillName(), level, cmd.remarks());
        }).toList();
        itSkillDetailRepository.saveAll(saved);
        return itSkillDetailRepository.findByInventoryId(inventoryId);
    }

    /** 棚卸のITスキル明細一覧を返す。 */
    @Transactional(readOnly = true)
    public List<ItSkillDetail> findItSkillDetails(int inventoryId, User user) {
        findById(inventoryId, user);
        return itSkillDetailRepository.findByInventoryId(inventoryId);
    }

    /**
     * ITスキル明細の備考のみを更新する。
     * 明細が指定棚卸に属するかを確認し、他の棚卸の明細へのアクセスは 403 を返す。
     */
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

    /** 資格明細を全件洗い替えで保存する。取得年月は "yyyy-MM-dd" 形式の文字列を LocalDate に変換する。 */
    @Transactional
    public List<QualificationDetail> saveQualificationDetails(int inventoryId, User user,
                                                               List<QualificationDetailCommand> commands) {
        Inventory inv = findById(inventoryId, user);
        qualificationDetailRepository.deleteByInventoryId(inventoryId);
        List<QualificationDetail> saved = commands.stream().map(cmd -> {
            Qualification q = cmd.qualificationId() != null
                    ? qualificationRepository.findById(cmd.qualificationId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "資格が見つかりません"))
                    : null;
            LocalDate date = cmd.acquiredYearMonth() != null ? LocalDate.parse(cmd.acquiredYearMonth()) : null;
            return QualificationDetail.create(inv, q, cmd.customQualificationName(), date, cmd.remarks());
        }).toList();
        qualificationDetailRepository.saveAll(saved);
        return qualificationDetailRepository.findByInventoryId(inventoryId);
    }

    /** 棚卸の資格明細一覧を返す。 */
    @Transactional(readOnly = true)
    public List<QualificationDetail> findQualificationDetails(int inventoryId, User user) {
        findById(inventoryId, user);
        return qualificationDetailRepository.findByInventoryId(inventoryId);
    }

    // --- Seminar details ---

    /**
     * セミナー明細を全件洗い替えで保存する。
     * ADセミナーが指定されている場合はセミナー分類を無視する（AD明細にカテゴリは不要）。
     */
    @Transactional
    public List<SeminarDetail> saveSeminarDetails(int inventoryId, User user,
                                                   List<SeminarDetailCommand> commands) {
        Inventory inv = findById(inventoryId, user);
        seminarDetailRepository.deleteByInventoryId(inventoryId);
        List<SeminarDetail> saved = commands.stream().map(cmd -> {
            AdSeminar ad = cmd.adSeminarId() != null
                    ? adSeminarRepository.findById(cmd.adSeminarId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ADセミナーが見つかりません"))
                    : null;
            SeminarCategory cat = (cmd.seminarCategoryId() != null && cmd.adSeminarId() == null)
                    ? seminarCategoryRepository.findById(cmd.seminarCategoryId()).orElse(null)
                    : null;
            LocalDate date = cmd.attendedYearMonth() != null ? LocalDate.parse(cmd.attendedYearMonth()) : null;
            return SeminarDetail.create(inv, ad, cmd.seminarName(), cat, date, cmd.remarks());
        }).toList();
        seminarDetailRepository.saveAll(saved);
        return seminarDetailRepository.findByInventoryId(inventoryId);
    }

    /** 棚卸のセミナー明細一覧を返す。 */
    @Transactional(readOnly = true)
    public List<SeminarDetail> findSeminarDetails(int inventoryId, User user) {
        findById(inventoryId, user);
        return seminarDetailRepository.findByInventoryId(inventoryId);
    }

    // --- Submit ---

    /** 棚卸を提出する。ステータスが PENDING_GOAL に遷移する。 */
    @Transactional
    public Inventory submit(int inventoryId, User user) {
        Inventory inv = findById(inventoryId, user);
        inv.submit();
        return inventoryRepository.save(inv);
    }

    // --- Comparison ---

    /**
     * 今年度と前年度のITスキルレベル差分を計算して返す。
     * 前年度棚卸は今年度の startDate より以前に終了した棚卸を検索する。
     * 同一マスタスキルがある場合のみ差分（diff = 今年度レベル − 前年度レベル）を計算する。
     * カスタムスキルはマスタと紐付かないため差分計算の対象外（diff=null）。
     */
    @Transactional(readOnly = true)
    public ComparisonQueryResult getComparison(int inventoryId, User user) {
        Inventory inv = findById(inventoryId, user);
        String currentFy = inv.getFiscalYear().getName();

        List<ItSkillDetail> currentDetails = itSkillDetailRepository.findByInventoryId(inventoryId);

        List<Inventory> allInventories = inventoryRepository.findByUserIdWithFiscalYear(inv.getUser().getId());
        Inventory prevInv = allInventories.stream()
                .filter(i -> !i.getId().equals(inventoryId))
                .filter(i -> i.getFiscalYear().getEndDate().isBefore(inv.getFiscalYear().getStartDate()))
                .findFirst().orElse(null);

        if (prevInv == null) {
            return new ComparisonQueryResult(inventoryId, currentFy, null, false, List.of());
        }

        List<ItSkillDetail> prevDetails = itSkillDetailRepository.findByInventoryId(prevInv.getId());
        Map<Integer, ItSkillDetail> prevBySkillId = prevDetails.stream()
                .filter(d -> d.getItSkill() != null)
                .collect(Collectors.toMap(d -> d.getItSkill().getId(), d -> d));

        List<ComparisonItem> items = currentDetails.stream()
                .map(d -> {
                    if (d.getItSkill() == null) {
                        return new ComparisonItem(null, d.getCustomSkillName(), d.getId(), null, d.getRemarks(), null, null);
                    }
                    ItSkillDetail prev = prevBySkillId.get(d.getItSkill().getId());
                    Short currentLv = d.getSkillLevel().getLevelValue();
                    Short prevLv = prev != null ? prev.getSkillLevel().getLevelValue() : null;
                    Integer diff = (prevLv != null) ? (int) currentLv - (int) prevLv : null;
                    return new ComparisonItem(
                            d.getItSkill().getId(), d.getItSkill().getName(),
                            d.getId(), (int) currentLv, d.getRemarks(),
                            prevLv != null ? (int) prevLv : null, diff);
                }).toList();

        return new ComparisonQueryResult(inventoryId, currentFy, prevInv.getFiscalYear().getName(), true, items);
    }

    // --- Goal review ---

    /**
     * 前年度の目標一覧と各目標への達成状況・振り返りメモを返す。
     * 前年度棚卸が存在しない場合は空レスポンスを返す（404 にはしない）。
     */
    @Transactional(readOnly = true)
    public GoalReviewQueryResult getGoalReview(int inventoryId, User user) {
        Inventory inv = findById(inventoryId, user);

        List<Inventory> allInventories = inventoryRepository.findByUserIdWithFiscalYear(inv.getUser().getId());
        Inventory prevInv = allInventories.stream()
                .filter(i -> !i.getId().equals(inventoryId))
                .filter(i -> i.getFiscalYear().getEndDate().isBefore(inv.getFiscalYear().getStartDate()))
                .findFirst().orElse(null);

        if (prevInv == null) {
            return new GoalReviewQueryResult(null, false, List.of());
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

        return new GoalReviewQueryResult(prevInv.getFiscalYear().getName(), !items.isEmpty(), items);
    }

    /** 前年度の目標に達成状況・振り返りメモを保存する。 */
    @Transactional
    public GoalReviewQueryResult saveGoalReview(int inventoryId, User user,
                                                List<GoalReviewUpdateCommand> commands) {
        findById(inventoryId, user);
        commands.forEach(cmd -> {
            InventoryGoal goal = inventoryGoalRepository.findById(cmd.prevGoalId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "目標が見つかりません"));
            AchievementStatus status = cmd.achievementStatus() != null
                    ? AchievementStatus.valueOf(cmd.achievementStatus()) : null;
            goal.updateReview(status, cmd.reviewNote());
            inventoryGoalRepository.save(goal);
        });
        return getGoalReview(inventoryId, user);
    }

    /** 目標振り返りを完了させる（goal_review_completed_at を設定）。 */
    @Transactional
    public Inventory completeGoalReview(int inventoryId, User user) {
        Inventory inv = findById(inventoryId, user);
        inv.completeGoalReview();
        return inventoryRepository.save(inv);
    }

    // --- Goals ---

    /** 今年度の目標一覧を返す。 */
    @Transactional(readOnly = true)
    public List<InventoryGoal> findGoals(int inventoryId, User user) {
        findById(inventoryId, user);
        return inventoryGoalRepository.findByInventoryId(inventoryId);
    }

    /** 目標を全件洗い替えで保存する。目標期間は "yyyy-MM-dd" 形式の文字列を LocalDate に変換する。 */
    @Transactional
    public List<InventoryGoal> saveGoals(int inventoryId, User user, List<GoalCommand> commands) {
        Inventory inv = findById(inventoryId, user);
        inventoryGoalRepository.deleteByInventoryId(inventoryId);
        List<InventoryGoal> saved = commands.stream().map(cmd -> {
            ItSkill skill = cmd.itSkillId() != null
                    ? itSkillRepository.findById(cmd.itSkillId()).orElse(null) : null;
            Qualification qual = cmd.qualificationId() != null
                    ? qualificationRepository.findById(cmd.qualificationId()).orElse(null) : null;
            AdSeminar ad = cmd.adSeminarId() != null
                    ? adSeminarRepository.findById(cmd.adSeminarId()).orElse(null) : null;
            LocalDate period = LocalDate.parse(cmd.targetPeriod());
            return InventoryGoal.create(inv, GoalCategory.valueOf(cmd.goalCategory()),
                    skill, qual, ad, cmd.customName(), period, cmd.reason());
        }).toList();
        inventoryGoalRepository.saveAll(saved);
        return inventoryGoalRepository.findByInventoryId(inventoryId);
    }

    /**
     * 目標設定を完了させる。
     * 完了条件: ITスキル/資格カテゴリの目標が1件以上 かつ AD カテゴリの目標が2件以上。
     * 条件未達の場合は GoalIncompleteException をスローする（フロントエンドが項目別エラーを表示）。
     * 完了後に InventoryCompletedEvent を発行し、AI 分析を非同期でトリガーする。
     */
    @Transactional
    public Inventory completeGoal(int inventoryId, User user) {
        Inventory inv = findById(inventoryId, user);
        List<InventoryGoal> goals = inventoryGoalRepository.findByInventoryId(inventoryId);

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
        eventPublisher.publishEvent(new InventoryCompletedEvent(saved.getUser().getId(), saved.getFiscalYear().getId()));
        return saved;
    }

    /**
     * 棚卸の所有権チェック。
     * 本人（棚卸の user_id と一致）は常に許可。TL/ADMIN は他ユーザーの棚卸も参照可。
     * GENERAL ロールが他ユーザーの棚卸にアクセスした場合は 403 をスローする。
     */
    private void checkOwnership(Inventory inv, User user) {
        if (!inv.getUser().getId().equals(user.getId())) {
            String role = user.getRole().name();
            if (!"TL".equals(role) && !"ADMIN".equals(role)) {
                throw new AuthException("FORBIDDEN", "アクセス権限がありません");
            }
        }
    }

    /** 目標名称を参照先の優先順位（ITスキル → 資格 → ADセミナー → カスタム名）で解決して返す。 */
    private String resolveGoalName(InventoryGoal g) {
        if (g.getItSkill() != null) return g.getItSkill().getName();
        if (g.getQualification() != null) return g.getQualification().getName();
        if (g.getAdSeminar() != null) return g.getAdSeminar().getName();
        return g.getCustomName();
    }
}
