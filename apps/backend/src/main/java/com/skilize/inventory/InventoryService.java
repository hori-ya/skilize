package com.skilize.inventory;

import com.skilize.common.exception.AuthException;
import com.skilize.common.exception.GoalIncompleteException;
import com.skilize.domain.fiscalyear.FiscalYear;
import com.skilize.domain.fiscalyear.FiscalYearRepository;
import com.skilize.domain.inventory.*;
import com.skilize.domain.master.*;
import com.skilize.domain.user.User;
import lombok.RequiredArgsConstructor;
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

    // --- Inventory header ---

    @Transactional(readOnly = true)
    public List<Inventory> findMine(int userId) {
        return inventoryRepository.findByUserIdWithFiscalYear(userId);
    }

    @Transactional
    public Inventory create(User user, int fiscalYearId) {
        FiscalYear fy = fiscalYearRepository.findById(fiscalYearId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "年度が見つかりません"));
        inventoryRepository.findByUserIdAndFiscalYearId(user.getId(), fiscalYearId).ifPresent(i -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当該年度の棚卸はすでに作成されています");
        });
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
                                                   List<InventoryController.ItSkillDetailItem> items) {
        Inventory inv = findById(inventoryId, user);
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
                                                               List<InventoryController.QualificationDetailItem> items) {
        Inventory inv = findById(inventoryId, user);
        qualificationDetailRepository.deleteByInventoryId(inventoryId);
        List<QualificationDetail> saved = items.stream().map(item -> {
            Qualification q = item.qualificationId() != null
                    ? qualificationRepository.findById(item.qualificationId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "資格が見つかりません"))
                    : null;
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
                                                   List<InventoryController.SeminarDetailItem> items) {
        Inventory inv = findById(inventoryId, user);
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

    @Transactional
    public Inventory submit(int inventoryId, User user) {
        Inventory inv = findById(inventoryId, user);
        inv.submit();
        return inventoryRepository.save(inv);
    }

    // --- Comparison ---

    @Transactional(readOnly = true)
    public InventoryController.ComparisonResponse getComparison(int inventoryId, User user) {
        Inventory inv = findById(inventoryId, user);
        String currentFy = inv.getFiscalYear().getName();

        List<ItSkillDetail> currentDetails = itSkillDetailRepository.findByInventoryId(inventoryId);

        // Find previous year inventory
        List<Inventory> allInventories = inventoryRepository.findByUserIdWithFiscalYear(user.getId());
        Inventory prevInv = allInventories.stream()
                .filter(i -> !i.getId().equals(inventoryId))
                .filter(i -> i.getFiscalYear().getEndDate().isBefore(inv.getFiscalYear().getStartDate()))
                .findFirst().orElse(null);

        if (prevInv == null) {
            return new InventoryController.ComparisonResponse(inventoryId, currentFy, null, false, List.of());
        }

        List<ItSkillDetail> prevDetails = itSkillDetailRepository.findByInventoryId(prevInv.getId());
        Map<Integer, ItSkillDetail> prevBySkillId = prevDetails.stream()
                .filter(d -> d.getItSkill() != null)
                .collect(Collectors.toMap(d -> d.getItSkill().getId(), d -> d));

        List<InventoryController.ComparisonItem> items = currentDetails.stream()
                .filter(d -> d.getItSkill() != null)
                .map(d -> {
                    ItSkillDetail prev = prevBySkillId.get(d.getItSkill().getId());
                    Short currentLv = d.getSkillLevel().getLevelValue();
                    Short prevLv = prev != null ? prev.getSkillLevel().getLevelValue() : null;
                    Integer diff = (prevLv != null) ? (int) currentLv - (int) prevLv : null;
                    return new InventoryController.ComparisonItem(
                            d.getItSkill().getId(), d.getItSkill().getName(),
                            d.getId(), (int) currentLv, d.getRemarks(),
                            prevLv != null ? (int) prevLv : null, diff);
                }).toList();

        return new InventoryController.ComparisonResponse(inventoryId, currentFy,
                prevInv.getFiscalYear().getName(), true, items);
    }

    // --- Goal review ---

    @Transactional(readOnly = true)
    public InventoryController.GoalReviewResponse getGoalReview(int inventoryId, User user) {
        Inventory inv = findById(inventoryId, user);

        List<Inventory> allInventories = inventoryRepository.findByUserIdWithFiscalYear(user.getId());
        Inventory prevInv = allInventories.stream()
                .filter(i -> !i.getId().equals(inventoryId))
                .filter(i -> i.getFiscalYear().getEndDate().isBefore(inv.getFiscalYear().getStartDate()))
                .findFirst().orElse(null);

        if (prevInv == null) {
            return new InventoryController.GoalReviewResponse(null, false, List.of());
        }

        List<InventoryGoal> prevGoals = inventoryGoalRepository.findByInventoryId(prevInv.getId());
        List<InventoryController.GoalReviewItem> items = prevGoals.stream()
                .map(g -> {
                    String name = resolveGoalName(g);
                    return new InventoryController.GoalReviewItem(
                            g.getId(), g.getGoalCategory().name(), name,
                            g.getTargetPeriod().toString(), g.getReason(),
                            g.getAchievementStatus() != null ? g.getAchievementStatus().name() : null,
                            g.getReviewNote());
                }).toList();

        return new InventoryController.GoalReviewResponse(prevInv.getFiscalYear().getName(), !items.isEmpty(), items);
    }

    @Transactional
    public InventoryController.GoalReviewResponse saveGoalReview(int inventoryId, User user,
                                                                   List<InventoryController.GoalReviewUpdateItem> items) {
        findById(inventoryId, user);
        items.forEach(item -> {
            InventoryGoal goal = inventoryGoalRepository.findById(item.prevGoalId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "目標が見つかりません"));
            AchievementStatus status = item.achievementStatus() != null
                    ? AchievementStatus.valueOf(item.achievementStatus()) : null;
            goal.updateReview(status, item.reviewNote());
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
    public List<InventoryGoal> saveGoals(int inventoryId, User user,
                                         List<InventoryController.GoalItem> items) {
        Inventory inv = findById(inventoryId, user);
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
        return inventoryRepository.save(inv);
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
