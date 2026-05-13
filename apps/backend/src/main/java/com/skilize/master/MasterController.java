package com.skilize.master;

import com.skilize.domain.master.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MasterController {

    private final SkillLevelRepository skillLevelRepository;
    private final ItSkillRepository itSkillRepository;
    private final QualificationRepository qualificationRepository;
    private final AdSeminarRepository adSeminarRepository;
    private final SeminarCategoryRepository seminarCategoryRepository;
    private final ItSkillCategoryRepository itSkillCategoryRepository;
    private final QualificationCategoryRepository qualificationCategoryRepository;
    private final AdSeminarCategoryRepository adSeminarCategoryRepository;

    @GetMapping("/skill-levels")
    public List<SkillLevelDto> getSkillLevels(@RequestParam(required = false) Boolean isActive) {
        List<SkillLevel> levels = isActive != null
                ? skillLevelRepository.findByActiveOrderByLevelValueAsc(isActive)
                : skillLevelRepository.findAllByOrderByLevelValueAsc();
        return levels.stream().map(SkillLevelDto::from).toList();
    }

    @PostMapping("/skill-levels")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<SkillLevelDto> createSkillLevel(@Valid @RequestBody SkillLevelRequest req) {
        SkillLevel level = SkillLevel.create(req.levelValue(), req.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(SkillLevelDto.from(skillLevelRepository.save(level)));
    }

    @PutMapping("/skill-levels/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public SkillLevelDto updateSkillLevel(@PathVariable int id, @Valid @RequestBody SkillLevelRequest req) {
        SkillLevel level = skillLevelRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        level.update(req.levelValue(), req.description(), req.active() != null ? req.active() : level.isActive());
        return SkillLevelDto.from(skillLevelRepository.save(level));
    }

    @GetMapping("/it-skills")
    public List<ItSkillDto> getItSkills(@RequestParam(required = false) Boolean isActive) {
        List<ItSkill> skills = isActive == null
                ? itSkillRepository.findAllWithCategory()
                : isActive ? itSkillRepository.findAllActiveWithCategory()
                           : itSkillRepository.findAllWithCategoryByActive(false);
        return skills.stream().map(s -> ItSkillDto.from(s, resolveCategory1(s))).toList();
    }

    @PostMapping("/it-skills")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<ItSkillDto> createItSkill(@Valid @RequestBody ItSkillRequest req) {
        ItSkillCategory category = itSkillCategoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "分類が見つかりません"));
        ItSkill skill = ItSkill.create(category, req.name(), req.description(),
                req.sortOrder() != null ? req.sortOrder() : 0);
        ItSkill saved = itSkillRepository.save(skill);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ItSkillDto.from(saved, resolveCategory1(saved)));
    }

    @PutMapping("/it-skills/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ItSkillDto updateItSkill(@PathVariable int id, @Valid @RequestBody ItSkillRequest req) {
        ItSkill skill = itSkillRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ItSkillCategory category = itSkillCategoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "分類が見つかりません"));
        skill.update(category, req.name(), req.description(),
                req.sortOrder() != null ? req.sortOrder() : skill.getSortOrder(),
                req.active() != null ? req.active() : skill.isActive());
        return ItSkillDto.from(itSkillRepository.save(skill), resolveCategory1(skill));
    }

    @GetMapping("/qualifications")
    public List<QualificationDto> getQualifications(@RequestParam(required = false) Boolean isActive) {
        List<Qualification> list = isActive == null
                ? qualificationRepository.findAllWithCategory()
                : isActive ? qualificationRepository.findAllActiveWithCategory()
                           : qualificationRepository.findAllWithCategoryByActive(false);
        return list.stream().map(QualificationDto::from).toList();
    }

    @PostMapping("/qualifications")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<QualificationDto> createQualification(@Valid @RequestBody QualificationRequest req) {
        QualificationCategory cat = req.categoryId() != null
                ? qualificationCategoryRepository.findById(req.categoryId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "カテゴリが見つかりません"))
                : null;
        Qualification q = Qualification.create(cat, req.name(), req.description(),
                req.sortOrder() != null ? req.sortOrder() : 0);
        return ResponseEntity.status(HttpStatus.CREATED).body(QualificationDto.from(qualificationRepository.save(q)));
    }

    @PutMapping("/qualifications/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public QualificationDto updateQualification(@PathVariable int id, @Valid @RequestBody QualificationRequest req) {
        Qualification q = qualificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        QualificationCategory cat = req.categoryId() != null
                ? qualificationCategoryRepository.findById(req.categoryId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "カテゴリが見つかりません"))
                : null;
        q.update(cat, req.name(), req.description(),
                req.sortOrder() != null ? req.sortOrder() : q.getSortOrder(),
                req.active() != null ? req.active() : q.isActive());
        return QualificationDto.from(qualificationRepository.save(q));
    }

    @GetMapping("/qualification-categories")
    public List<QualificationCategoryDto> getQualificationCategories(@RequestParam(required = false) Boolean isActive) {
        List<QualificationCategory> cats = isActive == null
                ? qualificationCategoryRepository.findAllByOrderBySortOrderAsc()
                : qualificationCategoryRepository.findByActiveTrueOrderBySortOrderAsc();
        return cats.stream().map(QualificationCategoryDto::from).toList();
    }

    @PostMapping("/qualification-categories")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<QualificationCategoryDto> createQualificationCategory(
            @Valid @RequestBody SimpleCategoryRequest req) {
        QualificationCategory c = QualificationCategory.create(req.name(), req.sortOrder() != null ? req.sortOrder() : 0);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(QualificationCategoryDto.from(qualificationCategoryRepository.save(c)));
    }

    @PutMapping("/qualification-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public QualificationCategoryDto updateQualificationCategory(@PathVariable int id,
                                                                 @Valid @RequestBody SimpleCategoryRequest req) {
        QualificationCategory c = qualificationCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        c.update(req.name(), req.sortOrder() != null ? req.sortOrder() : c.getSortOrder(),
                req.active() != null ? req.active() : c.isActive());
        return QualificationCategoryDto.from(qualificationCategoryRepository.save(c));
    }

    @GetMapping("/ad-seminars")
    public List<AdSeminarDto> getAdSeminars(@RequestParam(required = false) Boolean isActive) {
        List<AdSeminar> list = isActive == null
                ? adSeminarRepository.findAllWithCategory()
                : isActive ? adSeminarRepository.findAllActiveWithCategory()
                           : adSeminarRepository.findAllWithCategoryByActive(false);
        return list.stream().map(AdSeminarDto::from).toList();
    }

    @PostMapping("/ad-seminars")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<AdSeminarDto> createAdSeminar(@Valid @RequestBody AdSeminarRequest req) {
        AdSeminarCategory cat = req.categoryId() != null
                ? adSeminarCategoryRepository.findById(req.categoryId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "カテゴリが見つかりません"))
                : null;
        AdSeminar a = AdSeminar.create(cat, req.name(), req.description(),
                req.sortOrder() != null ? req.sortOrder() : 0);
        return ResponseEntity.status(HttpStatus.CREATED).body(AdSeminarDto.from(adSeminarRepository.save(a)));
    }

    @PutMapping("/ad-seminars/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AdSeminarDto updateAdSeminar(@PathVariable int id, @Valid @RequestBody AdSeminarRequest req) {
        AdSeminar a = adSeminarRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        AdSeminarCategory cat = req.categoryId() != null
                ? adSeminarCategoryRepository.findById(req.categoryId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "カテゴリが見つかりません"))
                : null;
        a.update(cat, req.name(), req.description(),
                req.sortOrder() != null ? req.sortOrder() : a.getSortOrder(),
                req.active() != null ? req.active() : a.isActive());
        return AdSeminarDto.from(adSeminarRepository.save(a));
    }

    @GetMapping("/seminar-categories")
    public List<SeminarCategoryDto> getSeminarCategories() {
        return seminarCategoryRepository.findByActiveTrueOrderBySortOrderAsc()
                .stream().map(SeminarCategoryDto::from).toList();
    }

    @GetMapping("/it-skill-categories")
    public List<ItSkillCategoryDto> getItSkillCategories(@RequestParam(required = false) Boolean isActive) {
        List<ItSkillCategory> cats = isActive == null
                ? itSkillCategoryRepository.findAllByOrderByLevelAscSortOrderAsc()
                : isActive ? itSkillCategoryRepository.findByActiveTrueOrderBySortOrderAsc()
                           : itSkillCategoryRepository.findByActiveTrueOrderBySortOrderAsc(); // fallback
        return cats.stream().map(ItSkillCategoryDto::from).toList();
    }

    @PostMapping("/it-skill-categories")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<ItSkillCategoryDto> createItSkillCategory(@Valid @RequestBody ItSkillCategoryRequest req) {
        short level;
        if (req.parentId() == null) {
            level = 1;
        } else {
            ItSkillCategory parent = itSkillCategoryRepository.findById(req.parentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "親分類が見つかりません"));
            level = (short) (parent.getLevel() + 1);
            if (level > 3) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "第3階層以上の分類は作成できません");
            }
        }
        ItSkillCategory cat = ItSkillCategory.create(req.parentId(), level, req.name(),
                req.sortOrder() != null ? req.sortOrder() : 0);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ItSkillCategoryDto.from(itSkillCategoryRepository.save(cat)));
    }

    @PutMapping("/it-skill-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ItSkillCategoryDto updateItSkillCategory(@PathVariable int id,
                                                     @Valid @RequestBody ItSkillCategoryUpdateRequest req) {
        ItSkillCategory cat = itSkillCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        cat.update(req.name(), req.sortOrder() != null ? req.sortOrder() : cat.getSortOrder(),
                req.active() != null ? req.active() : cat.isActive());
        return ItSkillCategoryDto.from(itSkillCategoryRepository.save(cat));
    }

    @GetMapping("/ad-seminar-categories")
    public List<AdSeminarCategoryDto> getAdSeminarCategories(@RequestParam(required = false) Boolean isActive) {
        List<AdSeminarCategory> cats = isActive == null
                ? adSeminarCategoryRepository.findAllByOrderBySortOrderAsc()
                : adSeminarCategoryRepository.findByActiveTrueOrderBySortOrderAsc();
        return cats.stream().map(AdSeminarCategoryDto::from).toList();
    }

    @PostMapping("/ad-seminar-categories")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<AdSeminarCategoryDto> createAdSeminarCategory(@Valid @RequestBody SimpleCategoryRequest req) {
        AdSeminarCategory c = AdSeminarCategory.create(req.name(), req.sortOrder() != null ? req.sortOrder() : 0);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AdSeminarCategoryDto.from(adSeminarCategoryRepository.save(c)));
    }

    @PutMapping("/ad-seminar-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AdSeminarCategoryDto updateAdSeminarCategory(@PathVariable int id,
                                                         @Valid @RequestBody SimpleCategoryRequest req) {
        AdSeminarCategory c = adSeminarCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        c.update(req.name(), req.sortOrder() != null ? req.sortOrder() : c.getSortOrder(),
                req.active() != null ? req.active() : c.isActive());
        return AdSeminarCategoryDto.from(adSeminarCategoryRepository.save(c));
    }

    private ItSkillCategory resolveCategory1(ItSkill skill) {
        ItSkillCategory cat = skill.getCategory();
        if (cat.getLevel() == 1) return cat;
        return itSkillCategoryRepository.findById(cat.getParentId())
                .map(parent -> parent.getLevel() == 1 ? parent
                        : itSkillCategoryRepository.findById(parent.getParentId()).orElse(parent))
                .orElse(cat);
    }

    public record SkillLevelDto(int id, short levelValue, String description, boolean isActive) {
        static SkillLevelDto from(SkillLevel s) {
            return new SkillLevelDto(s.getId(), s.getLevelValue(), s.getDescription(), s.isActive());
        }
    }

    public record SkillLevelRequest(
            @NotNull @Min(1) Short levelValue,
            @NotBlank String description,
            Boolean active
    ) {}

    public record ItSkillRequest(
            @NotNull Integer categoryId,
            @NotBlank String name,
            String description,
            Integer sortOrder,
            Boolean active
    ) {}

    public record ItSkillCategoryRequest(
            Integer parentId,
            @NotBlank String name,
            Integer sortOrder
    ) {}

    public record ItSkillCategoryUpdateRequest(
            @NotBlank String name,
            Integer sortOrder,
            Boolean active
    ) {}

    public record SimpleCategoryRequest(
            @NotBlank String name,
            Integer sortOrder,
            Boolean active
    ) {}

    public record QualificationRequest(
            Integer categoryId,
            @NotBlank String name,
            String description,
            Integer sortOrder,
            Boolean active
    ) {}

    public record AdSeminarRequest(
            Integer categoryId,
            @NotBlank String name,
            String description,
            Integer sortOrder,
            Boolean active
    ) {}

    public record ItSkillDto(int id, String name, int categoryId, Integer category1Id, String category1Name,
                              String category2Name, String category3Name,
                              String description, int sortOrder, boolean isActive) {
        static ItSkillDto from(ItSkill s, ItSkillCategory cat1) {
            ItSkillCategory cat = s.getCategory();
            String cat2 = null, cat3 = null;
            if (cat.getLevel() == 3) {
                cat3 = cat.getName();
                cat2 = null;
            } else if (cat.getLevel() == 2) {
                cat2 = cat.getName();
            }
            return new ItSkillDto(s.getId(), s.getName(), cat.getId(),
                    cat1 != null ? cat1.getId() : null,
                    cat1 != null ? cat1.getName() : null,
                    cat2, cat3, s.getDescription(), s.getSortOrder(), s.isActive());
        }
    }

    public record QualificationDto(int id, String name, Integer categoryId, String categoryName,
                                    String description, int sortOrder, boolean isActive) {
        static QualificationDto from(Qualification q) {
            return new QualificationDto(q.getId(), q.getName(),
                    q.getCategory() != null ? q.getCategory().getId() : null,
                    q.getCategory() != null ? q.getCategory().getName() : null,
                    q.getDescription(), q.getSortOrder(), q.isActive());
        }
    }

    public record AdSeminarDto(int id, String name, Integer categoryId, String categoryName,
                                String description, int sortOrder, boolean isActive) {
        static AdSeminarDto from(AdSeminar a) {
            return new AdSeminarDto(a.getId(), a.getName(),
                    a.getCategory() != null ? a.getCategory().getId() : null,
                    a.getCategory() != null ? a.getCategory().getName() : null,
                    a.getDescription(), a.getSortOrder(), a.isActive());
        }
    }

    public record SeminarCategoryDto(int id, String name, int sortOrder, boolean isActive) {
        static SeminarCategoryDto from(SeminarCategory c) {
            return new SeminarCategoryDto(c.getId(), c.getName(), c.getSortOrder(), c.isActive());
        }
    }

    public record ItSkillCategoryDto(int id, Integer parentId, short level, String name,
                                      int sortOrder, boolean isActive) {
        static ItSkillCategoryDto from(ItSkillCategory c) {
            return new ItSkillCategoryDto(c.getId(), c.getParentId(), c.getLevel(),
                    c.getName(), c.getSortOrder(), c.isActive());
        }
    }

    public record QualificationCategoryDto(int id, String name, int sortOrder, boolean isActive) {
        static QualificationCategoryDto from(QualificationCategory c) {
            return new QualificationCategoryDto(c.getId(), c.getName(), c.getSortOrder(), c.isActive());
        }
    }

    public record AdSeminarCategoryDto(int id, String name, int sortOrder, boolean isActive) {
        static AdSeminarCategoryDto from(AdSeminarCategory c) {
            return new AdSeminarCategoryDto(c.getId(), c.getName(), c.getSortOrder(), c.isActive());
        }
    }
}
