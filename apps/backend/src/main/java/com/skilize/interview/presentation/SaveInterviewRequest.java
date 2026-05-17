package com.skilize.interview.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SaveInterviewRequest(
        String generalNote,
        @NotNull @Valid List<DetailNoteRequest> detailNotes
) {}
