package com.skilize.master.application;

import com.skilize.master.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * マスタデータ（スキルレベル・ITスキル・ITスキル分類・資格・資格分類・ADセミナー・ADセミナー分類）の
 * 作成・更新ビジネスロジック。TL/ADMIN 操作が中心。
 * 各マスタは is_active フラグで論理削除を管理し、物理削除は行わない（過去棚卸の参照を保持するため）。
 */
@Service
@RequiredArgsConstructor
public class MasterService {

    private final SkillLevelRepository skillLevelRepository;
    private final ItSkillRepository itSkillRepository;
    private final ItSkillCategoryRepository itSkillCategoryRepository;
    private final QualificationRepository qualificationRepository;
    private final QualificationCategoryRepository qualificationCategoryRepository;
    private final AdSeminarRepository adSeminarRepository;
    private final AdSeminarCategoryRepository adSeminarCategoryRepository;

    /** スキルレベルを新規作成する。 */
    @Transactional
    public SkillLevel createSkillLevel(Short levelValue, String description) {
        return skillLevelRepository.save(SkillLevel.create(levelValue, description));
    }

    /**
     * スキルレベルを更新する。
     * active が null の場合は現在の値を維持する（部分更新パターン）。
     */
    @Transactional
    public SkillLevel updateSkillLevel(int id, Short levelValue, String description, Boolean active) {
        SkillLevel level = skillLevelRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // active が null = フロントエンドから未送信 → 既存の値をそのまま使う
        level.update(levelValue, description, active != null ? active : level.isActive());
        return skillLevelRepository.save(level);
    }

