package com.skilize.report.application.query;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdSeminarGoalRow {
    private final String category;
    private final String seminarName;
    private final Integer scheduledMonth;
    private final String remark;
}
