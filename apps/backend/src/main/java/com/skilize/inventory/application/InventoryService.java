/**************************************************************************************************************
 * 機能ID      ：INV
 * 機能名      ：棚卸管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * スキル棚卸のビジネスロジックを担うアプリケーションサービス。
 * 棚卸ヘッダー・明細（ITスキル/資格/セミナー）・目標・目標振り返り・前年比較の各操作を提供する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.inventory.application;

import com.skilize.fiscalyear.domain.model.FiscalYear;
import com.skilize.fiscalyear.domain.repository.FiscalYearRepository;
import com.skilize.inventory.application.command.*;
import com.skilize.inventory.application.query.ComparisonQueryResult;
import com.skilize.inventory.application.query.ComparisonQueryResult.ComparisonItem;
import com.skilize.inventory.application.query.GoalReviewQueryResult;
import com.skilize.inventory.application.query.GoalReviewQueryResult.GoalReviewItem;
import com.skilize.inventory.domain.model.*;
import com.skilize.inventory.domain.repository.*;
import com.skilize.master.domain.model.AdSeminar;
import com.skilize.master.domain.model.ItSkill;
import com.skilize.master.domain.model.Qualification;
import com.skilize.master.domain.model.SeminarCategory;
import com.skilize.master.domain.model.SkillLevel;
import com.skilize.master.domain.repository.AdSeminarRepository;
import com.skilize.master.domain.repository.ItSkillRepository;
import com.skilize.master.domain.repository.QualificationRepository;
import com.skilize.master.domain.repository.SeminarCategoryRepository;
import com.skilize.master.domain.repository.SkillLevelRepository;
import com.skilize.shared.domain.exception.AuthException;
import com.skilize.shared.domain.exception.GoalIncompleteException;
import com.skilize.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * スキル棚卸のビジネスロジック。
 * 棚卸ヘッダー・ITスキル/資格/セミナー明細・目標・前年比較・目標振り返りを管理する。
 * 明細の更新は全件洗い替え（deleteBy〜 → saveAll）で行う。
 * checkOwnership() で本人・TL・ADMIN のみが棚卸にアクセスできるよう制御する。
 */
