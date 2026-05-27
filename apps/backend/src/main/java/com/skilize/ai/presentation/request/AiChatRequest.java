package com.skilize.ai.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AiChatRequest(
        @NotBlank @Size(max = 4000) String message,
        @NotNull @Pattern(regexp = "NORMAL|PROOFREADING|CAREER|HELP") String mode,
        List<ChatHistoryItem> history
) {
    public record ChatHistoryItem(
            @NotBlank @Pattern(regexp = "user|assistant") String role,
            @NotBlank @Size(max = 8000) String content
    ) {}
}
