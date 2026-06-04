package com.skilize.user.presentation;

import com.skilize.inventory.domain.Inventory;
import com.skilize.inventory.domain.InventoryRepository;
import com.skilize.shared.domain.exception.AuthException;
import com.skilize.user.application.UserService;
import com.skilize.user.domain.Role;
import com.skilize.user.domain.User;
import com.skilize.user.domain.UserRepository;
import com.skilize.user.presentation.request.CreateUserRequest;
import com.skilize.user.presentation.request.UpdateUserRequest;
import com.skilize.user.presentation.response.MemberInventorySummaryResponse;
import com.skilize.user.presentation.response.ResetPasswordResponse;
import com.skilize.user.presentation.response.TeamMemberResponse;
import com.skilize.user.presentation.response.UserResponse;
import jakarta.validation.Valid;
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

/**
 * ユーザー管理・チーム照会の REST API コントローラー。
 * ADMIN 専用: ユーザー一覧・作成・更新・パスワードリセット
 * TL/ADMIN 共通: チームメンバー一覧・ユーザー別棚卸一覧
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final InventoryRepository inventoryRepository;

    // ─── Admin-only endpoints ─────────────────────────────────────────────────

    /**
     * 全ユーザーをユーザーID昇順で返す（ADMIN のみ）。
     * TL名表示のために nameById マップを構築し、レスポンス変換時に使用する。
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> list() {
        List<User> users = userRepository.findAllByOrderByUserIdAsc();
        // TL名を tlUserId から引けるよう「内部ID → 氏名」のマップを構築する
        Map<Integer, String> nameById = users.stream()
                .collect(Collectors.toMap(User::getId, User::getName));
        return users.stream().map(u -> UserResponse.from(u, nameById)).toList();
    }

    /**
     * ユーザーを新規作成する（ADMIN のみ）。初期パスワードはユーザーIDと同一。
     * TL名表示のため、作成後に全ユーザーを再取得して nameById マップを再構築する。
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        validateRole(req.role());
        User saved = userService.create(req.userId(), req.name(), req.email(),
                Role.valueOf(req.role()), req.tlUserId());
        // 作成後に全ユーザーを再取得してTL名マップを更新する（作成ユーザーを含むため）
        List<User> all = userRepository.findAllByOrderByUserIdAsc();
        Map<Integer, String> nameById = all.stream().collect(Collectors.toMap(User::getId, User::getName));
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(saved, nameById));
    }

    /**
     * ユーザー情報を更新する（ADMIN のみ）。
     * active フラグが未送信の場合は true（有効）をデフォルトとする。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse update(@PathVariable int id, @Valid @RequestBody UpdateUserRequest req) {
        validateRole(req.role());
        // active が null（未送信）の場合は true をデフォルトとする
        User saved = userService.update(id, req.name(), req.email(), Role.valueOf(req.role()),
                req.tlUserId(), req.active() != null ? req.active() : true);
        List<User> all = userRepository.findAllByOrderByUserIdAsc();
        Map<Integer, String> nameById = all.stream().collect(Collectors.toMap(User::getId, User::getName));
        return UserResponse.from(saved, nameById);
    }

    /**
     * パスワードをリセットする（ADMIN のみ）。仮パスワード（= ユーザーID）を返す。
     * リセット後、ユーザーが次回ログイン時にパスワード変更を強制される（is_initial_password = true）。
     */
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResetPasswordResponse resetPassword(@PathVariable int id) {
        String tempPassword = userService.resetPassword(id);
        return new ResetPasswordResponse(tempPassword);
    }

    // ─── TL + Admin endpoints ─────────────────────────────────────────────────

    /**
     * チームメンバー一覧を返す（TL/ADMIN のみ）。
     * ADMIN は全有効ユーザー、TL は自分が担当する有効ユーザーのみ参照できる。
     * 各メンバーの今年度棚卸情報を付与して返す。
     */
    @GetMapping("/me/team-members")
    @PreAuthorize("hasAnyRole('TL', 'ADMIN')")
    public List<TeamMemberResponse> getTeamMembers(@AuthenticationPrincipal User currentUser) {
        // ADMIN は全有効ユーザー、TL は tlUserId が自分の ID と一致する有効ユーザーのみ取得する
        List<User> members = currentUser.getRole() == Role.ADMIN
                ? userRepository.findByActiveTrue()
                : userRepository.findByTlUserIdAndActiveTrue(currentUser.getId());

        // 今日の日付を基準に現在の有効年度を取得する（存在しない場合は棚卸情報が null になる）
        var currentFy = userService.findCurrentFiscalYear();

        List<User> allUsers = userRepository.findAllByOrderByUserIdAsc();
        Map<Integer, String> nameById = allUsers.stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        return members.stream().map(member -> {
            // 今年度の棚卸を取得する。年度なし・棚卸なしの場合は null を設定する
            Inventory inv = currentFy
                    .flatMap(fy -> userService.findCurrentInventory(member.getId(), fy.getId()))
                    .orElse(null);
            return TeamMemberResponse.from(member, inv, nameById);
        }).toList();
    }

    /**
     * 指定ユーザーの棚卸一覧を返す（TL/ADMIN のみ）。
     * TL は担当メンバーのみ参照可（他チームへのアクセスは 403）。
     */
    @GetMapping("/{id}/inventories")
    @PreAuthorize("hasAnyRole('TL', 'ADMIN')")
    public List<MemberInventorySummaryResponse> getUserInventories(
            @PathVariable int id,
            @AuthenticationPrincipal User currentUser) {
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        // TL は担当チームメンバー（tl_user_id が自分のID のユーザー）のみ参照可
        if (currentUser.getRole() == Role.TL) {
            if (!currentUser.getId().equals(targetUser.getTlUserId())) {
                throw new AuthException("FORBIDDEN", "");
            }
        }

        return inventoryRepository.findByUserIdWithFiscalYear(id)
                .stream()
                .map(MemberInventorySummaryResponse::from)
                .toList();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * ロール文字列が有効な Role 列挙値かを検証する。
     * Role.valueOf() は不正文字列で IllegalArgumentException をスローするため、それをキャッチして 400 に変換する。
     */
    private void validateRole(String role) {
        try {
            Role.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_ROLE");
        }
    }

}
