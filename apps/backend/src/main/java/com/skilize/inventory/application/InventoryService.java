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

    @Transactional(readOnly = true)
    public List<Inventory> findMine(int userId) {
        return inventoryRepository.findByUserIdWithFiscalYear(userId);
    }

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

    @Transactional(readOnly = true)
    public Inventory findById(int id, User user) {
        Inventory inv = inventoryRepository.findByIdWithAssociations(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "棚卸が見つかりません"));
        checkOwnership(inv, user);
        return inv;
    }

    // --- IT Skill details ---

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

    @Transactional(readOnly = true)
    public List<QualificationDetail> findQualificationDetails(int inventoryId, User user) {
        findById(inventoryId, user);
        return qualificationDetailRepository.findByInventoryId(inventoryId);
    }

    // --- Seminar details ---

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

    @Transactional(readOnly = true)
    public List<SeminarDetail> findSeminarDetails(int inventoryId, User user) {
        findById(inventoryId, user);
        return seminarDetailRepository.findByInventoryId(inventoryId);
    }

    // --- Submit ---

    @Transactional
    public Inventory submit(int inventoryId, User user) {
        Inventory inv = findById(inventoryId, user);
        inv.submit();
        return inventoryRepository.save(inv);
    }

    // --- Comparison ---

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

    private void checkOwnership(Inventory inv, User user) {
        if (!inv.getUser().getId().equals(user.getId())) {
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
