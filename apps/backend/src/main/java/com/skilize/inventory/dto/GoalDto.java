package com.skilize.inventory.dto;

import com.skilize.inventory.domain.InventoryGoal;

public record GoalDto(int id, String goalCategory,
                       Integer itSkillId, String itSkillName,
                       Integer qualificationId, String qualificationName,
                       Integer adSeminarId, String adSeminarName,
                       String customName, String targetPeriod, String reason) {

    public static GoalDto from(InventoryGoal g) {
        return new GoalDto(g.getId(), g.getGoalCategory().name(),
                g.getItSkill() != null ? g.getItSkill().getId() : null,
                g.getItSkill() != null ? g.getItSkill().getName() : null,
                g.getQualification() != null ? g.getQualification().getId() : null,
                g.getQualification() != null ? g.getQualification().getName() : null,
                g.getAdSeminar() != null ? g.getAdSeminar().getId() : null,
                g.getAdSeminar() != null ? g.getAdSeminar().getName() : null,
                g.getCustomName(),
                g.getTargetPeriod().toString(), g.getReason());
    }
}
