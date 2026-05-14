package com.skilize.master.application;

import com.skilize.master.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    @Transactional
    public SkillLevel createSkillLevel(Short levelValue, String description) {
        return skillLevelRepository.save(SkillLevel.create(levelValue, description));
    }

    @Transactional
    public SkillLevel updateSkillLevel(int id, Short levelValue, String description, Boolean active) {
        SkillLevel level = skillLevelRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        level.update(levelValue, description, active != null ? active : level.isActive());
        return skillLevelRepository.save(level);
    }

    @Transactional
    public ItSkill createItSkill(int categoryId, String name, String description, Integer sortOrder) {
        ItSkillCategory category = itSkillCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "分類が見つかりません"));
        return itSkillRepository.save(ItSkill.create(category, name, description, sortOrder != null ? sortOrder : 0));
    }

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

    @Transactional
    public ItSkillCategory createItSkillCategory(Integer parentId, String name, Integer sortOrder) {
        short level;
        if (parentId == null) {
            level = 1;
        } else {
            ItSkillCategory parent = itSkillCategoryRepository.findById(parentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "親分類が見つかりません"));
            level = (short) (parent.getLevel() + 1);
            if (level > 3) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "第3階層以上の分類は作成できません");
            }
        }
        return itSkillCategoryRepository.save(
                ItSkillCategory.create(parentId, level, name, sortOrder != null ? sortOrder : 0));
    }

    @Transactional
    public ItSkillCategory updateItSkillCategory(int id, String name, Integer sortOrder, Boolean active) {
        ItSkillCategory cat = itSkillCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        cat.update(name, sortOrder != null ? sortOrder : cat.getSortOrder(), active != null ? active : cat.isActive());
        return itSkillCategoryRepository.save(cat);
    }

    @Transactional
    public Qualification createQualification(Integer categoryId, String name, String description, Integer sortOrder) {
        QualificationCategory cat = categoryId != null
                ? qualificationCategoryRepository.findById(categoryId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "カテゴリが見つかりません"))
                : null;
        return qualificationRepository.save(Qualification.create(cat, name, description, sortOrder != null ? sortOrder : 0));
    }

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

    @Transactional
    public QualificationCategory createQualificationCategory(String name, Integer sortOrder) {
        return qualificationCategoryRepository.save(
                QualificationCategory.create(name, sortOrder != null ? sortOrder : 0));
    }

    @Transactional
    public QualificationCategory updateQualificationCategory(int id, String name, Integer sortOrder, Boolean active) {
        QualificationCategory c = qualificationCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        c.update(name, sortOrder != null ? sortOrder : c.getSortOrder(), active != null ? active : c.isActive());
        return qualificationCategoryRepository.save(c);
    }

    @Transactional
    public AdSeminar createAdSeminar(Integer categoryId, String name, String description, Integer sortOrder) {
        AdSeminarCategory cat = categoryId != null
                ? adSeminarCategoryRepository.findById(categoryId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "カテゴリが見つかりません"))
                : null;
        return adSeminarRepository.save(AdSeminar.create(cat, name, description, sortOrder != null ? sortOrder : 0));
    }

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

    @Transactional
    public AdSeminarCategory createAdSeminarCategory(String name, Integer sortOrder) {
        return adSeminarCategoryRepository.save(
                AdSeminarCategory.create(name, sortOrder != null ? sortOrder : 0));
    }

    @Transactional
    public AdSeminarCategory updateAdSeminarCategory(int id, String name, Integer sortOrder, Boolean active) {
        AdSeminarCategory c = adSeminarCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        c.update(name, sortOrder != null ? sortOrder : c.getSortOrder(), active != null ? active : c.isActive());
        return adSeminarCategoryRepository.save(c);
    }
}
