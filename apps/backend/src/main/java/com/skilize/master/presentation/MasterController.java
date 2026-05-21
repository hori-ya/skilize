package com.skilize.master.presentation;

import com.skilize.master.application.MasterService;
import com.skilize.master.domain.*;
import com.skilize.master.presentation.request.*;
import com.skilize.master.presentation.response.*;
import jakarta.validation.Valid;
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
    public List<SkillLevelResponse> getSkillLevels(@RequestParam(required = false) Boolean isActive) {
        List<SkillLevel> levels = isActive != null
                ? skillLevelRepository.findByActiveOrderByLevelValueAsc(isActive)
                : skillLevelRepository.findAllByOrderByLevelValueAsc();
        return levels.stream().map(SkillLevelResponse::from).toList();
    }

    @PostMapping("/skill-levels")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SkillLevelResponse> createSkillLevel(@Valid @RequestBody SkillLevelRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SkillLevelResponse.from(masterService.createSkillLevel(req.levelValue(), req.description(), req.scoreWeight())));
    }

    @PutMapping("/skill-levels/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SkillLevelResponse updateSkillLevel(@PathVariable int id, @Valid @RequestBody SkillLevelRequest req) {
        return SkillLevelResponse.from(masterService.updateSkillLevel(id, req.levelValue(), req.description(), req.active(), req.scoreWeight()));
    }

    @GetMapping("/it-skills")
    public List<ItSkillResponse> getItSkills(@RequestParam(required = false) Boolean isActive) {
        List<ItSkill> skills = isActive == null
                ? itSkillRepository.findAllWithCategory()
                : isActive ? itSkillRepository.findAllActiveWithCategory()
                           : itSkillRepository.findAllWithCategoryByActive(false);
        return skills.stream().map(s -> ItSkillResponse.from(s, resolveCategory1(s))).toList();
    }

    @PostMapping("/it-skills")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItSkillResponse> createItSkill(@Valid @RequestBody ItSkillRequest req) {
        ItSkill saved = masterService.createItSkill(req.categoryId(), req.name(), req.description(), req.sortOrder());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ItSkillResponse.from(saved, resolveCategory1(saved)));
    }

    @PutMapping("/it-skills/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ItSkillResponse updateItSkill(@PathVariable int id, @Valid @RequestBody ItSkillRequest req) {
        ItSkill skill = masterService.updateItSkill(id, req.categoryId(), req.name(),
                req.description(), req.sortOrder(), req.active());
        return ItSkillResponse.from(skill, resolveCategory1(skill));
    }

    @GetMapping("/it-skills/custom-unregistered")
    @PreAuthorize("hasAnyRole('TL', 'ADMIN')")
    public List<CustomUnregisteredResponse> getCustomUnregisteredItSkills() {
        return masterService.getCustomUnregisteredItSkills().stream()
                .map(row -> new CustomUnregisteredResponse((String) row[0], (long) row[1]))
                .toList();
    }

    @PostMapping("/it-skills/promote")
    @PreAuthorize("hasAnyRole('TL', 'ADMIN')")
    public ResponseEntity<ItSkillResponse> promoteItSkill(@Valid @RequestBody PromoteItSkillRequest req) {
        ItSkill skill = masterService.promoteItSkill(req.customName(), req.categoryId(), req.name(),
                req.description(), req.sortOrder());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ItSkillResponse.from(skill, resolveCategory1(skill)));
    }

    @GetMapping("/qualifications")
    public List<QualificationResponse> getQualifications(@RequestParam(required = false) Boolean isActive) {
        List<Qualification> list = isActive == null
                ? qualificationRepository.findAllWithCategory()
                : isActive ? qualificationRepository.findAllActiveWithCategory()
                           : qualificationRepository.findAllWithCategoryByActive(false);
        return list.stream().map(QualificationResponse::from).toList();
    }

    @PostMapping("/qualifications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QualificationResponse> createQualification(@Valid @RequestBody QualificationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(QualificationResponse.from(
                masterService.createQualification(req.categoryId(), req.name(), req.description(), req.sortOrder())));
    }

    @PutMapping("/qualifications/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public QualificationResponse updateQualification(@PathVariable int id, @Valid @RequestBody QualificationRequest req) {
        return QualificationResponse.from(masterService.updateQualification(id, req.categoryId(), req.name(),
                req.description(), req.sortOrder(), req.active()));
    }

    @GetMapping("/qualifications/custom-unregistered")
    @PreAuthorize("hasAnyRole('TL', 'ADMIN')")
    public List<CustomUnregisteredResponse> getCustomUnregisteredQualifications() {
        return masterService.getCustomUnregisteredQualifications().stream()
                .map(row -> new CustomUnregisteredResponse((String) row[0], (long) row[1]))
                .toList();
    }

    @PostMapping("/qualifications/promote")
    @PreAuthorize("hasAnyRole('TL', 'ADMIN')")
    public ResponseEntity<QualificationResponse> promoteQualification(@Valid @RequestBody PromoteQualificationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(QualificationResponse.from(
                masterService.promoteQualification(req.customName(), req.categoryId(), req.name(),
                        req.description(), req.sortOrder())));
    }

    @GetMapping("/qualification-categories")
    public List<QualificationCategoryResponse> getQualificationCategories(@RequestParam(required = false) Boolean isActive) {
        List<QualificationCategory> cats = isActive == null
                ? qualificationCategoryRepository.findAllByOrderBySortOrderAsc()
                : qualificationCategoryRepository.findByActiveTrueOrderBySortOrderAsc();
        return cats.stream().map(QualificationCategoryResponse::from).toList();
    }

    @PostMapping("/qualification-categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QualificationCategoryResponse> createQualificationCategory(
            @Valid @RequestBody SimpleCategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(QualificationCategoryResponse.from(
                masterService.createQualificationCategory(req.name(), req.sortOrder())));
    }

    @PutMapping("/qualification-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public QualificationCategoryResponse updateQualificationCategory(@PathVariable int id,
                                                                      @Valid @RequestBody SimpleCategoryRequest req) {
        return QualificationCategoryResponse.from(
                masterService.updateQualificationCategory(id, req.name(), req.sortOrder(), req.active()));
    }

    @GetMapping("/ad-seminars")
    public List<AdSeminarResponse> getAdSeminars(@RequestParam(required = false) Boolean isActive) {
        List<AdSeminar> list = isActive == null
                ? adSeminarRepository.findAllWithCategory()
                : isActive ? adSeminarRepository.findAllActiveWithCategory()
                           : adSeminarRepository.findAllWithCategoryByActive(false);
        return list.stream().map(AdSeminarResponse::from).toList();
    }

    @PostMapping("/ad-seminars")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdSeminarResponse> createAdSeminar(@Valid @RequestBody AdSeminarRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(AdSeminarResponse.from(
                masterService.createAdSeminar(req.categoryId(), req.name(), req.description(), req.sortOrder())));
    }

    @PutMapping("/ad-seminars/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AdSeminarResponse updateAdSeminar(@PathVariable int id, @Valid @RequestBody AdSeminarRequest req) {
        return AdSeminarResponse.from(masterService.updateAdSeminar(id, req.categoryId(), req.name(),
                req.description(), req.sortOrder(), req.active()));
    }

    @GetMapping("/seminar-categories")
    public List<SeminarCategoryResponse> getSeminarCategories() {
        return seminarCategoryRepository.findByActiveTrueOrderBySortOrderAsc()
                .stream().map(SeminarCategoryResponse::from).toList();
    }

    @GetMapping("/it-skill-categories")
    public List<ItSkillCategoryResponse> getItSkillCategories(@RequestParam(required = false) Boolean isActive) {
        List<ItSkillCategory> cats = isActive == null
                ? itSkillCategoryRepository.findAllByOrderByLevelAscSortOrderAsc()
                : isActive ? itSkillCategoryRepository.findByActiveTrueOrderBySortOrderAsc()
                           : itSkillCategoryRepository.findByActiveTrueOrderBySortOrderAsc();
        return cats.stream().map(ItSkillCategoryResponse::from).toList();
    }

    @PostMapping("/it-skill-categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItSkillCategoryResponse> createItSkillCategory(@Valid @RequestBody ItSkillCategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ItSkillCategoryResponse.from(
                masterService.createItSkillCategory(req.parentId(), req.name(), req.sortOrder())));
    }

    @PutMapping("/it-skill-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ItSkillCategoryResponse updateItSkillCategory(@PathVariable int id,
                                                          @Valid @RequestBody ItSkillCategoryUpdateRequest req) {
        return ItSkillCategoryResponse.from(
                masterService.updateItSkillCategory(id, req.name(), req.sortOrder(), req.active()));
    }

    @GetMapping("/ad-seminar-categories")
    public List<AdSeminarCategoryResponse> getAdSeminarCategories(@RequestParam(required = false) Boolean isActive) {
        List<AdSeminarCategory> cats = isActive == null
                ? adSeminarCategoryRepository.findAllByOrderBySortOrderAsc()
                : adSeminarCategoryRepository.findByActiveTrueOrderBySortOrderAsc();
        return cats.stream().map(AdSeminarCategoryResponse::from).toList();
    }

    @PostMapping("/ad-seminar-categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdSeminarCategoryResponse> createAdSeminarCategory(@Valid @RequestBody SimpleCategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(AdSeminarCategoryResponse.from(
                masterService.createAdSeminarCategory(req.name(), req.sortOrder())));
    }

    @PutMapping("/ad-seminar-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AdSeminarCategoryResponse updateAdSeminarCategory(@PathVariable int id,
                                                              @Valid @RequestBody SimpleCategoryRequest req) {
        return AdSeminarCategoryResponse.from(
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
}
