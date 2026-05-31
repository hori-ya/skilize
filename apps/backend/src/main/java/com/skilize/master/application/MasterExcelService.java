package com.skilize.master.application;

import com.skilize.master.application.query.MasterImportErrorDetail;
import com.skilize.master.application.query.MasterImportQueryResult;
import com.skilize.master.domain.*;
import com.skilize.master.infrastructure.excel.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * マスタ Excel 出力・取込サービス。
 * 取込は all-or-nothing（バリデーションエラーがあれば全件ロールバック）。
 * Excel 非存在の DB レコードは論理削除（is_active = false）する。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MasterExcelService {

    private static final int MAX_ERRORS = 100;

    private final ItSkillCategoryRepository itSkillCategoryRepository;
    private final ItSkillRepository itSkillRepository;
    private final QualificationCategoryRepository qualificationCategoryRepository;
    private final QualificationRepository qualificationRepository;
    private final AdSeminarCategoryRepository adSeminarCategoryRepository;
    private final AdSeminarRepository adSeminarRepository;

    private final ItSkillExcelExporter itSkillExporter;
    private final ItSkillExcelImporter itSkillImporter;
    private final QualificationExcelExporter qualificationExporter;
    private final QualificationExcelImporter qualificationImporter;
    private final AdSeminarExcelExporter adSeminarExporter;
    private final AdSeminarExcelImporter adSeminarImporter;

    // ─── 出力 ───────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] exportItSkillExcel() {
        List<ItSkillCategory> categories = itSkillCategoryRepository.findAllByOrderByLevelAscParentIdAscSortOrderAsc();
        List<ItSkill> skills = itSkillRepository.findAllOrderByHierarchy();
        return itSkillExporter.export(categories, skills);
    }

    @Transactional(readOnly = true)
    public byte[] exportQualificationExcel() {
        List<QualificationCategory> categories = qualificationCategoryRepository.findAllByOrderBySortOrderAsc();
        List<Qualification> qualifications = qualificationRepository.findAllWithCategory();
        return qualificationExporter.export(categories, qualifications);
    }

    @Transactional(readOnly = true)
    public byte[] exportAdSeminarExcel() {
        List<AdSeminarCategory> categories = adSeminarCategoryRepository.findAllByOrderBySortOrderAsc();
        List<AdSeminar> seminars = adSeminarRepository.findAllWithCategory();
        return adSeminarExporter.export(categories, seminars);
    }

    // ─── 取込: ITスキル ──────────────────────────────────────────────────────────

    @Transactional
    public MasterImportQueryResult importItSkillExcel(MultipartFile file) {
        ItSkillExcelImporter.ItSkillImportData data = itSkillImporter.parse(file);
        List<MasterImportErrorDetail> errors = new ArrayList<>();

        // 既存 DB データを取得（カテゴリは後続のスキル保存でも参照するためミュータブルなマップ）
        Map<Integer, ItSkillCategory> dbCategories = new HashMap<>(itSkillCategoryRepository.findAll()
                .stream().collect(Collectors.toMap(ItSkillCategory::getId, c -> c)));
        Map<Integer, ItSkill> dbSkills = itSkillRepository.findAll()
                .stream().collect(Collectors.toMap(ItSkill::getId, s -> s));

        // バリデーション（全行のエラーを収集してから判定）
        validateItSkillCategories(data.categoryRows(), dbCategories, errors);
        validateItSkills(data.skillRows(), dbCategories, dbSkills, errors);

        if (!errors.isEmpty()) {
            return MasterImportQueryResult.ofErrors(errors);
        }

        // カテゴリ保存（⑩: 新規保存後は dbCategories に追加してスキル保存で再利用）
        Set<Integer> excelCategoryIds = new HashSet<>();
        int catCreated = 0, catUpdated = 0;

        for (ItSkillExcelImporter.CategoryRow row : data.categoryRows()) {
            if (row.id() != null) {
                ItSkillCategory cat = dbCategories.get(row.id());
                boolean active = row.active() != null ? row.active() : cat.isActive();
                cat.update(row.name(), row.siblingOrder(), active);
                itSkillCategoryRepository.save(cat);
                excelCategoryIds.add(row.id());
                catUpdated++;
            } else {
                Integer parentId = row.parentId();
                short level = parentId == null ? (short) 1
                        : (short) (dbCategories.get(parentId).getLevel() + 1);
                ItSkillCategory newCat = ItSkillCategory.create(parentId, level, row.name(), row.siblingOrder());
                if (Boolean.FALSE.equals(row.active())) {
                    newCat.update(newCat.getName(), newCat.getSortOrder(), false);
                }
                // 保存後の ID を dbCategories に追加（二重 findAll 不要）
                ItSkillCategory saved = itSkillCategoryRepository.save(newCat);
                dbCategories.put(saved.getId(), saved);
                catCreated++;
            }
        }

        // DB に存在するが Excel にない → 論理削除
        int catDeleted = 0;
        for (ItSkillCategory cat : dbCategories.values()) {
            if (!excelCategoryIds.contains(cat.getId()) && cat.isActive()) {
                cat.update(cat.getName(), cat.getSortOrder(), false);
                itSkillCategoryRepository.save(cat);
                catDeleted++;
            }
        }

        // スキル保存（dbCategories を再利用）
        Set<Integer> excelSkillIds = new HashSet<>();
        int skillCreated = 0, skillUpdated = 0;

        for (ItSkillExcelImporter.SkillRow row : data.skillRows()) {
            ItSkillCategory category = dbCategories.get(row.categoryId());
            if (row.id() != null) {
                ItSkill skill = dbSkills.get(row.id());
                boolean active = row.active() != null ? row.active() : skill.isActive();
                skill.update(category, row.name(), row.description(), row.siblingOrder(), active);
                itSkillRepository.save(skill);
                excelSkillIds.add(row.id());
                skillUpdated++;
            } else {
                ItSkill newSkill = ItSkill.create(category, row.name(), row.description(), row.siblingOrder());
                itSkillRepository.save(newSkill);
                skillCreated++;
            }
        }

        // DB に存在するが Excel にない → 論理削除
        int skillDeleted = 0;
        for (ItSkill skill : dbSkills.values()) {
            if (!excelSkillIds.contains(skill.getId()) && skill.isActive()) {
                skill.update(skill.getCategory(), skill.getName(), skill.getDescription(),
                        skill.getSortOrder(), false);
                itSkillRepository.save(skill);
                skillDeleted++;
            }
        }

        log.info("ITスキルExcel取込完了: カテゴリ[作成={} 更新={} 削除={}] スキル[作成={} 更新={} 削除={}]",
                catCreated, catUpdated, catDeleted, skillCreated, skillUpdated, skillDeleted);
        return MasterImportQueryResult.ofSuccess(
                catCreated + skillCreated, catUpdated + skillUpdated, catDeleted + skillDeleted);
    }

    // ─── 取込: 参考資格 ──────────────────────────────────────────────────────────

    @Transactional
    public MasterImportQueryResult importQualificationExcel(MultipartFile file) {
        QualificationExcelImporter.QualificationImportData data = qualificationImporter.parse(file);
        List<MasterImportErrorDetail> errors = new ArrayList<>();

        Map<Integer, QualificationCategory> dbCategories = new HashMap<>(qualificationCategoryRepository.findAll()
                .stream().collect(Collectors.toMap(QualificationCategory::getId, c -> c)));
        Map<Integer, Qualification> dbQuals = qualificationRepository.findAll()
                .stream().collect(Collectors.toMap(Qualification::getId, q -> q));

        validateQualificationCategories(data.categoryRows(), dbCategories, errors);
        validateQualifications(data.qualRows(), dbCategories, dbQuals, errors);

        if (!errors.isEmpty()) {
            return MasterImportQueryResult.ofErrors(errors);
        }

        Set<Integer> excelCatIds = new HashSet<>();
        int catCreated = 0, catUpdated = 0;
        for (QualificationExcelImporter.CategoryRow row : data.categoryRows()) {
            if (row.id() != null) {
                QualificationCategory cat = dbCategories.get(row.id());
                boolean active = row.active() != null ? row.active() : cat.isActive();
                cat.update(row.name(), row.siblingOrder(), active);
                qualificationCategoryRepository.save(cat);
                excelCatIds.add(row.id());
                catUpdated++;
            } else {
                QualificationCategory newCat = QualificationCategory.create(row.name(), row.siblingOrder());
                if (Boolean.FALSE.equals(row.active())) newCat.update(newCat.getName(), newCat.getSortOrder(), false);
                QualificationCategory saved = qualificationCategoryRepository.save(newCat);
                dbCategories.put(saved.getId(), saved);
                catCreated++;
            }
        }

        int catDeleted = 0;
        for (QualificationCategory cat : dbCategories.values()) {
            if (!excelCatIds.contains(cat.getId()) && cat.isActive()) {
                cat.update(cat.getName(), cat.getSortOrder(), false);
                qualificationCategoryRepository.save(cat);
                catDeleted++;
            }
        }

        Set<Integer> excelQualIds = new HashSet<>();
        int qualCreated = 0, qualUpdated = 0;
        for (QualificationExcelImporter.QualificationRow row : data.qualRows()) {
            QualificationCategory cat = row.categoryId() != null ? dbCategories.get(row.categoryId()) : null;
            if (row.id() != null) {
                Qualification q = dbQuals.get(row.id());
                boolean active = row.active() != null ? row.active() : q.isActive();
                q.update(cat, row.name(), row.description(), row.siblingOrder(), active);
                qualificationRepository.save(q);
                excelQualIds.add(row.id());
                qualUpdated++;
            } else {
                Qualification newQ = Qualification.create(cat, row.name(), row.description(), row.siblingOrder());
                if (Boolean.FALSE.equals(row.active())) newQ.update(newQ.getCategory(), newQ.getName(), newQ.getDescription(), newQ.getSortOrder(), false);
                qualificationRepository.save(newQ);
                qualCreated++;
            }
        }

        int qualDeleted = 0;
        for (Qualification q : dbQuals.values()) {
            if (!excelQualIds.contains(q.getId()) && q.isActive()) {
                q.update(q.getCategory(), q.getName(), q.getDescription(), q.getSortOrder(), false);
                qualificationRepository.save(q);
                qualDeleted++;
            }
        }

        log.info("参考資格Excel取込完了: カテゴリ[作成={} 更新={} 削除={}] 資格[作成={} 更新={} 削除={}]",
                catCreated, catUpdated, catDeleted, qualCreated, qualUpdated, qualDeleted);
        return MasterImportQueryResult.ofSuccess(
                catCreated + qualCreated, catUpdated + qualUpdated, catDeleted + qualDeleted);
    }

    // ─── 取込: ADマスタ ──────────────────────────────────────────────────────────

    @Transactional
    public MasterImportQueryResult importAdSeminarExcel(MultipartFile file) {
        AdSeminarExcelImporter.AdSeminarImportData data = adSeminarImporter.parse(file);
        List<MasterImportErrorDetail> errors = new ArrayList<>();

        Map<Integer, AdSeminarCategory> dbCategories = new HashMap<>(adSeminarCategoryRepository.findAll()
                .stream().collect(Collectors.toMap(AdSeminarCategory::getId, c -> c)));
        Map<Integer, AdSeminar> dbSeminars = adSeminarRepository.findAll()
                .stream().collect(Collectors.toMap(AdSeminar::getId, s -> s));

        validateAdSeminarCategories(data.categoryRows(), dbCategories, errors);
        validateAdSeminars(data.seminarRows(), dbCategories, dbSeminars, errors);

        if (!errors.isEmpty()) {
            return MasterImportQueryResult.ofErrors(errors);
        }

        Set<Integer> excelCatIds = new HashSet<>();
        int catCreated = 0, catUpdated = 0;
        for (AdSeminarExcelImporter.CategoryRow row : data.categoryRows()) {
            if (row.id() != null) {
                AdSeminarCategory cat = dbCategories.get(row.id());
                boolean active = row.active() != null ? row.active() : cat.isActive();
                cat.update(row.name(), row.siblingOrder(), active);
                adSeminarCategoryRepository.save(cat);
                excelCatIds.add(row.id());
                catUpdated++;
            } else {
                AdSeminarCategory newCat = AdSeminarCategory.create(row.name(), row.siblingOrder());
                if (Boolean.FALSE.equals(row.active())) newCat.update(newCat.getName(), newCat.getSortOrder(), false);
                AdSeminarCategory saved = adSeminarCategoryRepository.save(newCat);
                dbCategories.put(saved.getId(), saved);
                catCreated++;
            }
        }

        int catDeleted = 0;
        for (AdSeminarCategory cat : dbCategories.values()) {
            if (!excelCatIds.contains(cat.getId()) && cat.isActive()) {
                cat.update(cat.getName(), cat.getSortOrder(), false);
                adSeminarCategoryRepository.save(cat);
                catDeleted++;
            }
        }

        Set<Integer> excelSeminarIds = new HashSet<>();
        int semCreated = 0, semUpdated = 0;
        for (AdSeminarExcelImporter.SeminarRow row : data.seminarRows()) {
            AdSeminarCategory cat = row.categoryId() != null ? dbCategories.get(row.categoryId()) : null;
            if (row.id() != null) {
                AdSeminar s = dbSeminars.get(row.id());
                boolean active = row.active() != null ? row.active() : s.isActive();
                s.update(cat, row.name(), row.description(), row.siblingOrder(), active);
                adSeminarRepository.save(s);
                excelSeminarIds.add(row.id());
                semUpdated++;
            } else {
                AdSeminar newS = AdSeminar.create(cat, row.name(), row.description(), row.siblingOrder());
                if (Boolean.FALSE.equals(row.active())) newS.update(newS.getCategory(), newS.getName(), newS.getDescription(), newS.getSortOrder(), false);
                adSeminarRepository.save(newS);
                semCreated++;
            }
        }

        int semDeleted = 0;
        for (AdSeminar s : dbSeminars.values()) {
            if (!excelSeminarIds.contains(s.getId()) && s.isActive()) {
                s.update(s.getCategory(), s.getName(), s.getDescription(), s.getSortOrder(), false);
                adSeminarRepository.save(s);
                semDeleted++;
            }
        }

        log.info("ADセミナーExcel取込完了: カテゴリ[作成={} 更新={} 削除={}] AD[作成={} 更新={} 削除={}]",
                catCreated, catUpdated, catDeleted, semCreated, semUpdated, semDeleted);
        return MasterImportQueryResult.ofSuccess(
                catCreated + semCreated, catUpdated + semUpdated, catDeleted + semDeleted);
    }

    // ─── バリデーション ──────────────────────────────────────────────────────────
    // ⑨: 各行のエラーをまとめてから追加することで、MAX_ERRORS 到達時に行の途中で切れないようにする

    private void validateItSkillCategories(List<ItSkillExcelImporter.CategoryRow> rows,
                                            Map<Integer, ItSkillCategory> dbMap,
                                            List<MasterImportErrorDetail> errors) {
        for (ItSkillExcelImporter.CategoryRow row : rows) {
            if (errors.size() >= MAX_ERRORS) break;
            List<MasterImportErrorDetail> rowErrors = new ArrayList<>();
            if (row.name() == null || row.name().isBlank()) {
                rowErrors.add(new MasterImportErrorDetail("IT分類", row.rowNum(), "D", "カテゴリ名は必須です"));
            }
            if (row.id() != null && !dbMap.containsKey(row.id())) {
                rowErrors.add(new MasterImportErrorDetail("IT分類", row.rowNum(), "A",
                        "ID=" + row.id() + " のカテゴリが存在しません"));
            }
            if (row.parentId() != null && !dbMap.containsKey(row.parentId())) {
                rowErrors.add(new MasterImportErrorDetail("IT分類", row.rowNum(), "B",
                        "親カテゴリID=" + row.parentId() + " が存在しません"));
            }
            if (row.parentId() != null && dbMap.containsKey(row.parentId())
                    && dbMap.get(row.parentId()).getLevel() >= 3) {
                rowErrors.add(new MasterImportErrorDetail("IT分類", row.rowNum(), "B",
                        "L3 カテゴリの配下には子カテゴリを追加できません"));
            }
            appendRowErrors(errors, rowErrors);
        }
        if (errors.size() >= MAX_ERRORS) addMaxErrorsMessage(errors);
    }

    private void validateItSkills(List<ItSkillExcelImporter.SkillRow> rows,
                                   Map<Integer, ItSkillCategory> dbCategoryMap,
                                   Map<Integer, ItSkill> dbSkillMap,
                                   List<MasterImportErrorDetail> errors) {
        for (ItSkillExcelImporter.SkillRow row : rows) {
            if (errors.size() >= MAX_ERRORS) break;
            List<MasterImportErrorDetail> rowErrors = new ArrayList<>();
            if (row.name() == null || row.name().isBlank()) {
                rowErrors.add(new MasterImportErrorDetail("ITスキル", row.rowNum(), "F", "スキル名は必須です"));
            }
            if (row.categoryId() == null) {
                rowErrors.add(new MasterImportErrorDetail("ITスキル", row.rowNum(), "A", "カテゴリIDは必須です"));
            } else if (!dbCategoryMap.containsKey(row.categoryId())) {
                rowErrors.add(new MasterImportErrorDetail("ITスキル", row.rowNum(), "A",
                        "カテゴリID=" + row.categoryId() + " が存在しません"));
            }
            if (row.id() != null && !dbSkillMap.containsKey(row.id())) {
                rowErrors.add(new MasterImportErrorDetail("ITスキル", row.rowNum(), "E",
                        "ID=" + row.id() + " のスキルが存在しません"));
            }
            appendRowErrors(errors, rowErrors);
        }
        if (errors.size() >= MAX_ERRORS) addMaxErrorsMessage(errors);
    }

    private void validateQualificationCategories(List<QualificationExcelImporter.CategoryRow> rows,
                                                  Map<Integer, QualificationCategory> dbMap,
                                                  List<MasterImportErrorDetail> errors) {
        for (QualificationExcelImporter.CategoryRow row : rows) {
            if (errors.size() >= MAX_ERRORS) break;
            List<MasterImportErrorDetail> rowErrors = new ArrayList<>();
            if (row.name() == null || row.name().isBlank()) {
                rowErrors.add(new MasterImportErrorDetail("資格カテゴリ", row.rowNum(), "B", "カテゴリ名は必須です"));
            }
            if (row.id() != null && !dbMap.containsKey(row.id())) {
                rowErrors.add(new MasterImportErrorDetail("資格カテゴリ", row.rowNum(), "A",
                        "ID=" + row.id() + " のカテゴリが存在しません"));
            }
            appendRowErrors(errors, rowErrors);
        }
        if (errors.size() >= MAX_ERRORS) addMaxErrorsMessage(errors);
    }

    private void validateQualifications(List<QualificationExcelImporter.QualificationRow> rows,
                                         Map<Integer, QualificationCategory> dbCategoryMap,
                                         Map<Integer, Qualification> dbQualMap,
                                         List<MasterImportErrorDetail> errors) {
        for (QualificationExcelImporter.QualificationRow row : rows) {
            if (errors.size() >= MAX_ERRORS) break;
            List<MasterImportErrorDetail> rowErrors = new ArrayList<>();
            if (row.name() == null || row.name().isBlank()) {
                rowErrors.add(new MasterImportErrorDetail("参考資格", row.rowNum(), "D", "資格名は必須です"));
            }
            if (row.categoryId() != null && !dbCategoryMap.containsKey(row.categoryId())) {
                rowErrors.add(new MasterImportErrorDetail("参考資格", row.rowNum(), "A",
                        "カテゴリID=" + row.categoryId() + " が存在しません"));
            }
            if (row.id() != null && !dbQualMap.containsKey(row.id())) {
                rowErrors.add(new MasterImportErrorDetail("参考資格", row.rowNum(), "B",
                        "ID=" + row.id() + " の資格が存在しません"));
            }
            appendRowErrors(errors, rowErrors);
        }
        if (errors.size() >= MAX_ERRORS) addMaxErrorsMessage(errors);
    }

    private void validateAdSeminarCategories(List<AdSeminarExcelImporter.CategoryRow> rows,
                                              Map<Integer, AdSeminarCategory> dbMap,
                                              List<MasterImportErrorDetail> errors) {
        for (AdSeminarExcelImporter.CategoryRow row : rows) {
            if (errors.size() >= MAX_ERRORS) break;
            List<MasterImportErrorDetail> rowErrors = new ArrayList<>();
            if (row.name() == null || row.name().isBlank()) {
                rowErrors.add(new MasterImportErrorDetail("ADカテゴリ", row.rowNum(), "B", "カテゴリ名は必須です"));
            }
            if (row.id() != null && !dbMap.containsKey(row.id())) {
                rowErrors.add(new MasterImportErrorDetail("ADカテゴリ", row.rowNum(), "A",
                        "ID=" + row.id() + " のカテゴリが存在しません"));
            }
            appendRowErrors(errors, rowErrors);
        }
        if (errors.size() >= MAX_ERRORS) addMaxErrorsMessage(errors);
    }

    private void validateAdSeminars(List<AdSeminarExcelImporter.SeminarRow> rows,
                                     Map<Integer, AdSeminarCategory> dbCategoryMap,
                                     Map<Integer, AdSeminar> dbSeminarMap,
                                     List<MasterImportErrorDetail> errors) {
        for (AdSeminarExcelImporter.SeminarRow row : rows) {
            if (errors.size() >= MAX_ERRORS) break;
            List<MasterImportErrorDetail> rowErrors = new ArrayList<>();
            if (row.name() == null || row.name().isBlank()) {
                rowErrors.add(new MasterImportErrorDetail("ADセミナー", row.rowNum(), "D", "AD名は必須です"));
            }
            if (row.categoryId() != null && !dbCategoryMap.containsKey(row.categoryId())) {
                rowErrors.add(new MasterImportErrorDetail("ADセミナー", row.rowNum(), "A",
                        "カテゴリID=" + row.categoryId() + " が存在しません"));
            }
            if (row.id() != null && !dbSeminarMap.containsKey(row.id())) {
                rowErrors.add(new MasterImportErrorDetail("ADセミナー", row.rowNum(), "C",
                        "ID=" + row.id() + " のADセミナーが存在しません"));
            }
            appendRowErrors(errors, rowErrors);
        }
        if (errors.size() >= MAX_ERRORS) addMaxErrorsMessage(errors);
    }

    /** 行単位のエラーを全量追加する（MAX_ERRORS を超えた分は切り捨て）。 */
    private void appendRowErrors(List<MasterImportErrorDetail> errors, List<MasterImportErrorDetail> rowErrors) {
        int remaining = MAX_ERRORS - errors.size();
        if (remaining <= 0) return;
        errors.addAll(rowErrors.subList(0, Math.min(rowErrors.size(), remaining)));
    }

    private void addMaxErrorsMessage(List<MasterImportErrorDetail> errors) {
        // 重複追加を防ぐ
        boolean alreadyAdded = errors.stream().anyMatch(e -> e.column().isEmpty());
        if (!alreadyAdded) {
            errors.add(new MasterImportErrorDetail("", 0, "",
                    "100 件を超えるエラーがあります。ファイルを修正してから再度取込してください。"));
        }
    }
}
