package com.skilize.interview.dto;

import com.skilize.interview.domain.DetailType;

public record DetailNoteItem(DetailType detailType, Integer detailId, String note) {}
