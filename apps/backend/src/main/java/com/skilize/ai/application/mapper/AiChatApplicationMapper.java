package com.skilize.ai.application.mapper;

import com.skilize.ai.application.command.AiChatCommand;
import com.skilize.ai.presentation.request.AiChatRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiChatApplicationMapper {

    public AiChatCommand toCommand(AiChatRequest request, int userId) {
        List<AiChatCommand.ChatHistoryItem> history = request.history() == null ? List.of() :
                request.history().stream()
                        .map(item -> new AiChatCommand.ChatHistoryItem(item.role(), item.content()))
                        .toList();
        return new AiChatCommand(request.message(), request.mode(), userId, history);
    }
}
