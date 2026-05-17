package com.skilize.interview.presentation;

import com.skilize.interview.domain.DetailType;
import jakarta.validation.constraints.NotNull;

public record DetailNoteRequest(
        @NotNull DetailType detailType,
        @NotNull Integer detailId,
        String note
) {}
