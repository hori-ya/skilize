package com.skilize.master.application;

import com.skilize.inventory.domain.repository.ItSkillDetailRepository;
import com.skilize.inventory.domain.repository.QualificationDetailRepository;
import com.skilize.master.domain.model.*;
import com.skilize.master.domain.repository.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MasterService の単体テスト。isActive による絞り込み分岐・部分更新（null維持）・
 * カテゴリ階層解決（親子関係・最大深度）・カスタムマスタ昇格を検証する。
 */
@ExtendWith(MockitoExtension.class)
class MasterServiceTest {

    @Mock SkillLevelRepository skillLevelRepository;
    @Mock ItSkillRepository itSkillRepository;
    @Mock ItSkillCategoryRepository itSkillCategoryRepository;
    @Mock QualificationRepository qualificationRepository;
    @Mock QualificationCategoryRepository qualificationCategoryRepository;
    @Mock AdSeminarRepository adSeminarRepository;
    @Mock AdSeminarCategoryRepository adSeminarCategoryRepository;
    @Mock SeminarCategoryRepository seminarCategoryRepository;
    @Mock ItSkillDetailRepository itSkillDetailRepository;
    @Mock QualificationDetailRepository qualificationDetailRepository;

    @InjectMocks MasterService masterService;

    @Nested
    class SkillLevels {

        @Test
        void getSkillLevels_isActiveなし_全件を返す() {
            when(skillLevelRepository.findAllByOrderByLevelValueAsc()).thenReturn(List.of());
            masterService.getSkillLevels(null);
            verify(skillLevelRepository).findAllByOrderByLevelValueAsc();
        }

        @Test
        void getSkillLevels_isActive指定_絞り込みメソッドを呼ぶ() {
            when(skillLevelRepository.findByActiveOrderByLevelValueAsc(true)).thenReturn(List.of());
            masterService.getSkillLevels(true);
            verify(skillLevelRepository).findByActiveOrderByLevelValueAsc(true);
        }

        @Test
        void createSkillLevel_正常系_新規作成される() {
            SkillLevel saved = SkillLevel.create((short) 3, "上級", 30);
            ReflectionTestUtils.setField(saved, "id", 1);
            when(skillLevelRepository.save(any(SkillLevel.class))).thenReturn(saved);

            SkillLevel result = masterService.createSkillLevel((short) 3, "上級", 30);

            assertThat(result.getId()).isEqualTo(1);
        }

        @Test
        void updateSkillLevel_正常系_activeがnull_現在の値を維持する() {
            SkillLevel level = SkillLevel.create((short) 3, "上級", 30);
            when(skillLevelRepository.findById(1)).thenReturn(Optional.of(level));
            when(skillLevelRepository.save(level)).thenReturn(level);

            SkillLevel result = masterService.updateSkillLevel(1, (short) 4, "改", null, 40);

            assertThat(result.isActive()).isTrue();
            assertThat(result.getDescription()).isEqualTo("改");
        }

        @Test
        void updateSkillLevel_異常系_不在_404をスロー() {
            when(skillLevelRepository.findById(99)).thenReturn(Optional.empty());
            try {
                masterService.updateSkillLevel(99, (short) 1, "x", true, 10);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(404);
            }
        }
    }

    @Nested
    class ItSkills {

        @Test
        void getItSkills_isActiveなし_階層順を返す() {
            when(itSkillRepository.findAllOrderByHierarchy()).thenReturn(List.of());
            masterService.getItSkills(null);
            verify(itSkillRepository).findAllOrderByHierarchy();
        }

        @Test
        void getItSkills_isActiveTrue_有効のみを返す() {
            when(itSkillRepository.findAllActiveWithCategory()).thenReturn(List.of());
            masterService.getItSkills(true);
            verify(itSkillRepository).findAllActiveWithCategory();
        }

        @Test
        void getItSkills_isActiveFalse_無効のみを返す() {
            when(itSkillRepository.findByActiveFalseOrderByHierarchy()).thenReturn(List.of());
            masterService.getItSkills(false);
            verify(itSkillRepository).findByActiveFalseOrderByHierarchy();
        }

        @Test
        void createItSkill_正常系_分類解決して作成する() {
            ItSkillCategory cat = ItSkillCategory.create(null, (short) 2, "言語", 1);
            ReflectionTestUtils.setField(cat, "id", 10);
            when(itSkillCategoryRepository.findById(10)).thenReturn(Optional.of(cat));
            ItSkill saved = ItSkill.create(cat, "Java", null, 0);
            ReflectionTestUtils.setField(saved, "id", 100);
            when(itSkillRepository.save(any(ItSkill.class))).thenReturn(saved);

            ItSkill result = masterService.createItSkill(10, "Java", null, null);

            assertThat(result.getId()).isEqualTo(100);
        }

