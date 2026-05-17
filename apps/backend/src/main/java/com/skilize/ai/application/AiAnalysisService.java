package com.skilize.ai.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skilize.ai.domain.AiCareerAnalysis;
import com.skilize.ai.domain.AiCareerAnalysisRepository;
import com.skilize.ai.presentation.AiAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiCareerAnalysisRepository repository;

    @Value("${ai.service.url:http://ai:8000}")
    private String aiServiceUrl;

    @Value("${ai.secret.key:}")
    private String aiSecretKey;

    @Transactional(readOnly = true)
    public List<AiAnalysisResponse> findByUserId(int userId) {
        return repository.findByUserIdOrderByFiscalYearIdDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Async
    @Transactional
    public void upsertPendingAndTrigger(int userId, int fiscalYearId) {
        AiCareerAnalysis analysis = repository.findByUserIdAndFiscalYearId(userId, fiscalYearId)
                .map(existing -> { existing.resetToPending(); return existing; })
                .orElseGet(() -> AiCareerAnalysis.createPending(userId, fiscalYearId));
        repository.save(analysis);
        callAiService(userId, fiscalYearId);
    }

    private void callAiService(int userId, int fiscalYearId) {
        try {
            String body = String.format("{\"userId\":%d,\"fiscalYearId\":%d}", userId, fiscalYearId);
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(aiServiceUrl + "/analyze"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            if (aiSecretKey != null && !aiSecretKey.isEmpty()) {
                requestBuilder.header("X-Internal-Key", aiSecretKey);
            }
            HttpRequest request = requestBuilder.build();
            client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(ex -> {
                        log.error("AI service call failed for user={} fiscalYear={}", userId, fiscalYearId, ex);
                        return null;
                    });
        } catch (Exception e) {
            log.error("Failed to call AI service for user={} fiscalYear={}", userId, fiscalYearId, e);
        }
    }

    private AiAnalysisResponse toResponse(AiCareerAnalysis a) {
        Object parsed = null;
        if (a.getAnalysisResult() != null) {
            try {
                parsed = OBJECT_MAPPER.readValue(a.getAnalysisResult(), Object.class);
            } catch (Exception e) {
                log.warn("Failed to parse analysisResult for id={}", a.getId(), e);
            }
        }
        return new AiAnalysisResponse(
                a.getId(),
                a.getFiscalYearId(),
                a.getStatus().name(),
                parsed,
                a.getErrorMessage(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
