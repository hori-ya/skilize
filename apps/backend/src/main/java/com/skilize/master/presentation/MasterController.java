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

/**
 * マスタデータ（スキルレベル・ITスキル・資格・ADセミナーとその各分類）の REST API コントローラー。
 * 参照は全ロール可。作成・更新は ADMIN のみ（カスタム名昇格は TL/ADMIN も可）。
 * isActive パラメータ: null → 全件、true → 有効のみ、false → 無効のみ（一部エンドポイントのみ対応）。
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

    /** スキルレベル一覧を levelValue 昇順で返す。isActive で有効/無効を絞り込み可。 */
    @GetMapping("/skill-levels")
    public List<SkillLevelResponse> getSkillLevels(@RequestParam(required = false) Boolean isActive) {
        List<SkillLevel> levels = isActive != null
                ? skillLevelRepository.findByActiveOrderByLevelValueAsc(isActive)
                : skillLevelRepository.findAllByOrderByLevelValueAsc();
        return levels.stream().map(SkillLevelResponse::from).toList();
    }

    /** スキルレベルを新規作成する（ADMIN のみ）。 */
    @PostMapping("/skill-levels")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SkillLevelResponse> createSkillLevel(@Valid @RequestBody SkillLevelRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SkillLevelResponse.from(masterService.createSkillLevel(req.levelValue(), req.description(), req.scoreWeight())));
    }

    /** スキルレベルを更新する（ADMIN のみ）。 */
    @PutMapping("/skill-levels/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SkillLevelResponse updateSkillLevel(@PathVariable int id, @Valid @RequestBody SkillLevelRequest req) {
        return SkillLevelResponse.from(masterService.updateSkillLevel(id, req.levelValue(), req.description(), req.active(), req.scoreWeight()));
    }

    /**
     * ITスキル一覧を返す。isActive で有効/無効を絞り込み可。
     * 全件・無効のみは分類1→分類2→分類3→並順でソート。有効のみは棚卸入力画面向けのため従来順を維持。
     */
    @GetMapping("/it-skills")
    public List<ItSkillResponse> getItSkills(@RequestParam(required = false) Boolean isActive) {
        List<ItSkill> skills = isActive == null
                ? itSkillRepository.findAllOrderByHierarchy()
                : isActive ? itSkillRepository.findAllActiveWithCategory()
                           : itSkillRepository.findByActiveFalseOrderByHierarchy();
        return skills.stream().map(this::toItSkillResponse).toList();
    }

    /** ITスキルを新規作成する（ADMIN のみ）。resolveCategory1() で表示用の大分類を解決する。 */
    @PostMapping("/it-skills")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItSkillResponse> createItSkill(@Valid @RequestBody ItSkillRequest req) {
        ItSkill saved = masterService.createItSkill(req.categoryId(), req.name(), req.description(), req.sortOrder());
        return ResponseEntity.status(HttpStatus.CREATED).body(toItSkillResponse(saved));
    }

    /** ITスキルを更新する（ADMIN のみ）。 */
    @PutMapping("/it-skills/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ItSkillResponse updateItSkill(@PathVariable int id, @Valid @RequestBody ItSkillRequest req) {
        ItSkill skill = masterService.updateItSkill(id, req.categoryId(), req.name(),
                req.description(), req.sortOrder(), req.active());
        return toItSkillResponse(skill);
    }

    /** カスタム入力されたが未マスタ登録のITスキル名を使用件数付きで返す（TL/ADMIN のみ）。 */
    @GetMapping("/it-skills/custom-unregistered")
    @PreAuthorize("hasAnyRole('TL', 'ADMIN')")
    public List<CustomUnregisteredResponse> getCustomUnregisteredItSkills() {
        return masterService.getCustomUnregisteredItSkills().stream()
                .map(row -> new CustomUnregisteredResponse((String) row[0], (long) row[1]))
                .toList();
    }

    /** カスタムITスキルをマスタに昇格し、同名の棚卸明細をマスタへ紐付ける（TL/ADMIN のみ）。 */
    @PostMapping("/it-skills/promote")
    @PreAuthorize("hasAnyRole('TL', 'ADMIN')")
    public ResponseEntity<ItSkillResponse> promoteItSkill(@Valid @RequestBody PromoteItSkillRequest req) {
        ItSkill skill = masterService.promoteItSkill(req.customName(), req.categoryId(), req.name(),
                req.description(), req.sortOrder());
        return ResponseEntity.status(HttpStatus.CREATED).body(toItSkillResponse(skill));
    }

    /** 資格一覧を返す。isActive で有効/無効を絞り込み可。全件・無効のみは分類→並順でソート。有効のみは棚卸入力画面向けのため従来順を維持。 */
    @GetMapping("/qualifications")
    public List<QualificationResponse> getQualifications(@RequestParam(required = false) Boolean isActive) {
        List<Qualification> list = isActive == null
                ? qualificationRepository.findAllWithCategory()
                : isActive ? qualificationRepository.findAllActiveWithCategory()
                           : qualificationRepository.findAllWithCategoryByActive(false);
        return list.stream().map(QualificationResponse::from).toList();
    }

    /** 資格を新規作成する（ADMIN のみ）。 */
    @PostMapping("/qualifications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QualificationResponse> createQualification(@Valid @RequestBody QualificationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(QualificationResponse.from(
                masterService.createQualification(req.categoryId(), req.name(), req.description(), req.sortOrder())));
    }

    /** 資格を更新する（ADMIN のみ）。 */
    @PutMapping("/qualifications/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public QualificationResponse updateQualification(@PathVariable int id, @Valid @RequestBody QualificationRequest req) {
        return QualificationResponse.from(masterService.updateQualification(id, req.categoryId(), req.name(),
                req.description(), req.sortOrder(), req.active()));
    }

    /** カスタム入力されたが未マスタ登録の資格名を使用件数付きで返す（TL/ADMIN のみ）。 */
    @GetMapping("/qualifications/custom-unregistered")
    @PreAuthorize("hasAnyRole('TL', 'ADMIN')")
    public List<CustomUnregisteredResponse> getCustomUnregisteredQualifications() {
        return masterService.getCustomUnregisteredQualifications().stream()
                .map(row -> new CustomUnregisteredResponse((String) row[0], (long) row[1]))
                .toList();
    }

    /** カスタム資格をマスタに昇格し、同名の棚卸明細をマスタへ紐付ける（TL/ADMIN のみ）。 */
    @PostMapping("/qualifications/promote")
    @PreAuthorize("hasAnyRole('TL', 'ADMIN')")
    public ResponseEntity<QualificationResponse> promoteQualification(@Valid @RequestBody PromoteQualificationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(QualificationResponse.from(
                masterService.promoteQualification(req.customName(), req.categoryId(), req.name(),
                        req.description(), req.sortOrder())));
    }

    /** 資格分類一覧を sortOrder 昇順で返す。isActive で有効/無効を絞り込み可。 */
    @GetMapping("/qualification-categories")
    public List<QualificationCategoryResponse> getQualificationCategories(@RequestParam(required = false) Boolean isActive) {
        List<QualificationCategory> cats = isActive == null
                ? qualificationCategoryRepository.findAllByOrderBySortOrderAsc()
                : qualificationCategoryRepository.findByActiveTrueOrderBySortOrderAsc();
        return cats.stream().map(QualificationCategoryResponse::from).toList();
    }

    /** 資格分類を新規作成する（ADMIN のみ）。 */
    @PostMapping("/qualification-categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QualificationCategoryResponse> createQualificationCategory(
            @Valid @RequestBody SimpleCategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(QualificationCategoryResponse.from(
                masterService.createQualificationCategory(req.name(), req.sortOrder())));
    }

    /** 資格分類を更新する（ADMIN のみ）。 */
    @PutMapping("/qualification-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public QualificationCategoryResponse updateQualificationCategory(@PathVariable int id,
                                                                      @Valid @RequestBody SimpleCategoryRequest req) {
        return QualificationCategoryResponse.from(
                masterService.updateQualificationCategory(id, req.name(), req.sortOrder(), req.active()));
    }

    /** ADセミナー一覧を返す。isActive で有効/無効を絞り込み可。全件・無効のみは分類→並順でソート。有効のみは棚卸入力画面向けのため従来順を維持。 */
    @GetMapping("/ad-seminars")
    public List<AdSeminarResponse> getAdSeminars(@RequestParam(required = false) Boolean isActive) {
        List<AdSeminar> list = isActive == null
                ? adSeminarRepository.findAllWithCategory()
                : isActive ? adSeminarRepository.findAllActiveWithCategory()
                           : adSeminarRepository.findAllWithCategoryByActive(false);
        return list.stream().map(AdSeminarResponse::from).toList();
    }

    /** ADセミナーを新規作成する（ADMIN のみ）。 */
    @PostMapping("/ad-seminars")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdSeminarResponse> createAdSeminar(@Valid @RequestBody AdSeminarRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(AdSeminarResponse.from(
                masterService.createAdSeminar(req.categoryId(), req.name(), req.description(), req.sortOrder())));
    }

    /** ADセミナーを更新する（ADMIN のみ）。 */
    @PutMapping("/ad-seminars/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AdSeminarResponse updateAdSeminar(@PathVariable int id, @Valid @RequestBody AdSeminarRequest req) {
        return AdSeminarResponse.from(masterService.updateAdSeminar(id, req.categoryId(), req.name(),
                req.description(), req.sortOrder(), req.active()));
    }

    /** 有効なセミナー分類一覧を sortOrder 昇順で返す（isActive フィルタなし・常に有効のみ）。 */
    @GetMapping("/seminar-categories")
    public List<SeminarCategoryResponse> getSeminarCategories() {
        return seminarCategoryRepository.findByActiveTrueOrderBySortOrderAsc()
                .stream().map(SeminarCategoryResponse::from).toList();
    }

    /**
     * ITスキル分類一覧を返す。isActive=null で全件、true で有効のみ、false で無効のみ。
     * 全件・無効のみは階層レベル昇順→親分類ID昇順→表示順昇順でソートする。
     * 有効のみは棚卸入力画面向けのため表示順昇順のみ。
     */
    @GetMapping("/it-skill-categories")
    public List<ItSkillCategoryResponse> getItSkillCategories(@RequestParam(required = false) Boolean isActive) {
        List<ItSkillCategory> cats = isActive == null
                ? itSkillCategoryRepository.findAllByOrderByLevelAscParentIdAscSortOrderAsc()
                : isActive ? itSkillCategoryRepository.findByActiveTrueOrderBySortOrderAsc()
                           : itSkillCategoryRepository.findByActiveFalseOrderByLevelAscParentIdAscSortOrderAsc();
        return cats.stream().map(ItSkillCategoryResponse::from).toList();
    }

    /** ITスキル分類を新規作成する（ADMIN のみ）。 */
    @PostMapping("/it-skill-categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItSkillCategoryResponse> createItSkillCategory(@Valid @RequestBody ItSkillCategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ItSkillCategoryResponse.from(
                masterService.createItSkillCategory(req.parentId(), req.name(), req.sortOrder())));
    }

    /** ITスキル分類を更新する（ADMIN のみ）。 */
    @PutMapping("/it-skill-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ItSkillCategoryResponse updateItSkillCategory(@PathVariable int id,
                                                          @Valid @RequestBody ItSkillCategoryUpdateRequest req) {
        return ItSkillCategoryResponse.from(
                masterService.updateItSkillCategory(id, req.name(), req.sortOrder(), req.active()));
    }

    /** ADセミナー分類一覧を sortOrder 昇順で返す。isActive で有効/無効を絞り込み可。 */
    @GetMapping("/ad-seminar-categories")
    public List<AdSeminarCategoryResponse> getAdSeminarCategories(@RequestParam(required = false) Boolean isActive) {
        List<AdSeminarCategory> cats = isActive == null
                ? adSeminarCategoryRepository.findAllByOrderBySortOrderAsc()
                : adSeminarCategoryRepository.findByActiveTrueOrderBySortOrderAsc();
        return cats.stream().map(AdSeminarCategoryResponse::from).toList();
    }

    /** ADセミナー分類を新規作成する（ADMIN のみ）。 */
    @PostMapping("/ad-seminar-categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdSeminarCategoryResponse> createAdSeminarCategory(@Valid @RequestBody SimpleCategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(AdSeminarCategoryResponse.from(
                masterService.createAdSeminarCategory(req.name(), req.sortOrder())));
    }

    /** ADセミナー分類を更新する（ADMIN のみ）。 */
    @PutMapping("/ad-seminar-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AdSeminarCategoryResponse updateAdSeminarCategory(@PathVariable int id,
                                                              @Valid @RequestBody SimpleCategoryRequest req) {
        return AdSeminarCategoryResponse.from(
                masterService.updateAdSeminarCategory(id, req.name(), req.sortOrder(), req.active()));
    }

    /**
     * ITスキルの表示用大分類（レベル1）を返すプライベートヘルパー。
     * ITスキル分類は最大3階層（レベル1=大分類、レベル2、レベル3）。
     * レベル1 → そのまま返す / レベル2 → 親（レベル1）を返す / レベル3 → 親の親（レベル1）を返す。
     * 上位分類に到達できない場合は直接の分類をそのまま返す（フォールバック）。
     */
    private ItSkillCategory resolveCategory1(ItSkill skill) {
        ItSkillCategory cat = skill.getCategory();
        if (cat.getLevel() == 1) return cat;
        return itSkillCategoryRepository.findById(cat.getParentId())
                .map(parent -> parent.getLevel() == 1 ? parent
                        : itSkillCategoryRepository.findById(parent.getParentId()).orElse(parent))
                .orElse(cat);
    }

    /**
     * ITスキルの表示用中分類（レベル2）を返すプライベートヘルパー。
     * レベル3のスキルのみ親（レベル2）を取得して返す。それ以外は null を返す。
     * （レベル2の場合は from() 内で cat 自身を cat2 として扱うため不要）
     */
    private ItSkillCategory resolveCategory2(ItSkill skill) {
        ItSkillCategory cat = skill.getCategory();
        if (cat.getLevel() != 3) return null;
        return itSkillCategoryRepository.findById(cat.getParentId()).orElse(null);
    }

    private ItSkillResponse toItSkillResponse(ItSkill skill) {
        return ItSkillResponse.from(skill, resolveCategory1(skill), resolveCategory2(skill));
    }
}
