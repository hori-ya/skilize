/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * マスタデータ（スキルレベル・ITスキル・資格・ADセミナーとその分類）の
 * 作成・更新・カスタム昇格ビジネスロジックを提供するアプリケーションサービス。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.application;

import com.skilize.inventory.domain.ItSkillDetailRepository;
import com.skilize.inventory.domain.QualificationDetailRepository;
import com.skilize.master.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * マスタデータ（スキルレベル・ITスキル・ITスキル分類・資格・資格分類・ADセミナー・ADセミナー分類）の
 * 作成・更新ビジネスロジック。TL/ADMIN 操作が中心。
 * 各マスタは is_active フラグで論理削除を管理し、物理削除は行わない（過去棚卸の参照を保持するため）。
 */
@Slf4j
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
    // 昇格時に棚卸明細のカスタム名をマスタへ紐付けるため inventory ドメインのリポジトリを参照する
    private final ItSkillDetailRepository itSkillDetailRepository;
    private final QualificationDetailRepository qualificationDetailRepository;

    /** スキルレベルを新規作成する。 */
    @Transactional
    public SkillLevel createSkillLevel(Short levelValue, String description, int scoreWeight) {
        SkillLevel saved = skillLevelRepository.save(SkillLevel.create(levelValue, description, scoreWeight));
        log.info("SkillLevel created: id={} level={}", saved.getId(), levelValue);
        return saved;
    }

    /**
     * スキルレベルを更新する。
     * active が null の場合は現在の値を維持する（部分更新パターン）。
     */
    @Transactional
    public SkillLevel updateSkillLevel(int id, Short levelValue, String description, Boolean active, int scoreWeight) {
        SkillLevel level = skillLevelRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        // active が null = フロントエンドから未送信 → 既存の値をそのまま使う
        level.update(levelValue, description, active != null ? active : level.isActive(), scoreWeight);
        SkillLevel saved = skillLevelRepository.save(level);
        log.info("SkillLevel updated: id={}", id);
        return saved;
    }

    /** ITスキルを新規作成する。sortOrder が未指定の場合は 0 として登録する。 */
    @Transactional
    public ItSkill createItSkill(int categoryId, String name, String description, Integer sortOrder) {
        ItSkillCategory category = itSkillCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "分類が見つかりません"));
        ItSkill saved = itSkillRepository.save(ItSkill.create(category, name, description, sortOrder != null ? sortOrder : 0));
        log.info("ItSkill created: id={}", saved.getId());
        return saved;
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
        ItSkill saved = itSkillRepository.save(skill);
        log.info("ItSkill updated: id={}", id);
        return saved;
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
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "PARENT_CATEGORY_NOT_FOUND"));
            // 子のレベルは親のレベル + 1。最大3階層まで許可する。
            level = (short) (parent.getLevel() + 1);
            if (level > 3) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CATEGORY_MAX_DEPTH_EXCEEDED");
            }
        }
        ItSkillCategory saved = itSkillCategoryRepository.save(
                ItSkillCategory.create(parentId, level, name, sortOrder != null ? sortOrder : 0));
        log.info("ItSkillCategory created: id={} level={}", saved.getId(), level);
        return saved;
    }

    /** ITスキル分類を更新する。sortOrder・active が null の場合は現在の値を維持する。 */
    @Transactional
    public ItSkillCategory updateItSkillCategory(int id, String name, Integer sortOrder, Boolean active) {
        ItSkillCategory cat = itSkillCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        cat.update(name, sortOrder != null ? sortOrder : cat.getSortOrder(), active != null ? active : cat.isActive());
        ItSkillCategory saved = itSkillCategoryRepository.save(cat);
        log.info("ItSkillCategory updated: id={}", id);
        return saved;
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
        Qualification saved = qualificationRepository.save(Qualification.create(cat, name, description, sortOrder != null ? sortOrder : 0));
        log.info("Qualification created: id={}", saved.getId());
        return saved;
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
        Qualification saved = qualificationRepository.save(q);
        log.info("Qualification updated: id={}", id);
        return saved;
    }

    /** 資格分類を新規作成する。 */
    @Transactional
    public QualificationCategory createQualificationCategory(String name, Integer sortOrder) {
        QualificationCategory saved = qualificationCategoryRepository.save(
                QualificationCategory.create(name, sortOrder != null ? sortOrder : 0));
        log.info("QualificationCategory created: id={}", saved.getId());
        return saved;
    }

    /** 資格分類を更新する。sortOrder・active が null の場合は現在の値を維持する。 */
    @Transactional
    public QualificationCategory updateQualificationCategory(int id, String name, Integer sortOrder, Boolean active) {
        QualificationCategory c = qualificationCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        c.update(name, sortOrder != null ? sortOrder : c.getSortOrder(), active != null ? active : c.isActive());
        QualificationCategory saved = qualificationCategoryRepository.save(c);
        log.info("QualificationCategory updated: id={}", id);
        return saved;
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
        AdSeminar saved = adSeminarRepository.save(AdSeminar.create(cat, name, description, sortOrder != null ? sortOrder : 0));
        log.info("AdSeminar created: id={}", saved.getId());
        return saved;
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
        AdSeminar saved = adSeminarRepository.save(a);
        log.info("AdSeminar updated: id={}", id);
        return saved;
    }

    /** ADセミナー分類を新規作成する。 */
    @Transactional
    public AdSeminarCategory createAdSeminarCategory(String name, Integer sortOrder) {
        AdSeminarCategory saved = adSeminarCategoryRepository.save(
                AdSeminarCategory.create(name, sortOrder != null ? sortOrder : 0));
        log.info("AdSeminarCategory created: id={}", saved.getId());
        return saved;
    }

    /** ADセミナー分類を更新する。sortOrder・active が null の場合は現在の値を維持する。 */
    @Transactional
    public AdSeminarCategory updateAdSeminarCategory(int id, String name, Integer sortOrder, Boolean active) {
        AdSeminarCategory c = adSeminarCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        c.update(name, sortOrder != null ? sortOrder : c.getSortOrder(), active != null ? active : c.isActive());
        AdSeminarCategory saved = adSeminarCategoryRepository.save(c);
        log.info("AdSeminarCategory updated: id={}", id);
        return saved;
    }

    /** マスタ未登録のカスタムITスキル名一覧を使用件数付きで返す。 */
    public List<Object[]> getCustomUnregisteredItSkills() {
        return itSkillDetailRepository.findCustomUnregisteredSkillNames();
    }

    /**
     * カスタムITスキルをマスタに昇格する。
     * 新規 ItSkill を登録し、同名カスタムスキル明細を新マスタへ紐付ける（同一トランザクション）。
     */
    @Transactional
    public ItSkill promoteItSkill(String customName, int categoryId, String name, String description, Integer sortOrder) {
        ItSkillCategory category = itSkillCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "分類が見つかりません"));
        ItSkill skill = itSkillRepository.save(
                ItSkill.create(category, name, description, sortOrder != null ? sortOrder : 0));
        itSkillDetailRepository.linkToMasterSkill(customName, skill);
        log.info("ItSkill promoted: customName={} newId={}", customName, skill.getId());
        return skill;
    }

    /** マスタ未登録のカスタム資格名一覧を使用件数付きで返す。 */
    public List<Object[]> getCustomUnregisteredQualifications() {
        return qualificationDetailRepository.findCustomUnregisteredQualificationNames();
    }

    /**
     * カスタム資格をマスタに昇格する。
     * 新規 Qualification を登録し、同名カスタム資格明細を新マスタへ紐付ける（同一トランザクション）。
     */
    @Transactional
    public Qualification promoteQualification(String customName, Integer categoryId, String name, String description, Integer sortOrder) {
        QualificationCategory cat = categoryId != null
                ? qualificationCategoryRepository.findById(categoryId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "カテゴリが見つかりません"))
                : null;
        Qualification q = qualificationRepository.save(
                Qualification.create(cat, name, description, sortOrder != null ? sortOrder : 0));
        qualificationDetailRepository.linkToMasterQualification(customName, q);
        log.info("Qualification promoted: customName={} newId={}", customName, q.getId());
        return q;
    }
}
