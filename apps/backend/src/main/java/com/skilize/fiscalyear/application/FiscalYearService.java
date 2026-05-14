package com.skilize.fiscalyear.application;

import com.skilize.fiscalyear.domain.FiscalYear;
import com.skilize.fiscalyear.domain.FiscalYearRepository;
import com.skilize.fiscalyear.domain.FiscalYearSettings;
import com.skilize.fiscalyear.domain.FiscalYearSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class FiscalYearService {

    private final FiscalYearRepository fiscalYearRepository;
    private final FiscalYearSettingsRepository settingsRepository;

    @Transactional
    public FiscalYear createFiscalYear(String name, LocalDate startDate, LocalDate endDate,
                                       LocalDate inputStartDate, LocalDate inputEndDate) {
        FiscalYear fy = FiscalYear.create(name, startDate, endDate, inputStartDate, inputEndDate);
        return fiscalYearRepository.save(fy);
    }

    @Transactional
    public FiscalYear updateFiscalYear(int id, String name, LocalDate startDate, LocalDate endDate,
                                       LocalDate inputStartDate, LocalDate inputEndDate, boolean active) {
        FiscalYear fy = fiscalYearRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        fy.update(name, startDate, endDate, inputStartDate, inputEndDate, active);
        return fiscalYearRepository.save(fy);
    }

    @Transactional
    public FiscalYearSettings updateSettings(short fiscalYearStartMonth) {
        FiscalYearSettings s = settingsRepository.findById((short) 1)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        s.setFiscalYearStartMonth(fiscalYearStartMonth);
        return settingsRepository.save(s);
    }
}
