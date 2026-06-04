package com.skilize.report.application.query;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdSeminarActualRow {
    private final String category;
    private final String seminarName;
    private final Integer attendedMonth;
    private final String remark;
}
