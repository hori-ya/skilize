package com.skilize.user.dto;

import com.skilize.inventory.domain.Inventory;
import com.skilize.user.domain.User;

import java.util.Map;

public record TeamMemberDto(int id, String userId, String name, String email,
                             String role, Integer tlUserId, String tlName,
                             boolean isActive, CurrentInventoryDto currentInventory) {

    public record CurrentInventoryDto(int id, FiscalYearRef fiscalYear, String status) {}

    public static TeamMemberDto from(User u, Inventory inv, Map<Integer, String> nameById) {
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