        @Test
        void createItSkill_異常系_分類不在_400をスロー() {
            when(itSkillCategoryRepository.findById(99)).thenReturn(Optional.empty());
            try {
                masterService.createItSkill(99, "Java", null, null);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(400);
            }
        }

        @Test
        void updateItSkill_異常系_スキル不在_404をスロー() {
            when(itSkillRepository.findById(99)).thenReturn(Optional.empty());
            try {
                masterService.updateItSkill(99, 1, "名前", null, null, null);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(404);
            }
        }
    }

    @Nested
    class ItSkillCategories {

        @Test
        void createItSkillCategory_正常系_親なし_レベル1で作成される() {
            ItSkillCategory saved = ItSkillCategory.create(null, (short) 1, "大分類", 0);
            ReflectionTestUtils.setField(saved, "id", 1);
            when(itSkillCategoryRepository.save(any(ItSkillCategory.class))).thenReturn(saved);

            ItSkillCategory result = masterService.createItSkillCategory(null, "大分類", null);

            assertThat(result.getLevel()).isEqualTo((short) 1);
        }

        @Test
        void createItSkillCategory_正常系_親あり_親レベル1の子はレベル2になる() {
            ItSkillCategory parent = ItSkillCategory.create(null, (short) 1, "大分類", 0);
            ReflectionTestUtils.setField(parent, "id", 1);
            when(itSkillCategoryRepository.findById(1)).thenReturn(Optional.of(parent));
            ItSkillCategory saved = ItSkillCategory.create(1, (short) 2, "中分類", 0);
            when(itSkillCategoryRepository.save(any(ItSkillCategory.class))).thenReturn(saved);

            ItSkillCategory result = masterService.createItSkillCategory(1, "中分類", null);

            assertThat(result.getLevel()).isEqualTo((short) 2);
        }

        @Test
        void createItSkillCategory_異常系_親不在_400をスロー() {
            when(itSkillCategoryRepository.findById(99)).thenReturn(Optional.empty());
            try {
                masterService.createItSkillCategory(99, "中分類", null);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(400);
                assertThat(e.getBody().getDetail()).isEqualTo("PARENT_CATEGORY_NOT_FOUND");
            }
        }

        @Test
        void createItSkillCategory_異常系_最大深度超過_400をスロー() {
            ItSkillCategory level3Parent = ItSkillCategory.create(1, (short) 3, "小分類", 0);
            ReflectionTestUtils.setField(level3Parent, "id", 3);
            when(itSkillCategoryRepository.findById(3)).thenReturn(Optional.of(level3Parent));

            try {
                masterService.createItSkillCategory(3, "第4階層", null);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getBody().getDetail()).isEqualTo("CATEGORY_MAX_DEPTH_EXCEEDED");
            }
        }

