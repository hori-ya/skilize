package com.skilize.inventory.dto;

public record GoalItem(Integer id, String goalCategory, Integer itSkillId,
                       Integer qualificationId, Integer adSeminarId,
                       String customName, String targetPeriod, String reason) {}
