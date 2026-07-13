/**************************************************************************************************************
 * 機能ID      ：USR
 * 機能名      ：ユーザー管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ユーザー管理・チーム照会の REST API コントローラー。
 * ADMIN専用エンドポイント（一覧・作成・更新・PW リセット）と TL/ADMIN共通エンドポイント（チームメンバー照会）を提供する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.user.presentation;

import com.skilize.fiscalyear.domain.model.FiscalYear;
import com.skilize.inventory.domain.model.Inventory;
import com.skilize.shared.domain.exception.AuthException;
import com.skilize.user.application.UserService;
import com.skilize.user.domain.model.Role;
import com.skilize.user.domain.model.User;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ユーザー管理・チーム照会の REST API コントローラー。
 * ADMIN 専用: ユーザー一覧・作成・更新・パスワードリセット
 * TL/ADMIN 共通: チームメンバー一覧・ユーザー別棚卸一覧
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ─── Admin-only endpoints ─────────────────────────────────────────────────

    /**
     * 全ユーザーをユーザーID昇順で返す（ADMIN のみ）。
     * TL名表示のために nameById マップを構築し、レスポンス変換時に使用する。
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> list() {
        List<User> users = userService.findAllOrdered();
        // TL名を tlUserId から引けるよう「内部ID → 氏名」のマップを構築する
        Map<Integer, String> nameById = buildNameById(users);
        List<UserResponse> responses = new ArrayList<>();
        for (User u : users) {
            responses.add(UserResponse.from(u, nameById));
        }
        return responses;
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
        List<User> all = userService.findAllOrdered();
        Map<Integer, String> nameById = buildNameById(all);
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
        boolean active = true;
        if (req.active() != null) {
            active = req.active();
        }
        User saved = userService.update(id, req.name(), req.email(), Role.valueOf(req.role()),
                req.tlUserId(), active);
        List<User> all = userService.findAllOrdered();
        Map<Integer, String> nameById = buildNameById(all);
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
    public List<TeamMemberResponse> getTeamMembers(@AuthenticationPrincipal(expression = "user") User currentUser) {
        // ADMIN は全有効ユーザー、TL は tlUserId が自分の ID と一致する有効ユーザーのみ取得する
        List<User> members = userService.findActiveMembersFor(currentUser);

        // 今日の日付を基準に現在の有効年度を取得する（存在しない場合は棚卸情報が null になる）
        Optional<FiscalYear> currentFy = userService.findCurrentFiscalYear();

        List<User> allUsers = userService.findAllOrdered();
        Map<Integer, String> nameById = buildNameById(allUsers);

        List<TeamMemberResponse> responses = new ArrayList<>();
        for (User member : members) {
            // 今年度の棚卸を取得する。年度なし・棚卸なしの場合は null を設定する
            Inventory inv = null;
            if (currentFy.isPresent()) {
                Optional<Inventory> inventoryOptional =
                        userService.findCurrentInventory(member.getId(), currentFy.get().getId());
                if (inventoryOptional.isPresent()) {
                    inv = inventoryOptional.get();
                }
            }
            responses.add(TeamMemberResponse.from(member, inv, nameById));
        }
        return responses;
    }

    /**
     * 指定ユーザーの棚卸一覧を返す（TL/ADMIN のみ）。
     * TL は担当メンバーのみ参照可（他チームへのアクセスは 403）。
     */
    @GetMapping("/{id}/inventories")
    @PreAuthorize("hasAnyRole('TL', 'ADMIN')")
    public List<MemberInventorySummaryResponse> getUserInventories(
            @PathVariable int id,
            @AuthenticationPrincipal(expression = "user") User currentUser) {
        Optional<User> targetUserOptional = userService.findById(id);
        if (targetUserOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
        }
        User targetUser = targetUserOptional.get();

        // TL は担当チームメンバー（tl_user_id が自分のID のユーザー）のみ参照可
        if (currentUser.getRole() == Role.TL) {
            if (!currentUser.getId().equals(targetUser.getTlUserId())) {
                throw new AuthException("FORBIDDEN", "");
            }
        }

        List<MemberInventorySummaryResponse> responses = new ArrayList<>();
        for (Inventory inv : userService.findInventoriesByUserId(id)) {
            responses.add(MemberInventorySummaryResponse.from(inv));
        }
        return responses;
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

    /** ユーザー内部PK → 氏名 のマップを構築する（TL名表示の解決に使用する）。 */
    private Map<Integer, String> buildNameById(List<User> users) {
        Map<Integer, String> nameById = new HashMap<>();
        for (User u : users) {
            nameById.put(u.getId(), u.getName());
        }
        return nameById;
    }

}
