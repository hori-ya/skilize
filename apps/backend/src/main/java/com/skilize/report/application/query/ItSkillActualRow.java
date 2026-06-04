package com.skilize.report.application.query;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ItSkillActualRow {
    private final String category1;
    private final String category2;
    private final String skillName;
    private final Integer level;
    private final String remark;
}
