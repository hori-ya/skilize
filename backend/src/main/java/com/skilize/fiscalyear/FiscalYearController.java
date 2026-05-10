package com.skilize.fiscalyear;

import com.skilize.domain.fiscalyear.FiscalYear;
import com.skilize.domain.fiscalyear.FiscalYearRepository;
import com.skilize.domain.fiscalyear.FiscalYearSettings;
import com.skilize.domain.fiscalyear.FiscalYearSettingsRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FiscalYearController {

    private final FiscalYearRepository fiscalYearRepository;
    private final FiscalYearSettingsRepository settingsRepository;

    @GetMapping("/fiscal-years")
    public List<FiscalYearDto> list() {
        return fiscalYearRepository.findAll().stream()
                .sorted((a, b) -> b.getStartDate().compareTo(a.getStartDate()))
                .map(FiscalYearDto::from)
                .toList();
    }

    @GetMapping("/fiscal-years/current")
    public ResponseEntity<FiscalYearDto> current() {
        return fiscalYearRepository.findCurrent(LocalDate.now())
                .map(FiscalYearDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/fiscal-years")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<FiscalYearDto> create(@Valid @RequestBody FiscalYearRequest req) {
        FiscalYear fy = FiscalYear.create(
                req.name(),
                LocalDate.parse(req.startDate()),
                LocalDate.parse(req.endDate()),
                req.inputStartDate() != null ? LocalDate.parse(req.inputStartDate()) : null,
                req.inputEndDate() != null ? LocalDate.parse(req.inputEndDate()) : null
        );
        FiscalYear saved = fiscalYearRepository.save(fy);
        return ResponseEntity.status(HttpStatus.CREATED).body(FiscalYearDto.from(saved));
    }

    @PutMapping("/fiscal-years/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public FiscalYearDto update(@PathVariable int id, @Valid @RequestBody FiscalYearRequest req) {
        FiscalYear fy = fiscalYearRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        fy.update(
                req.name(),
                LocalDate.parse(req.startDate()),
                LocalDate.parse(req.endDate()),
                req.inputStartDate() != null ? LocalDate.parse(req.inputStartDate()) : null,
                req.inputEndDate() != null ? LocalDate.parse(req.inputEndDate()) : null,
                req.active() != null ? req.active() : fy.isActive()
        );
        return FiscalYearDto.from(fiscalYearRepository.save(fy));
    }

    @GetMapping("/fiscal-year-settings")
    public FiscalYearSettingsDto getSettings() {
        FiscalYearSettings s = settingsRepository.findById((short) 1)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new FiscalYearSettingsDto(s.getFiscalYearStartMonth());
    }

    @PutMapping("/fiscal-year-settings")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public FiscalYearSettingsDto updateSettings(@Valid @RequestBody FiscalYearSettingsRequest req) {
        FiscalYearSettings s = settingsRepository.findById((short) 1)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        s.setFiscalYearStartMonth(req.fiscalYearStartMonth());
        settingsRepository.save(s);
        return new FiscalYearSettingsDto(s.getFiscalYearStartMonth());
    }

    public record FiscalYearDto(int id, String name, String startDate, String endDate,
                                String inputStartDate, String inputEndDate, boolean isActive) {
        static FiscalYearDto from(FiscalYear f) {
            return new FiscalYearDto(f.getId(), f.getName(),
                    f.getStartDate().toString(),
                    f.getEndDate().toString(),
                    f.getInputStartDate() != null ? f.getInputStartDate().toString() : null,
                    f.getInputEndDate() != null ? f.getInputEndDate().toString() : null,
                    f.isActive());
        }
    }

    public record FiscalYearRequest(
            @NotBlank String name,
            @NotBlank String startDate,
            @NotBlank String endDate,
            String inputStartDate,
            String inputEndDate,
            Boolean active
    ) {}

    public record FiscalYearSettingsDto(short fiscalYearStartMonth) {}

    public record FiscalYearSettingsRequest(
            @NotNull @Min(1) @Max(12) Short fiscalYearStartMonth
    ) {}
}
