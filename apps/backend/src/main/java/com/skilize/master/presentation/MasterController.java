package com.skilize.master.presentation;

import com.skilize.master.application.MasterService;
import com.skilize.master.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MasterController {

    private final MasterService masterService;
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
    public ResponseEntity<SkillLevelDto> createSkillLevel(@Valid @RequestBody SkillLevelRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SkillLevelDto.from(masterService.createSkillLevel(req.levelValue(), req.description())));
    }

    @PutMapping("/skill-levels/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SkillLevelDto updateSkillLevel(@PathVariable int id, @Valid @RequestBody SkillLevelRequest req) {
        return SkillLevelDto.from(masterService.updateSkillLevel(id, req.levelValue(), req.description(), req.active()));
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
    public ResponseEntity<ItSkillDto> createItSkill(@Valid @RequestBody ItSkillRequest req) {
        ItSkill saved = masterService.createItSkill(req.categoryId(), req.name(), req.description(), req.sortOrder());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ItSkillDto.from(saved, resolveCategory1(saved)));
    }

    @PutMapping("/it-skills/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ItSkillDto updateItSkill(@PathVariable int id, @Valid @RequestBody ItSkillRequest req) {
        ItSkill skill = masterService.updateItSkill(id, req.categoryId(), req.name(),
                req.description(), req.sortOrder(), req.active());
        return ItSkillDto.from(skill, resolveCategory1(skill));
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
    public ResponseEntity<QualificationDto> createQualification(@Valid @RequestBody QualificationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(QualificationDto.from(
                masterService.createQualification(req.categoryId(), req.name(), req.description(), req.sortOrder())));
    }

    @PutMapping("/qualifications/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public QualificationDto updateQualification(@PathVariable int id, @Valid @RequestBody QualificationRequest req) {
        return QualificationDto.from(masterService.updateQualification(id, req.categoryId(), req.name(),
                req.description(), req.sortOrder(), req.active()));
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
    public ResponseEntity<QualificationCategoryDto> createQualificationCategory(
            @Valid @RequestBody SimpleCategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(QualificationCategoryDto.from(
                masterService.createQualificationCategory(req.name(), req.sortOrder())));
    }

    @PutMapping("/qualification-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public QualificationCategoryDto updateQualificationCategory(@PathVariable int id,
                                                                 @Valid @RequestBody SimpleCategoryRequest req) {
        return QualificationCategoryDto.from(
                masterService.updateQualificationCategory(id, req.name(), req.sortOrder(), req.active()));
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
    public ResponseEntity<AdSeminarDto> createAdSeminar(@Valid @RequestBody AdSeminarRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(AdSeminarDto.from(
                masterService.createAdSeminar(req.categoryId(), req.name(), req.description(), req.sortOrder())));
    }

    @PutMapping("/ad-seminars/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AdSeminarDto updateAdSeminar(@PathVariable int id, @Valid @RequestBody AdSeminarRequest req) {
        return AdSeminarDto.from(masterService.updateAdSeminar(id, req.categoryId(), req.name(),
                req.description(), req.sortOrder(), req.active()));
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
                           : itSkillCategoryRepository.findByActiveTrueOrderBySortOrderAsc();
        return cats.stream().map(ItSkillCategoryDto::from).toList();
    }

    @PostMapping("/it-skill-categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItSkillCategoryDto> createItSkillCategory(@Valid @RequestBody ItSkillCategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ItSkillCategoryDto.from(
                masterService.createItSkillCategory(req.parentId(), req.name(), req.sortOrder())));
    }

    @PutMapping("/it-skill-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ItSkillCategoryDto updateItSkillCategory(@PathVariable int id,
                                                     @Valid @RequestBody ItSkillCategoryUpdateRequest req) {
        return ItSkillCategoryDto.from(
                masterService.updateItSkillCategory(id, req.name(), req.sortOrder(), req.active()));
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
    public ResponseEntity<AdSeminarCategoryDto> createAdSeminarCategory(@Valid @RequestBody SimpleCategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(AdSeminarCategoryDto.from(
                masterService.createAdSeminarCategory(req.name(), req.sortOrder())));
    }

    @PutMapping("/ad-seminar-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AdSeminarCategoryDto updateAdSeminarCategory(@PathVariable int id,
                                                         @Valid @RequestBody SimpleCategoryRequest req) {
        return AdSeminarCategoryDto.from(
                masterService.updateAdSeminarCategory(id, req.name(), req.sortOrder(), req.active()));
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
                              int category1SortOrder,
                              String category2Name, String category3Name,
                              String description, int sortOrder, boolean isActive) {
        static ItSkillDto from(ItSkill s, ItSkillCategory cat1) {
            ItSkillCategory cat = s.getCategory();
            String cat2 = null, cat3 = null;
            if (cat.getLevel() == 3) {
                cat3 = cat.getName();
            } else if (cat.getLevel() == 2) {
                cat2 = cat.getName();
            }
            return new ItSkillDto(s.getId(), s.getName(), cat.getId(),
                    cat1 != null ? cat1.getId() : null,
                    cat1 != null ? cat1.getName() : null,
                    cat1 != null ? cat1.getSortOrder() : 0,
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
