package com.skilize.ai.presentation;

import com.skilize.ai.application.AiChatService;
import com.skilize.ai.application.command.AiChatCommand;
import com.skilize.ai.application.mapper.AiChatApplicationMapper;
import com.skilize.ai.application.query.AiChatQueryResult;
import com.skilize.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.skilize.ai.presentation.request.AiChatRequest;

/**
 * AI チャット REST API コントローラー。
 * フロントエンドからのチャットリクエストを受け取り、Python AI サービスに転送して応答を返す。
 * 会話履歴の永続化は行わない（フロントエンドのみで保持）。
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
    private final AiChatApplicationMapper mapper;

    @PostMapping("/chat")
    public AiChatQueryResult chat(
            @Valid @RequestBody AiChatRequest request,
            @AuthenticationPrincipal User user
    ) {
        AiChatCommand command = mapper.toCommand(request, user.getId());
        return aiChatService.chat(command);
    }
}
