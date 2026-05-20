package com.skilize.inventory.dto;

import com.skilize.inventory.domain.ItSkillDetail;

public record ItSkillDetailDto(int id, Integer itSkillId, String itSkillName,
                                String customSkillName, int skillLevelId,
                                short levelValue, String remarks) {

    public static ItSkillDetailDto from(ItSkillDetail d) {
        return new ItSkillDetailDto(d.getId(),
                d.getItSkill() != null ? d.getItSkill().getId() : null,
                d.getItSkill() != null ? d.getItSkill().getName() : null,
                d.getCustomSkillName(),
                d.getSkillLevel().getId(), d.getSkillLevel().getLevelValue(), d.getRemarks());
    }
}
