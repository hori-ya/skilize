package com.skilize.user.presentation;

import com.skilize.inventory.domain.Inventory;
import com.skilize.inventory.domain.InventoryRepository;
import com.skilize.shared.domain.exception.AuthException;
import com.skilize.user.application.UserService;
import com.skilize.user.domain.Role;
import com.skilize.user.domain.User;
import com.skilize.user.domain.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final InventoryRepository inventoryRepository;

    // ─── Admin-only endpoints ─────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDto> list() {
        List<User> users = userRepository.findAllByOrderByUserIdAsc();
        Map<Integer, String> nameById = users.stream()
                .collect(Collectors.toMap(User::getId, User::getName));
        return users.stream().map(u -> UserDto.from(u, nameById)).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> create(@Valid @RequestBody CreateUserRequest req) {
        validateRole(req.role());
        User saved = userService.create(req.userId(), req.name(), req.email(),
                Role.valueOf(req.role()), req.tlUserId());
        List<User> all = userRepository.findAllByOrderByUserIdAsc();
        Map<Integer, String> nameById = all.stream().collect(Collectors.toMap(User::getId, User::getName));
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDto.from(saved, nameById));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto update(@PathVariable int id, @Valid @RequestBody UpdateUserRequest req) {
        validateRole(req.role());
        User saved = userService.update(id, req.name(), req.email(), Role.valueOf(req.role()),
                req.tlUserId(), req.active() != null ? req.active() : true);
        List<User> all = userRepository.findAllByOrderByUserIdAsc();
        Map<Integer, String> nameById = all.stream().collect(Collectors.toMap(User::getId, User::getName));
        return UserDto.from(saved, nameById);
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResetPasswordResponse resetPassword(@PathVariable int id) {
        String tempPassword = userService.resetPassword(id);
        return new ResetPasswordResponse(tempPassword);
    }

    // ─── TL + Admin endpoints ─────────────────────────────────────────────────

    @GetMapping("/me/team-members")
    @PreAuthorize("hasAnyRole('TL', 'ADMIN')")
    public List<TeamMemberDto> getTeamMembers(@AuthenticationPrincipal User currentUser) {
        List<User> members = currentUser.getRole() == Role.ADMIN
                ? userRepository.findByActiveTrue()
                : userRepository.findByTlUserIdAndActiveTrue(currentUser.getId());

        var currentFy = userService.findCurrentFiscalYear();

        List<User> allUsers = userRepository.findAllByOrderByUserIdAsc();
        Map<Integer, String> nameById = allUsers.stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        return members.stream().map(member -> {
            Inventory inv = currentFy
                    .flatMap(fy -> userService.findCurrentInventory(member.getId(), fy.getId()))
                    .orElse(null);
            return TeamMemberDto.from(member, inv, nameById);
        }).toList();
    }

    @GetMapping("/{id}/inventories")
    @PreAuthorize("hasAnyRole('TL', 'ADMIN')")
    public List<MemberInventorySummaryDto> getUserInventories(
            @PathVariable int id,
            @AuthenticationPrincipal User currentUser) {
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ユーザーが見つかりません"));

        if (currentUser.getRole() == Role.TL) {
            if (!currentUser.getId().equals(targetUser.getTlUserId())) {
                throw new AuthException("FORBIDDEN", "このユーザーへのアクセス権限がありません");
            }
        }

        return inventoryRepository.findByUserIdWithFiscalYear(id)
                .stream()
                .map(MemberInventorySummaryDto::from)
                .toList();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void validateRole(String role) {
        try {
            Role.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不正なロールです: " + role);
        }
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    public record UserDto(int id, String userId, String name, String email, String role,
                           Integer tlUserId, String tlName, boolean isInitialPassword,
                           boolean isActive, String createdAt) {
        static UserDto from(User u, Map<Integer, String> nameById) {
            return new UserDto(
                    u.getId(), u.getUserId(), u.getName(), u.getEmail(),
                    u.getRole().name(),
                    u.getTlUserId(),
                    u.getTlUserId() != null ? nameById.get(u.getTlUserId()) : null,
                    u.isInitialPassword(), u.isActive(),
                    u.getCreatedAt() != null ? u.getCreatedAt().toString() : null
            );
        }
    }

    public record CreateUserRequest(
            @NotBlank String userId,
            @NotBlank String name,
            String email,
            @NotBlank String role,
            Integer tlUserId
    ) {}

    public record UpdateUserRequest(
            @NotBlank String name,
            String email,
            @NotBlank @Pattern(regexp = "GENERAL|TL|ADMIN") String role,
            Integer tlUserId,
            Boolean active
    ) {}

    public record ResetPasswordResponse(String temporaryPassword) {}

    record FiscalYearRef(int id, String name) {}

    record CurrentInventoryDto(int id, FiscalYearRef fiscalYear, String status) {}

    public record TeamMemberDto(int id, String userId, String name, String email,
                                 String role, Integer tlUserId, String tlName,
                                 boolean isActive, CurrentInventoryDto currentInventory) {
        static TeamMemberDto from(User u, Inventory inv, Map<Integer, String> nameById) {
            CurrentInventoryDto invDto = inv == null ? null : new CurrentInventoryDto(
                    inv.getId(),
                    new FiscalYearRef(inv.getFiscalYear().getId(), inv.getFiscalYear().getName()),
                    inv.getStatus().name()
            );
            String tlName = u.getTlUserId() != null ? nameById.get(u.getTlUserId()) : null;
            return new TeamMemberDto(u.getId(), u.getUserId(), u.getName(), u.getEmail(),
                    u.getRole().name(), u.getTlUserId(), tlName, u.isActive(), invDto);
        }
    }

    public record MemberInventorySummaryDto(int id, FiscalYearRef fiscalYear, String status,
                                             String submittedAt, String goalCompletedAt) {
        static MemberInventorySummaryDto from(Inventory inv) {
            return new MemberInventorySummaryDto(
                    inv.getId(),
                    new FiscalYearRef(inv.getFiscalYear().getId(), inv.getFiscalYear().getName()),
                    inv.getStatus().name(),
                    inv.getSubmittedAt() != null ? inv.getSubmittedAt().toString() : null,
                    inv.getGoalCompletedAt() != null ? inv.getGoalCompletedAt().toString() : null
            );
        }
    }
}
