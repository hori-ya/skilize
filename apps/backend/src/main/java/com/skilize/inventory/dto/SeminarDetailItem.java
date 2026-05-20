package com.skilize.inventory.dto;

public record SeminarDetailItem(Integer id, Integer adSeminarId, String seminarName,
                                Integer seminarCategoryId, String attendedYearMonth, String remarks) {}
