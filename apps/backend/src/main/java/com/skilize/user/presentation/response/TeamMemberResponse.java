package com.skilize.user.presentation.response;

import com.skilize.inventory.domain.Inventory;
import com.skilize.user.domain.User;

import java.util.Map;

/**
 * チームメンバー1件のレスポンス。GET /api/users/me/team-members のレスポンスに使用する（TL/ADMIN 向け）。
 * 現在年度の棚卸サマリを内包する。
 *
 * @param id               ユーザー内部PK
 * @param userId           ログインID
 * @param name             氏名
 * @param email            メールアドレス
 * @param role             ロール（GENERAL / TL / ADMIN）
 * @param tlUserId         所属TLのユーザー内部PK
 * @param tlName           所属TL氏名
 * @param isActive         有効フラグ
 * @param currentInventory 当年度棚卸サマリ（棚卸未作成の場合は null）
 */
public record TeamMemberResponse(int id, String userId, String name, String email,
                                  String role, Integer tlUserId, String tlName,
                                  boolean isActive, CurrentInventoryInfo currentInventory) {

    /**
     * 当年度棚卸サマリ。
     *
     * @param id         棚卸内部PK
     * @param fiscalYear 年度参照情報
     * @param status     棚卸ステータス
     */
    public record CurrentInventoryInfo(int id, FiscalYearRef fiscalYear, String status) {}

    public static TeamMemberResponse from(User u, Inventory inv, Map<Integer, String> nameById) {
        CurrentInventoryInfo invInfo = inv == null ? null : new CurrentInventoryInfo(
                inv.getId(),
                new FiscalYearRef(inv.getFiscalYear().getId(), inv.getFiscalYear().getName()),
                inv.getStatus().name()
        );
        String tlName = u.getTlUserId() != null ? nameById.get(u.getTlUserId()) : null;
        return new TeamMemberResponse(u.getId(), u.getUserId(), u.getName(), u.getEmail(),
                u.getRole().name(), u.getTlUserId(), tlName, u.isActive(), invInfo);
    }
}