        @Test
        void updateItSkillCategory_異常系_不在_404をスロー() {
            when(itSkillCategoryRepository.findById(99)).thenReturn(Optional.empty());
            try {
                masterService.updateItSkillCategory(99, "名前", null, null);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(404);
            }
        }
    }

    @Nested
    class Qualifications {

        @Test
        void getQualifications_isActiveなし_全件を返す() {
            when(qualificationRepository.findAllWithCategory()).thenReturn(List.of());
            masterService.getQualifications(null);
            verify(qualificationRepository).findAllWithCategory();
        }

        @Test
        void createQualification_正常系_カテゴリなしで作成できる() {
            Qualification saved = Qualification.create(null, "基本情報", null, 0);
            when(qualificationRepository.save(any(Qualification.class))).thenReturn(saved);

            Qualification result = masterService.createQualification(null, "基本情報", null, null);

            assertThat(result.getCategory()).isNull();
        }

        @Test
        void createQualification_異常系_カテゴリ不在_400をスロー() {
            when(qualificationCategoryRepository.findById(99)).thenReturn(Optional.empty());
            try {
                masterService.createQualification(99, "基本情報", null, null);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(400);
            }
        }

        @Test
        void updateQualification_異常系_不在_404をスロー() {
            when(qualificationRepository.findById(99)).thenReturn(Optional.empty());
            try {
                masterService.updateQualification(99, null, "名前", null, null, null);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(404);
            }
        }
    }

    @Nested
    class QualificationCategories {

        @Test
        void createQualificationCategory_正常系_作成される() {
            QualificationCategory saved = QualificationCategory.create("分類A", 0);
            when(qualificationCategoryRepository.save(any(QualificationCategory.class))).thenReturn(saved);

            QualificationCategory result = masterService.createQualificationCategory("分類A", null);

            assertThat(result.getName()).isEqualTo("分類A");
        }

        @Test
        void updateQualificationCategory_異常系_不在_404をスロー() {
            when(qualificationCategoryRepository.findById(99)).thenReturn(Optional.empty());
            try {
                masterService.updateQualificationCategory(99, "名前", null, null);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(404);
            }
        }
    }

    @Nested
    class AdSeminars {

        @Test
        void getAdSeminars_isActiveFalse_無効のみを返す() {
            when(adSeminarRepository.findAllWithCategoryByActive(false)).thenReturn(List.of());
            masterService.getAdSeminars(false);
            verify(adSeminarRepository).findAllWithCategoryByActive(false);
        }

        @Test
        void createAdSeminar_異常系_カテゴリ不在_400をスロー() {
            when(adSeminarCategoryRepository.findById(99)).thenReturn(Optional.empty());
            try {
                masterService.createAdSeminar(99, "AWS研修", null, null);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(400);
            }
        }

        @Test
        void updateAdSeminar_異常系_不在_404をスロー() {
            when(adSeminarRepository.findById(99)).thenReturn(Optional.empty());
            try {
                masterService.updateAdSeminar(99, null, "名前", null, null, null);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(404);
            }
        }
    }

    @Nested
    class AdSeminarCategories {

        @Test
        void createAdSeminarCategory_正常系_作成される() {
            AdSeminarCategory saved = AdSeminarCategory.create("研修", 0);
            when(adSeminarCategoryRepository.save(any(AdSeminarCategory.class))).thenReturn(saved);

            AdSeminarCategory result = masterService.createAdSeminarCategory("研修", null);

            assertThat(result.getName()).isEqualTo("研修");
        }

        @Test
        void updateAdSeminarCategory_異常系_不在_404をスロー() {
            when(adSeminarCategoryRepository.findById(99)).thenReturn(Optional.empty());
            try {
                masterService.updateAdSeminarCategory(99, "名前", null, null);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(404);
            }
        }
    }

    @Nested
    class SeminarCategoriesAndItSkillCategoryList {

        @Test
        void getSeminarCategories_正常系_有効な分類一覧を返す() {
            when(seminarCategoryRepository.findByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of());
            masterService.getSeminarCategories();
            verify(seminarCategoryRepository).findByActiveTrueOrderBySortOrderAsc();
        }

        @Test
        void getItSkillCategories_isActiveFalse_無効のみを返す() {
            when(itSkillCategoryRepository.findByActiveFalseOrderByLevelAscParentIdAscSortOrderAsc()).thenReturn(List.of());
            masterService.getItSkillCategories(false);
            verify(itSkillCategoryRepository).findByActiveFalseOrderByLevelAscParentIdAscSortOrderAsc();
        }

        @Test
        void findItSkillCategoryById_正常系_取得できる() {
            ItSkillCategory cat = ItSkillCategory.create(null, (short) 1, "大分類", 0);
            when(itSkillCategoryRepository.findById(1)).thenReturn(Optional.of(cat));

            Optional<ItSkillCategory> result = masterService.findItSkillCategoryById(1);

            assertThat(result).contains(cat);
        }
    }

    @Nested
    class PromoteCustom {

        @Test
        void getCustomUnregisteredItSkills_正常系_一覧を返す() {
            when(itSkillDetailRepository.findCustomUnregisteredSkillNames()).thenReturn(List.of());
            masterService.getCustomUnregisteredItSkills();
            verify(itSkillDetailRepository).findCustomUnregisteredSkillNames();
        }

        @Test
        void promoteItSkill_正常系_新規マスタ登録し明細を紐付ける() {
            ItSkillCategory cat = ItSkillCategory.create(null, (short) 1, "大分類", 0);
            ReflectionTestUtils.setField(cat, "id", 1);
            when(itSkillCategoryRepository.findById(1)).thenReturn(Optional.of(cat));
            ItSkill saved = ItSkill.create(cat, "自作フレームワーク", null, 0);
            ReflectionTestUtils.setField(saved, "id", 200);
            when(itSkillRepository.save(any(ItSkill.class))).thenReturn(saved);

            ItSkill result = masterService.promoteItSkill("自作フレームワーク", 1, "自作フレームワーク", null, null);

            assertThat(result.getId()).isEqualTo(200);
            verify(itSkillDetailRepository).linkToMasterSkill("自作フレームワーク", saved);
        }

        @Test
        void getCustomUnregisteredQualifications_正常系_一覧を返す() {
            when(qualificationDetailRepository.findCustomUnregisteredQualificationNames()).thenReturn(List.of());
            masterService.getCustomUnregisteredQualifications();
            verify(qualificationDetailRepository).findCustomUnregisteredQualificationNames();
        }

        @Test
        void promoteQualification_正常系_新規マスタ登録し明細を紐付ける() {
            Qualification saved = Qualification.create(null, "自作資格", null, 0);
            ReflectionTestUtils.setField(saved, "id", 300);
            when(qualificationRepository.save(any(Qualification.class))).thenReturn(saved);

            Qualification result = masterService.promoteQualification("自作資格", null, "自作資格", null, null);

            assertThat(result.getId()).isEqualTo(300);
            verify(qualificationDetailRepository).linkToMasterQualification("自作資格", saved);
        }
    }
}
