package com.skilize.charts.dto;

import java.util.List;

public record TimelineResponse(List<TimelineEvent> events) {

    public record TimelineEvent(String type, String lane, String name,
                                 String yearMonth, boolean isPast) {}
}
