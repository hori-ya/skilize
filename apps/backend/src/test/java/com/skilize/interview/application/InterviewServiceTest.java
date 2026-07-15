package com.skilize.interview.application;

import com.skilize.interview.application.command.DetailNoteCommand;
import com.skilize.interview.domain.model.DetailType;
import com.skilize.interview.domain.model.InterviewDetailNote;
import com.skilize.interview.domain.model.InventoryInterview;
import com.skilize.interview.domain.repository.InterviewDetailNoteRepository;
import com.skilize.interview.domain.repository.InventoryInterviewRepository;
import com.skilize.inventory.domain.model.Inventory;
import com.skilize.inventory.domain.repository.InventoryRepository;
import com.skilize.fiscalyear.domain.model.FiscalYear;
import com.skilize.user.domain.model.Role;
import com.skilize.user.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * InterviewService の単体テスト。TL/ADMIN限定アクセス・upsert・明細ノート全件洗い替え・前年度参照を検証する。
 */
@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock InventoryInterviewRepository inventoryInterviewRepository;
    @Mock InterviewDetailNoteRepository interviewDetailNoteRepository;
    @Mock InventoryRepository inventoryRepository;

    @InjectMocks InterviewService interviewService;

    private User tlUser;
    private User generalUser;
    private FiscalYear currentFy;
    private FiscalYear prevFy;
    private Inventory currentInv;

    @BeforeEach
    void setUp() {
        tlUser = User.create("tl01", "TL", null, Role.TL, null, "hash");
        ReflectionTestUtils.setField(tlUser, "id", 2);

        generalUser = User.create("user01", "一般ユーザー", null, Role.GENERAL, null, "hash");
        ReflectionTestUtils.setField(generalUser, "id", 4);

        currentFy = FiscalYear.create("2025年度", LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31), null, null);
        ReflectionTestUtils.setField(currentFy, "id", 2);
        prevFy = FiscalYear.create("2024年度", LocalDate.of(2024, 4, 1), LocalDate.of(2025, 3, 31), null, null);
        ReflectionTestUtils.setField(prevFy, "id", 1);

        currentInv = Inventory.create(generalUser, currentFy);
        ReflectionTestUtils.setField(currentInv, "id", 10);
    }

    @Nested
    class FindMine {

        @Test
        void 正常系_TL_自身の面談メモを取得できる() {
            InventoryInterview interview = InventoryInterview.create(10, tlUser, "メモ");
            when(inventoryInterviewRepository.findByInventoryIdAndInterviewerId(10, 2))
                    .thenReturn(Optional.of(interview));

            Optional<InventoryInterview> result = interviewService.findMine(10, tlUser);

            assertThat(result).contains(interview);
        }

        @Test
        void 異常系_GENERAL_403をスロー() {
            try {
                interviewService.findMine(10, generalUser);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(403);
            }
        }
    }

    @Nested
    class Save {

        @Test
        void 正常系_新規面談メモ_明細ノートを保存する() {
            when(inventoryRepository.findById(10)).thenReturn(Optional.of(currentInv));
            when(inventoryInterviewRepository.findByInventoryIdAndInterviewerId(10, 2)).thenReturn(Optional.empty());
            InventoryInterview saved = InventoryInterview.create(10, tlUser, "メモ");
            ReflectionTestUtils.setField(saved, "id", 100);
            when(inventoryInterviewRepository.save(any(InventoryInterview.class))).thenReturn(saved);

            List<DetailNoteCommand> commands = List.of(new DetailNoteCommand(DetailType.IT_SKILL, 1, "コメント"));
            InventoryInterview result = interviewService.save(10, tlUser, "メモ", commands);

            assertThat(result.getId()).isEqualTo(100);
            verify(interviewDetailNoteRepository).deleteByInterviewId(100);
            ArgumentCaptor<List<InterviewDetailNote>> captor = ArgumentCaptor.captor();
            verify(interviewDetailNoteRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
            assertThat(captor.getValue().get(0).getDetailType()).isEqualTo(DetailType.IT_SKILL);
        }

        @Test
        void 正常系_既存面談メモ_更新して保存する() {
            InventoryInterview existing = InventoryInterview.create(10, tlUser, "旧メモ");
            ReflectionTestUtils.setField(existing, "id", 100);

            when(inventoryRepository.findById(10)).thenReturn(Optional.of(currentInv));
            when(inventoryInterviewRepository.findByInventoryIdAndInterviewerId(10, 2)).thenReturn(Optional.of(existing));
            when(inventoryInterviewRepository.save(existing)).thenReturn(existing);

            InventoryInterview result = interviewService.save(10, tlUser, "新メモ", List.of());

            assertThat(result.getGeneralNote()).isEqualTo("新メモ");
        }

        @Test
        void 異常系_棚卸不在_404をスロー() {
            when(inventoryRepository.findById(99)).thenReturn(Optional.empty());

            try {
                interviewService.save(99, tlUser, "メモ", List.of());
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(404);
            }
        }

        @Test
        void 異常系_GENERAL_403をスロー() {
            try {
                interviewService.save(10, generalUser, "メモ", List.of());
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(403);
            }
        }
    }

    @Nested
    class FindPrevYear {

        @Test
        void 正常系_前年度棚卸あり_前年度の面談メモを返す() {
            Inventory prevInv = Inventory.create(generalUser, prevFy);
            ReflectionTestUtils.setField(prevInv, "id", 9);
            InventoryInterview prevInterview = InventoryInterview.create(9, tlUser, "前年度メモ");

            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(currentInv));
            when(inventoryRepository.findByUserIdWithFiscalYear(4)).thenReturn(List.of(currentInv, prevInv));
            when(inventoryInterviewRepository.findByInventoryIdAndInterviewerId(9, 2))
                    .thenReturn(Optional.of(prevInterview));

            Optional<InventoryInterview> result = interviewService.findPrevYear(10, tlUser);

            assertThat(result).contains(prevInterview);
        }

        @Test
        void 正常系_前年度棚卸なし_空を返す() {
            when(inventoryRepository.findByIdWithAssociations(10)).thenReturn(Optional.of(currentInv));
            when(inventoryRepository.findByUserIdWithFiscalYear(4)).thenReturn(List.of(currentInv));

            Optional<InventoryInterview> result = interviewService.findPrevYear(10, tlUser);

            assertThat(result).isEmpty();
        }

        @Test
        void 異常系_棚卸不在_404をスロー() {
            when(inventoryRepository.findByIdWithAssociations(99)).thenReturn(Optional.empty());

            try {
                interviewService.findPrevYear(99, tlUser);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(404);
            }
        }
    }

    @Nested
    class FindDetailNotes {

        @Test
        void 正常系_明細ノート一覧を返す() {
            InventoryInterview interview = InventoryInterview.create(10, tlUser, "メモ");
            ReflectionTestUtils.setField(interview, "id", 100);
            InterviewDetailNote note = InterviewDetailNote.create(interview, DetailType.QUALIFICATION, 5, "コメント");
            when(interviewDetailNoteRepository.findByInterviewId(100)).thenReturn(List.of(note));

            List<InterviewDetailNote> result = interviewService.findDetailNotes(100);

            assertThat(result).containsExactly(note);
        }
    }
}
