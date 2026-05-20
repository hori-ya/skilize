package com.skilize.master.presentation;

import com.skilize.master.application.MasterService;
import com.skilize.master.dto.*;
import com.skilize.master.domain.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * マスタデータ（スキルレベル・ITスキル・資格・ADセミナー・各分類）の REST API コントローラー。
 * 参照は全ロール可。作成・更新は ADMIN のみ（@PreAuthorize で制御）。
 * isActive クエリパラメータで有効/無効/全件のフィルタリングが可能。
 */
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

    /**
     * スキルレベル一覧を返す。isActive=true で有効のみ、isActive=false で無効のみ、未指定で全件。
     * 棚卸入力画面では isActive=true を指定して有効なレベルのみ取得する。
     */
    @GetMapping("/skill-levels")
    public List<SkillLevelDto> getSkillLevels(@RequestParam(required = false) Boolean isActive) {
        List<SkillLevel> levels = isActive != null
                ? skillLevelRepository.findByActiveOrderByLevelValueAsc(isActive)
                : skillLevelRepository.findAllByOrderByLevelValueAsc();
        return levels.stream().map(SkillLevelDto::from).toList();
    }

    /** スキルレベルを新規作成する（ADMIN のみ）。 */
    @PostMapping("/skill-levels")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SkillLevelDto> createSkillLevel(@Valid @RequestBody SkillLevelRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SkillLevelDto.from(masterService.createSkillLevel(req.levelValue(), req.description(), req.scoreWeight())));
    }

    /** スキルレベルを更新する（ADMIN のみ）。active=null の場合は現在の値を維持する。 */
    @PutMapping("/skill-levels/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SkillLevelDto updateSkillLevel(@PathVariable int id, @Valid @RequestBody SkillLevelRequest req) {
        return SkillLevelDto.from(masterService.updateSkillLevel(id, req.levelValue(), req.description(), req.active(), req.scoreWeight()));
    }

    /**
     * ITスキル一覧を返す。isActive フィルタリングと大分類（level=1 のカテゴリ）解決を行う。
     * resolveCategory1() で階層を遡り大分類を取得している（DTO のソート・グループ化に使用）。
     */
    @GetMapping("/it-skills")
    public List<ItSkillDto> getItSkills(@RequestParam(required = false) Boolean isActive) {
        List<ItSkill> skills = isActive == null
                ? itSkillRepository.findAllWithCategory()
                : isActive ? itSkillRepository.findAllActiveWithCategory()
                           : itSkillRepository.findAllWithCategoryByActive(false);
        return skills.stream().map(s -> ItSkillDto.from(s, resolveCategory1(s))).toList();
    }

    /** ITスキルを新規作成する（ADMIN のみ）。 */
    @PostMapping("/it-skills")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItSkillDto> createItSkill(@Valid @RequestBody ItSkillRequest req) {
        ItSkill saved = masterService.createItSkill(req.categoryId(), req.name(), req.description(), req.sortOrder());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ItSkillDto.from(saved, resolveCategory1(saved)));
    }

    /** ITスキルを更新する（ADMIN のみ）。 */
    @PutMapping("/it-skills/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ItSkillDto updateItSkill(@PathVariable int id, @Valid @RequestBody ItSkillRequest req) {
        ItSkill skill = masterService.updateItSkill(id, req.categoryId(), req.name(),
                req.description(), req.sortOrder(), req.active());
        return ItSkillDto.from(skill, resolveCategory1(skill));
    }

    /** マスタ未登録のカスタムITスキル名一覧を返す（TL/ADMIN）。 */
    @GetMapping("/it-skills/custom-unregistered")
    @PreAuthorize("hasAnyRole('TL', 'ADMIN')")
    public List<CustomUnregisteredDto> getCustomUnregisteredItSkills() {
        return masterService.getCustomUnregisteredItSkills().stream()
                .map(row -> new CustomUnregisteredDto((String) row[0], (long) row[1]))
                .toList();
    }

    /** カスタムITスキルをマスタに昇格する（TL/ADMIN）。 */
    @PostMapping("/it-skills/promote")
    @PreAuthorize("hasAnyRole('TL', 'ADMIN')")
    public ResponseEntity<ItSkillDto> promoteItSkill(@Valid @RequestBody PromoteItSkillRequest req) {
        ItSkill skill = masterService.promoteItSkill(req.customName(), req.categoryId(), req.name(),
                req.description(), req.sortOrder());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ItSkillDto.from(skill, resolveCategory1(skill)));
    }

    /** 資格一覧を返す。isActive フィルタリング可能。 */
    @GetMapping("/qualifications")
    public List<QualificationDto> getQualifications(@RequestParam(required = false) Boolean isActive) {
        List<Qualification> list = isActive == null
                ? qualificationRepository.findAllWithCategory()
                : isActive ? qualificationRepository.findAllActiveWithCategory()
                           : qualificationRepository.findAllWithCategoryByActive(false);
        return list.stream().map(QualificationDto::from).toList();
    }

    /** 資格を新規作成する（ADMIN のみ）。categoryId は省略可能（null = 分類なし）。 */
    @PostMapping("/qualifications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QualificationDto> createQualification(@Valid @RequestBody QualificationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(QualificationDto.from(
                masterService.createQualification(req.categoryId(), req.name(), req.description(), req.sortOrder())));
    }

    /** 資格を更新する（ADMIN のみ）。 */
    @PutMapping("/qualifications/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public QualificationDto updateQualification(@PathVariable int id, @Valid @RequestBody QualificationRequest req) {
        return QualificationDto.from(masterService.updateQualification(id, req.categoryId(), req.name(),
                req.description(), req.sortOrder(), req.active()));
    }

    /** マスタ未登録のカスタム資格名一覧を返す（TL/ADMIN）。 */
    @GetMapping("/qualifications/custom-unregistered")
    @PreAuthorize("hasAnyRole('TL', 'ADMIN')")
    public List<CustomUnregisteredDto> getCustomUnregisteredQualifications() {
        return masterService.getCustomUnregisteredQualifications().stream()
                .map(row -> new CustomUnregisteredDto((String) row[0], (long) row[1]))
                .toList();
    }

    /** カスタム資格をマスタに昇格する（TL/ADMIN）。 */
    @PostMapping("/qualifications/promote")
    @PreAuthorize("hasAnyRole('TL', 'ADMIN')")
    public ResponseEntity<QualificationDto> promoteQualification(@Valid @RequestBody PromoteQualificationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(QualificationDto.from(
                masterService.promoteQualification(req.customName(), req.categoryId(), req.name(),
                        req.description(), req.sortOrder())));
    }

    /** 資格分類一覧を返す。isActive=true の場合は有効のみ、未指定で全件。 */
    @GetMapping("/qualification-categories")
    public List<QualificationCategoryDto> getQualificationCategories(@RequestParam(required = false) Boolean isActive) {
        List<QualificationCategory> cats = isActive == null
                ? qualificationCategoryRepository.findAllByOrderBySortOrderAsc()
                : qualificationCategoryRepository.findByActiveTrueOrderBySortOrderAsc();
        return cats.stream().map(QualificationCategoryDto::from).toList();
    }

    /** 資格分類を新規作成する（ADMIN のみ）。 */
    @PostMapping("/qualification-categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QualificationCategoryDto> createQualificationCategory(
            @Valid @RequestBody SimpleCategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(QualificationCategoryDto.from(
                masterService.createQualificationCategory(req.name(), req.sortOrder())));
    }

    /** 資格分類を更新する（ADMIN のみ）。 */
    @PutMapping("/qualification-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public QualificationCategoryDto updateQualificationCategory(@PathVariable int id,
                                                                 @Valid @RequestBody SimpleCategoryRequest req) {
        return QualificationCategoryDto.from(
                masterService.updateQualificationCategory(id, req.name(), req.sortOrder(), req.active()));
    }

    /** ADセミナー一覧を返す。isActive フィルタリング可能。 */
    @GetMapping("/ad-seminars")
    public List<AdSeminarDto> getAdSeminars(@RequestParam(required = false) Boolean isActive) {
        List<AdSeminar> list = isActive == null
                ? adSeminarRepository.findAllWithCategory()
                : isActive ? adSeminarRepository.findAllActiveWithCategory()
                           : adSeminarRepository.findAllWithCategoryByActive(false);
        return list.stream().map(AdSeminarDto::from).toList();
    }

    /** ADセミナーを新規作成する（ADMIN のみ）。categoryId は省略可能。 */
    @PostMapping("/ad-seminars")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdSeminarDto> createAdSeminar(@Valid @RequestBody AdSeminarRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(AdSeminarDto.from(
                masterService.createAdSeminar(req.categoryId(), req.name(), req.description(), req.sortOrder())));
    }

    /** ADセミナーを更新する（ADMIN のみ）。 */
    @PutMapping("/ad-seminars/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AdSeminarDto updateAdSeminar(@PathVariable int id, @Valid @RequestBody AdSeminarRequest req) {
        return AdSeminarDto.from(masterService.updateAdSeminar(id, req.categoryId(), req.name(),
                req.description(), req.sortOrder(), req.active()));
    }

    /** セミナー分類（自由入力セミナー向け）の一覧を返す。有効なもののみ返す。 */
    @GetMapping("/seminar-categories")
    public List<SeminarCategoryDto> getSeminarCategories() {
        return seminarCategoryRepository.findByActiveTrueOrderBySortOrderAsc()
                .stream().map(SeminarCategoryDto::from).toList();
    }

    /** ITスキル分類一覧を返す。階層レベル→表示順の昇順で返す。 */
    @GetMapping("/it-skill-categories")
    public List<ItSkillCategoryDto> getItSkillCategories(@RequestParam(required = false) Boolean isActive) {
        List<ItSkillCategory> cats = isActive == null
                ? itSkillCategoryRepository.findAllByOrderByLevelAscSortOrderAsc()
                : isActive ? itSkillCategoryRepository.findByActiveTrueOrderBySortOrderAsc()
                           : itSkillCategoryRepository.findByActiveTrueOrderBySortOrderAsc();
        return cats.stream().map(ItSkillCategoryDto::from).toList();
    }

    /**
     * ITスキル分類を新規作成する（ADMIN のみ）。
     * parentId が null の場合は大分類（level=1）として作成する。3階層超はエラー。
     */
    @PostMapping("/it-skill-categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItSkillCategoryDto> createItSkillCategory(@Valid @RequestBody ItSkillCategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ItSkillCategoryDto.from(
                masterService.createItSkillCategory(req.parentId(), req.name(), req.sortOrder())));
    }

    /** ITスキル分類を更新する（ADMIN のみ）。親変更・階層変更は不可（名前・表示順・有効フラグのみ更新）。 */
    @PutMapping("/it-skill-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ItSkillCategoryDto updateItSkillCategory(@PathVariable int id,
                                                     @Valid @RequestBody ItSkillCategoryUpdateRequest req) {
        return ItSkillCategoryDto.from(
                masterService.updateItSkillCategory(id, req.name(), req.sortOrder(), req.active()));
    }

    /** ADセミナー分類一覧を返す。isActive フィルタリング可能。 */
    @GetMapping("/ad-seminar-categories")
    public List<AdSeminarCategoryDto> getAdSeminarCategories(@RequestParam(required = false) Boolean isActive) {
        List<AdSeminarCategory> cats = isActive == null
                ? adSeminarCategoryRepository.findAllByOrderBySortOrderAsc()
                : adSeminarCategoryRepository.findByActiveTrueOrderBySortOrderAsc();
        return cats.stream().map(AdSeminarCategoryDto::from).toList();
    }

    /** ADセミナー分類を新規作成する（ADMIN のみ）。 */
    @PostMapping("/ad-seminar-categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdSeminarCategoryDto> createAdSeminarCategory(@Valid @RequestBody SimpleCategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(AdSeminarCategoryDto.from(
                masterService.createAdSeminarCategory(req.name(), req.sortOrder())));
    }

    /** ADセミナー分類を更新する（ADMIN のみ）。 */
    @PutMapping("/ad-seminar-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AdSeminarCategoryDto updateAdSeminarCategory(@PathVariable int id,
                                                         @Valid @RequestBody SimpleCategoryRequest req) {
        return AdSeminarCategoryDto.from(
                masterService.updateAdSeminarCategory(id, req.name(), req.sortOrder(), req.active()));
    }

    /**
     * ITスキルの大分類（level=1）を解決するヘルパー。
     * スキルに紐づくカテゴリが level=1 ならそのまま返す。
     * level=2 の場合は親（level=1）を、level=3 の場合は親の親（level=1）を DB から取得する。
     * フロントエンドでのグループ化・ソートに大分類IDが必要なため使用する。
     */
    private ItSkillCategory resolveCategory1(ItSkill skill) {
        ItSkillCategory cat = skill.getCategory();
        if (cat.getLevel() == 1) return cat;
        // level=2 or level=3 の場合、parentId を辿って大分類（level=1）を取得する
        return itSkillCategoryRepository.findById(cat.getParentId())
                .map(parent -> parent.getLevel() == 1 ? parent
                        // 親がまだ level=2 の場合（level=3 のとき）、さらに上の親を取得する
                        : itSkillCategoryRepository.findById(parent.getParentId()).orElse(parent))
                .orElse(cat);
    }

}
