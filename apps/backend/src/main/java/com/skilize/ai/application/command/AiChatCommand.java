package com.skilize.ai.application.command;

import java.util.List;

public record AiChatCommand(
        String message,
        String mode,
        int userId,
        List<ChatHistoryItem> history
) {
    public record ChatHistoryItem(String role, String content) {}
}
