package com.skilize.ai.application;

import com.skilize.ai.application.query.AiAnalysisQueryResult;
import com.skilize.ai.domain.model.AiAnalysisStatus;
import com.skilize.ai.domain.model.AiCareerAnalysis;
import com.skilize.ai.domain.repository.AiCareerAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AiAnalysisService の単体テスト。JSON パース・ステータス別レスポンス整形・upsert（新規/リセット）分岐を検証する。
 * HTTP 呼び出し（callAiService）は fire-and-forget かつ内部で例外を握るため、実ネットワーク呼び出しの成否は検証しない。
 */
@ExtendWith(MockitoExtension.class)
class AiAnalysisServiceTest {

    @Mock AiCareerAnalysisRepository repository;

    @InjectMocks AiAnalysisService aiAnalysisService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiAnalysisService, "aiServiceUrl", "http://localhost:0");
        ReflectionTestUtils.setField(aiAnalysisService, "aiSecretKey", "test-secret");
    }

    @Nested
    class FindByUserId {

        @Test
        void 正常系_COMPLETED_analysisResultをパースして返す() {
            AiCareerAnalysis analysis = AiCareerAnalysis.reconstruct(1, 5, 2, AiAnalysisStatus.COMPLETED,
                    "{\"summary\":\"good\"}", null, OffsetDateTime.now(), OffsetDateTime.now());
            when(repository.findByUserIdOrderByFiscalYearIdDesc(5)).thenReturn(List.of(analysis));

            List<AiAnalysisQueryResult> result = aiAnalysisService.findByUserId(5);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).status()).isEqualTo("COMPLETED");
            assertThat(result.get(0).analysisResult()).isNotNull();
        }

        @Test
        void 正常系_FAILED_errorMessageを返しanalysisResultはnull() {
            AiCareerAnalysis analysis = AiCareerAnalysis.reconstruct(1, 5, 2, AiAnalysisStatus.FAILED,
                    null, "analysis timeout", OffsetDateTime.now(), OffsetDateTime.now());
            when(repository.findByUserIdOrderByFiscalYearIdDesc(5)).thenReturn(List.of(analysis));

            List<AiAnalysisQueryResult> result = aiAnalysisService.findByUserId(5);

            assertThat(result.get(0).analysisResult()).isNull();
            assertThat(result.get(0).errorMessage()).isEqualTo("analysis timeout");
        }

        @Test
        void 異常系_不正なJSON_パース失敗時はanalysisResultがnullになる() {
            AiCareerAnalysis analysis = AiCareerAnalysis.reconstruct(1, 5, 2, AiAnalysisStatus.COMPLETED,
                    "{invalid json", null, OffsetDateTime.now(), OffsetDateTime.now());
            when(repository.findByUserIdOrderByFiscalYearIdDesc(5)).thenReturn(List.of(analysis));

            List<AiAnalysisQueryResult> result = aiAnalysisService.findByUserId(5);

            assertThat(result.get(0).analysisResult()).isNull();
        }

        @Test
        void 正常系_分析結果なし_空リストを返す() {
            when(repository.findByUserIdOrderByFiscalYearIdDesc(5)).thenReturn(List.of());

            List<AiAnalysisQueryResult> result = aiAnalysisService.findByUserId(5);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class UpsertPendingAndTrigger {

        @Test
        void 正常系_既存レコードなし_新規PENDINGレコードを作成する() {
            when(repository.findByUserIdAndFiscalYearId(5, 2)).thenReturn(Optional.empty());

            aiAnalysisService.upsertPendingAndTrigger(5, 2);

            ArgumentCaptor<AiCareerAnalysis> captor = ArgumentCaptor.forClass(AiCareerAnalysis.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(AiAnalysisStatus.PENDING);
            assertThat(captor.getValue().getUserId()).isEqualTo(5);
        }

        @Test
        void 正常系_既存レコードあり_PENDINGへリセットする() {
            AiCareerAnalysis existing = AiCareerAnalysis.reconstruct(1, 5, 2, AiAnalysisStatus.FAILED,
                    null, "前回エラー", OffsetDateTime.now(), OffsetDateTime.now());
            when(repository.findByUserIdAndFiscalYearId(5, 2)).thenReturn(Optional.of(existing));

            aiAnalysisService.upsertPendingAndTrigger(5, 2);

            verify(repository).save(existing);
            assertThat(existing.getStatus()).isEqualTo(AiAnalysisStatus.PENDING);
            assertThat(existing.getErrorMessage()).isNull();
        }
    }
}