    /** ITスキルを新規作成する。sortOrder が未指定の場合は 0 として登録する。 */
    @Transactional
    public ItSkill createItSkill(int categoryId, String name, String description, Integer sortOrder) {
        ItSkillCategory category = itSkillCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "分類が見つかりません"));
        return itSkillRepository.save(ItSkill.create(category, name, description, sortOrder != null ? sortOrder : 0));
    }

    /**
     * ITスキルを更新する。
     * sortOrder・active が null の場合は現在の値を維持する（部分更新パターン）。
     */
    @Transactional
    public ItSkill updateItSkill(int id, int categoryId, String name, String description,
                                  Integer sortOrder, Boolean active) {
        ItSkill skill = itSkillRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ItSkillCategory category = itSkillCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "分類が見つかりません"));
        skill.update(category, name, description,
                sortOrder != null ? sortOrder : skill.getSortOrder(),
                active != null ? active : skill.isActive());
        return itSkillRepository.save(skill);
    }

    /**
     * ITスキル分類を新規作成する。
     * 親分類が null の場合はレベル1（大分類）として作成する。
     * 親分類が指定された場合は「親のレベル + 1」で計算し、最大3階層を超える場合はエラーとする。
     */
    @Transactional
    public ItSkillCategory createItSkillCategory(Integer parentId, String name, Integer sortOrder) {
        short level;
        if (parentId == null) {
            // 親なし = ルートカテゴリ（レベル1 = 大分類）
            level = 1;
        } else {
            ItSkillCategory parent = itSkillCategoryRepository.findById(parentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "親分類が見つかりません"));
            // 子のレベルは親のレベル + 1。最大3階層まで許可する。
            level = (short) (parent.getLevel() + 1);
            if (level > 3) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "第3階層以上の分類は作成できません");
            }
        }
        return itSkillCategoryRepository.save(
                ItSkillCategory.create(parentId, level, name, sortOrder != null ? sortOrder : 0));
    }

    /** ITスキル分類を更新する。sortOrder・active が null の場合は現在の値を維持する。 */
    @Transactional
    public ItSkillCategory updateItSkillCategory(int id, String name, Integer sortOrder, Boolean active) {
        ItSkillCategory cat = itSkillCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        cat.update(name, sortOrder != null ? sortOrder : cat.getSortOrder(), active != null ? active : cat.isActive());
        return itSkillCategoryRepository.save(cat);
    }

    /**
     * 資格を新規作成する。分類（categoryId）は任意項目のため null も許容する。
     * ITスキルと異なり分類は1階層のみ（ItSkillCategory のような階層構造はない）。
     */
    @Transactional
    public Qualification createQualification(Integer categoryId, String name, String description, Integer sortOrder) {
        // categoryId が null の場合は分類なしで登録する
        QualificationCategory cat = categoryId != null
                ? qualificationCategoryRepository.findById(categoryId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "カテゴリが見つかりません"))
                : null;
        return qualificationRepository.save(Qualification.create(cat, name, description, sortOrder != null ? sortOrder : 0));
    }

    /** 資格を更新する。sortOrder・active が null の場合は現在の値を維持する。 */
    @Transactional
    public Qualification updateQualification(int id, Integer categoryId, String name,
                                              String description, Integer sortOrder, Boolean active) {
        Qualification q = qualificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        QualificationCategory cat = categoryId != null
                ? qualificationCategoryRepository.findById(categoryId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "カテゴリが見つかりません"))
                : null;
        q.update(cat, name, description,
                sortOrder != null ? sortOrder : q.getSortOrder(),
                active != null ? active : q.isActive());
        return qualificationRepository.save(q);
    }

    /** 資格分類を新規作成する。 */
    @Transactional
    public QualificationCategory createQualificationCategory(String name, Integer sortOrder) {
        return qualificationCategoryRepository.save(
                QualificationCategory.create(name, sortOrder != null ? sortOrder : 0));
    }

    /** 資格分類を更新する。sortOrder・active が null の場合は現在の値を維持する。 */
    @Transactional
    public QualificationCategory updateQualificationCategory(int id, String name, Integer sortOrder, Boolean active) {
        QualificationCategory c = qualificationCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        c.update(name, sortOrder != null ? sortOrder : c.getSortOrder(), active != null ? active : c.isActive());
        return qualificationCategoryRepository.save(c);
    }

    /**
     * ADセミナーを新規作成する。分類（categoryId）は任意項目のため null も許容する。
     * ADセミナーは棚卸のセミナー明細と目標設定の両方から参照される。
     */
    @Transactional
    public AdSeminar createAdSeminar(Integer categoryId, String name, String description, Integer sortOrder) {
        AdSeminarCategory cat = categoryId != null
                ? adSeminarCategoryRepository.findById(categoryId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "カテゴリが見つかりません"))
                : null;
        return adSeminarRepository.save(AdSeminar.create(cat, name, description, sortOrder != null ? sortOrder : 0));
    }

    /** ADセミナーを更新する。sortOrder・active が null の場合は現在の値を維持する。 */
    @Transactional
    public AdSeminar updateAdSeminar(int id, Integer categoryId, String name,
                                      String description, Integer sortOrder, Boolean active) {
        AdSeminar a = adSeminarRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        AdSeminarCategory cat = categoryId != null
                ? adSeminarCategoryRepository.findById(categoryId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "カテゴリが見つかりません"))
                : null;
        a.update(cat, name, description,
                sortOrder != null ? sortOrder : a.getSortOrder(),
                active != null ? active : a.isActive());
        return adSeminarRepository.save(a);
    }

    /** ADセミナー分類を新規作成する。 */
    @Transactional
    public AdSeminarCategory createAdSeminarCategory(String name, Integer sortOrder) {
        return adSeminarCategoryRepository.save(
                AdSeminarCategory.create(name, sortOrder != null ? sortOrder : 0));
    }

    /** ADセミナー分類を更新する。sortOrder・active が null の場合は現在の値を維持する。 */
    @Transactional
    public AdSeminarCategory updateAdSeminarCategory(int id, String name, Integer sortOrder, Boolean active) {
        AdSeminarCategory c = adSeminarCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        c.update(name, sortOrder != null ? sortOrder : c.getSortOrder(), active != null ? active : c.isActive());
        return adSeminarCategoryRepository.save(c);
    }
}
