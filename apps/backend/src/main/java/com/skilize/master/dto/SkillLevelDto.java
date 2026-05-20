package com.skilize.master.dto;

import com.skilize.master.domain.SkillLevel;

public record SkillLevelDto(int id, short levelValue, String description, boolean isActive, int scoreWeight) {

    public static SkillLevelDto from(SkillLevel s) {
        return new SkillLevelDto(s.getId(), s.getLevelValue(), s.getDescription(), s.isActive(), s.getScoreWeight());
    }
}