@Slf4j
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
        Optional<FiscalYear> fiscalYearOptional = fiscalYearRepository.findById(fiscalYearId);
        if (fiscalYearOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FISCAL_YEAR_NOT_FOUND");
        }
        if (inventoryRepository.findByUserIdAndFiscalYearId(user.getId(), fiscalYearId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "INVENTORY_ALREADY_EXISTS");
        }
        FiscalYear fy = fiscalYearOptional.get();
        return inventoryRepository.save(Inventory.create(user, fy));
    }

    /** 棚卸を ID で取得し、所有権チェックを行う。 */
    @Transactional(readOnly = true)
    public Inventory findById(int id, User user) {
        Optional<Inventory> invOptional = inventoryRepository.findByIdWithAssociations(id);
        if (invOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "INVENTORY_NOT_FOUND");
        }
        Inventory inv = invOptional.get();
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
        List<ItSkillDetail> saved = new ArrayList<>();
        for (ItSkillDetailCommand cmd : commands) {
            ItSkill skill = null;
            if (cmd.itSkillId() != null) {
                Optional<ItSkill> skillOptional = itSkillRepository.findById(cmd.itSkillId());
                if (skillOptional.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "IT_SKILL_NOT_FOUND");
                }
                skill = skillOptional.get();
            }
            Optional<SkillLevel> levelOptional = skillLevelRepository.findById(cmd.skillLevelId());
            if (levelOptional.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SKILL_LEVEL_NOT_FOUND");
            }
            SkillLevel level = levelOptional.get();
            saved.add(ItSkillDetail.create(inv, skill, cmd.customSkillName(), level, cmd.remarks()));
        }
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
        Optional<ItSkillDetail> detailOptional = itSkillDetailRepository.findById(detailId);
        if (detailOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "DETAIL_NOT_FOUND");
        }
        ItSkillDetail detail = detailOptional.get();
        if (!detail.getInventoryId().equals(inventoryId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FORBIDDEN");
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
        List<QualificationDetail> saved = new ArrayList<>();
        for (QualificationDetailCommand cmd : commands) {
            Qualification q = null;
            if (cmd.qualificationId() != null) {
                Optional<Qualification> qualificationOptional = qualificationRepository.findById(cmd.qualificationId());
                if (qualificationOptional.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "QUALIFICATION_NOT_FOUND");
                }
                q = qualificationOptional.get();
            }
            LocalDate date = null;
            if (cmd.acquiredYearMonth() != null) {
                date = LocalDate.parse(cmd.acquiredYearMonth());
            }
            saved.add(QualificationDetail.create(inv, q, cmd.customQualificationName(), date, cmd.remarks()));
        }
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
        List<SeminarDetail> saved = new ArrayList<>();
        for (SeminarDetailCommand cmd : commands) {
            AdSeminar ad = null;
            if (cmd.adSeminarId() != null) {
                Optional<AdSeminar> adOptional = adSeminarRepository.findById(cmd.adSeminarId());
                if (adOptional.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "AD_SEMINAR_NOT_FOUND");
                }
                ad = adOptional.get();
            }
            SeminarCategory cat = null;
            if (cmd.seminarCategoryId() != null && cmd.adSeminarId() == null) {
                Optional<SeminarCategory> catOptional = seminarCategoryRepository.findById(cmd.seminarCategoryId());
                if (catOptional.isPresent()) {
                    cat = catOptional.get();
                }
            }
            LocalDate date = null;
            if (cmd.attendedYearMonth() != null) {
                date = LocalDate.parse(cmd.attendedYearMonth());
            }
            saved.add(SeminarDetail.create(inv, ad, cmd.seminarName(), cat, date, cmd.remarks()));
        }
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
        Inventory saved = inventoryRepository.save(inv);
        log.info("Inventory submitted: inventoryId={}", inventoryId);
        return saved;
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

        Inventory prevInv = findPrevInventory(inv, inventoryId);

        if (prevInv == null) {
            return new ComparisonQueryResult(inventoryId, currentFy, null, false, List.of());
        }

        List<ItSkillDetail> prevDetails = itSkillDetailRepository.findByInventoryId(prevInv.getId());
        Map<Integer, ItSkillDetail> prevBySkillId = new HashMap<>();
        for (ItSkillDetail d : prevDetails) {
            if (d.getItSkill() != null) {
                prevBySkillId.put(d.getItSkill().getId(), d);
            }
        }

        List<ComparisonItem> items = new ArrayList<>();
        for (ItSkillDetail d : currentDetails) {
            if (d.getItSkill() == null) {
                items.add(new ComparisonItem(null, d.getCustomSkillName(), d.getId(), null, d.getRemarks(), null, null));
                continue;
            }
            ItSkillDetail prev = prevBySkillId.get(d.getItSkill().getId());
            Short currentLv = d.getSkillLevel().getLevelValue();
            Short prevLv = null;
            if (prev != null) {
                prevLv = prev.getSkillLevel().getLevelValue();
            }
            Integer diff = null;
            Integer prevLvInt = null;
            if (prevLv != null) {
                diff = (int) currentLv - (int) prevLv;
                prevLvInt = (int) prevLv;
            }
            items.add(new ComparisonItem(
                    d.getItSkill().getId(), d.getItSkill().getName(),
                    d.getId(), (int) currentLv, d.getRemarks(),
                    prevLvInt, diff));
        }

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

        Inventory prevInv = findPrevInventory(inv, inventoryId);

        if (prevInv == null) {
            return new GoalReviewQueryResult(null, false, List.of());
        }

        List<InventoryGoal> prevGoals = inventoryGoalRepository.findByInventoryId(prevInv.getId());
        List<GoalReviewItem> items = new ArrayList<>();
        for (InventoryGoal g : prevGoals) {
            String name = resolveGoalName(g);
            String achievementStatus = null;
            if (g.getAchievementStatus() != null) {
                achievementStatus = g.getAchievementStatus().name();
            }
            items.add(new GoalReviewItem(
                    g.getId(), g.getGoalCategory().name(), name,
                    g.getTargetPeriod().toString(), g.getReason(),
                    achievementStatus,
                    g.getReviewNote()));
        }

        return new GoalReviewQueryResult(prevInv.getFiscalYear().getName(), !items.isEmpty(), items);
    }

    /** 前年度の目標に達成状況・振り返りメモを保存する。 */
    @Transactional
    public GoalReviewQueryResult saveGoalReview(int inventoryId, User user,
                                                List<GoalReviewUpdateCommand> commands) {
        findById(inventoryId, user);
        for (GoalReviewUpdateCommand cmd : commands) {
            Optional<InventoryGoal> goalOptional = inventoryGoalRepository.findById(cmd.prevGoalId());
            if (goalOptional.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "GOAL_NOT_FOUND");
            }
            InventoryGoal goal = goalOptional.get();
            AchievementStatus status = null;
            if (cmd.achievementStatus() != null) {
                status = AchievementStatus.valueOf(cmd.achievementStatus());
            }
            goal.updateReview(status, cmd.reviewNote());
            inventoryGoalRepository.save(goal);
        }
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
        List<InventoryGoal> saved = new ArrayList<>();
        for (GoalCommand cmd : commands) {
            ItSkill skill = null;
            if (cmd.itSkillId() != null) {
                Optional<ItSkill> skillOptional = itSkillRepository.findById(cmd.itSkillId());
                if (skillOptional.isPresent()) {
                    skill = skillOptional.get();
                }
            }
            Qualification qual = null;
            if (cmd.qualificationId() != null) {
                Optional<Qualification> qualOptional = qualificationRepository.findById(cmd.qualificationId());
                if (qualOptional.isPresent()) {
                    qual = qualOptional.get();
                }
            }
            AdSeminar ad = null;
            if (cmd.adSeminarId() != null) {
                Optional<AdSeminar> adOptional = adSeminarRepository.findById(cmd.adSeminarId());
                if (adOptional.isPresent()) {
                    ad = adOptional.get();
                }
            }
            LocalDate period = LocalDate.parse(cmd.targetPeriod());
            saved.add(InventoryGoal.create(inv, GoalCategory.valueOf(cmd.goalCategory()),
                    skill, qual, ad, cmd.customName(), period, cmd.reason()));
        }
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

        long itOrQual = 0;
        long ad = 0;
        for (InventoryGoal g : goals) {
            if (g.getGoalCategory() == GoalCategory.IT_SKILL || g.getGoalCategory() == GoalCategory.QUALIFICATION) {
                itOrQual++;
            }
            if (g.getGoalCategory() == GoalCategory.AD) {
                ad++;
            }
        }

        if (itOrQual < 1 || ad < 2) {
            List<GoalIncompleteException.GoalValidationError> errors = new ArrayList<>();
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
                throw new AuthException("FORBIDDEN", "");
            }
        }
    }

    /**
     * 指定棚卸の前年度棚卸を検索する。
     * 対象ユーザーの全棚卸から、指定棚卸自身を除き、年度終了日が指定棚卸の年度開始日より前のものを検索する。
     */
    private Inventory findPrevInventory(Inventory inv, int inventoryId) {
        List<Inventory> allInventories = inventoryRepository.findByUserIdWithFiscalYear(inv.getUser().getId());
        for (Inventory i : allInventories) {
            if (i.getId().equals(inventoryId)) {
                continue;
            }
            if (i.getFiscalYear().getEndDate().isBefore(inv.getFiscalYear().getStartDate())) {
                return i;
            }
        }
        return null;
    }

    /** 目標名称を参照先の優先順位（ITスキル → 資格 → ADセミナー → カスタム名）で解決して返す。 */
    private String resolveGoalName(InventoryGoal g) {
        if (g.getItSkill() != null) return g.getItSkill().getName();
        if (g.getQualification() != null) return g.getQualification().getName();
        if (g.getAdSeminar() != null) return g.getAdSeminar().getName();
        return g.getCustomName();
    }
}
