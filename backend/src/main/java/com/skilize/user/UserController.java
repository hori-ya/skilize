package com.skilize.user;

import com.skilize.common.exception.AuthException;
import com.skilize.domain.fiscalyear.FiscalYear;
import com.skilize.domain.fiscalyear.FiscalYearRepository;
import com.skilize.domain.inventory.Inventory;
import com.skilize.domain.inventory.InventoryRepository;
import com.skilize.domain.user.Role;
import com.skilize.domain.user.User;
import com.skilize.domain.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FiscalYearRepository fiscalYearRepository;
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
    @Transactional
    public ResponseEntity<UserDto> create(@Valid @RequestBody CreateUserRequest req) {
        if (userRepository.findByUserId(req.userId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "このユーザーIDは既に使用されています");
        }
        validateRole(req.role());
        User user = User.create(req.userId(), req.name(), req.email(),
                Role.valueOf(req.role()), req.tlUserId(),
                passwordEncoder.encode(req.userId()));
        User saved = userRepository.save(user);
        List<User> all = userRepository.findAllByOrderByUserIdAsc();
        Map<Integer, String> nameById = all.stream().collect(Collectors.toMap(User::getId, User::getName));
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDto.from(saved, nameById));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserDto update(@PathVariable int id, @Valid @RequestBody UpdateUserRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        validateRole(req.role());
        user.update(req.name(), req.email(), Role.valueOf(req.role()), req.tlUserId(),
                req.active() != null ? req.active() : user.isActive());
        User saved = userRepository.save(user);
        List<User> all = userRepository.findAllByOrderByUserIdAsc();
        Map<Integer, String> nameById = all.stream().collect(Collectors.toMap(User::getId, User::getName));
        return UserDto.from(saved, nameById);
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResetPasswordResponse resetPassword(@PathVariable int id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String tempPassword = user.getUserId();
        user.resetPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);
        return new ResetPasswordResponse(tempPassword);
    }

    // ─── TL + Admin endpoints ─────────────────────────────────────────────────

    @GetMapping("/me/team-members")
    @PreAuthorize("hasAnyRole('TL', 'ADMIN')")
    public List<TeamMemberDto> getTeamMembers(@AuthenticationPrincipal User currentUser) {
        List<User> members = currentUser.getRole() == Role.ADMIN
                ? userRepository.findByActiveTrue()
                : userRepository.findByTlUserIdAndActiveTrue(currentUser.getId());

        Optional<FiscalYear> currentFy = fiscalYearRepository.findCurrent(LocalDate.now());

        List<User> allUsers = userRepository.findAllByOrderByUserIdAsc();
        Map<Integer, String> nameById = allUsers.stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        return members.stream().map(member -> {
            Inventory inv = currentFy
                    .flatMap(fy -> inventoryRepository.findByUserIdAndFiscalYearId(member.getId(), fy.getId()))
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
