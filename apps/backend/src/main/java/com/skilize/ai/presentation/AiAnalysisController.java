package com.skilize.ai.presentation;

import com.skilize.ai.application.AiAnalysisService;
import com.skilize.shared.domain.exception.AuthException;
import com.skilize.user.domain.Role;
import com.skilize.user.domain.User;
import com.skilize.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;
    private final UserRepository userRepository;

    @GetMapping("/users/me/ai-analyses")
    public List<AiAnalysisResponse> getMyAnalyses(@AuthenticationPrincipal User user) {
        return aiAnalysisService.findByUserId(user.getId());
    }

    @GetMapping("/users/{userId}/ai-analyses")
    @PreAuthorize("hasAnyRole('TL', 'ADMIN')")
    public List<AiAnalysisResponse> getMemberAnalyses(
            @PathVariable int userId,
            @AuthenticationPrincipal User currentUser) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ユーザーが見つかりません"));

        if (currentUser.getRole() == Role.TL) {
            if (!currentUser.getId().equals(targetUser.getTlUserId())) {
                throw new AuthException("FORBIDDEN", "このユーザーへのアクセス権限がありません");
            }
        }

        return aiAnalysisService.findByUserId(userId);
    }
}
