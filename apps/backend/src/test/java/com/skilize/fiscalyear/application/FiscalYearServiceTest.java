package com.skilize.fiscalyear.application;

import com.skilize.fiscalyear.domain.model.FiscalYear;
import com.skilize.fiscalyear.domain.model.FiscalYearSettings;
import com.skilize.fiscalyear.domain.repository.FiscalYearRepository;
import com.skilize.fiscalyear.domain.repository.FiscalYearSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.when;

/**
 * FiscalYearService の単体テスト。ソート順・部分更新（active維持）・年度設定の存在チェックを検証する。
 */
@ExtendWith(MockitoExtension.class)
class FiscalYearServiceTest {

    @Mock FiscalYearRepository fiscalYearRepository;
    @Mock FiscalYearSettingsRepository settingsRepository;

    @InjectMocks FiscalYearService fiscalYearService;

    private FiscalYear fy2024;
    private FiscalYear fy2025;

    @BeforeEach
    void setUp() {
        fy2024 = FiscalYear.create("2024年度", LocalDate.of(2024, 4, 1), LocalDate.of(2025, 3, 31), null, null);
        ReflectionTestUtils.setField(fy2024, "id", 1);
        fy2025 = FiscalYear.create("2025年度", LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31), null, null);
        ReflectionTestUtils.setField(fy2025, "id", 2);
    }

    @Nested
    class FindAllOrderByStartDateDesc {

        @Test
        void 正常系_開始日降順にソートして返す() {
            when(fiscalYearRepository.findAll()).thenReturn(List.of(fy2024, fy2025));

            List<FiscalYear> result = fiscalYearService.findAllOrderByStartDateDesc();

            assertThat(result).containsExactly(fy2025, fy2024);
        }
    }

    @Nested
    class FindCurrent {

        @Test
        void 正常系_現在有効な年度を返す() {
            when(fiscalYearRepository.findCurrent(any(LocalDate.class))).thenReturn(Optional.of(fy2025));

            Optional<FiscalYear> result = fiscalYearService.findCurrent(LocalDate.of(2025, 6, 1));

            assertThat(result).contains(fy2025);
        }
    }

    @Nested
    class GetSettings {

        @Test
        void 正常系_設定を返す() {
            FiscalYearSettings settings = FiscalYearSettings.reconstruct((short) 1, (short) 4, null);
            when(settingsRepository.findById((short) 1)).thenReturn(Optional.of(settings));

            FiscalYearSettings result = fiscalYearService.getSettings();

            assertThat(result.getFiscalYearStartMonth()).isEqualTo((short) 4);
        }

        @Test
        void 異常系_設定未初期化_404をスロー() {
            when(settingsRepository.findById((short) 1)).thenReturn(Optional.empty());

            try {
                fiscalYearService.getSettings();
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(404);
            }
        }
    }

    @Nested
    class CreateFiscalYear {

        @Test
        void 正常系_年度を新規作成する() {
            when(fiscalYearRepository.save(any(FiscalYear.class))).thenAnswer(
                    new org.mockito.stubbing.Answer<FiscalYear>() {
                        @Override
                        public FiscalYear answer(org.mockito.invocation.InvocationOnMock invocation) {
                            return invocation.getArgument(0);
                        }
                    });

            FiscalYear result = fiscalYearService.createFiscalYear("2026年度",
                    LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31), null, null);

            assertThat(result.getName()).isEqualTo("2026年度");
        }
    }

    @Nested
    class UpdateFiscalYear {

        @Test
        void 正常系_activeを指定_指定した値で更新される() {
            when(fiscalYearRepository.findById(1)).thenReturn(Optional.of(fy2024));
            when(fiscalYearRepository.save(fy2024)).thenReturn(fy2024);

            FiscalYear result = fiscalYearService.updateFiscalYear(1, "2024年度改",
                    LocalDate.of(2024, 4, 1), LocalDate.of(2025, 3, 31), null, null, false);

            assertThat(result.getName()).isEqualTo("2024年度改");
            assertThat(result.isActive()).isFalse();
        }

        @Test
        void 正常系_activeがnull_現在の値を維持する() {
            when(fiscalYearRepository.findById(1)).thenReturn(Optional.of(fy2024));
            when(fiscalYearRepository.save(fy2024)).thenReturn(fy2024);

            FiscalYear result = fiscalYearService.updateFiscalYear(1, "2024年度改",
                    LocalDate.of(2024, 4, 1), LocalDate.of(2025, 3, 31), null, null, null);

            assertThat(result.isActive()).isTrue();
        }

        @Test
        void 異常系_対象年度不在_404をスロー() {
            when(fiscalYearRepository.findById(99)).thenReturn(Optional.empty());

            try {
                fiscalYearService.updateFiscalYear(99, "名前",
                        LocalDate.of(2024, 4, 1), LocalDate.of(2025, 3, 31), null, null, true);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(404);
            }
        }
    }

    @Nested
    class UpdateSettings {

        @Test
        void 正常系_年度開始月を更新する() {
            FiscalYearSettings settings = FiscalYearSettings.reconstruct((short) 1, (short) 4, null);
            when(settingsRepository.findById((short) 1)).thenReturn(Optional.of(settings));
            when(settingsRepository.save(settings)).thenReturn(settings);

            FiscalYearSettings result = fiscalYearService.updateSettings((short) 7);

            assertThat(result.getFiscalYearStartMonth()).isEqualTo((short) 7);
        }

        @Test
        void 異常系_設定未初期化_404をスロー() {
            when(settingsRepository.findById((short) 1)).thenReturn(Optional.empty());

            try {
                fiscalYearService.updateSettings((short) 7);
                fail("ResponseStatusException が発生する想定");
            } catch (ResponseStatusException e) {
                assertThat(e.getStatusCode().value()).isEqualTo(404);
            }
        }
    }
}
