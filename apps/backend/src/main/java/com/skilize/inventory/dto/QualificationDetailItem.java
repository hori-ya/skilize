package com.skilize.inventory.dto;

public record QualificationDetailItem(Integer id, Integer qualificationId,
                                      String customQualificationName,
                                      String acquiredYearMonth, String remarks) {}
