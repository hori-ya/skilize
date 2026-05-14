package com.skilize.charts.presentation;

import com.skilize.charts.application.ChartService;
import com.skilize.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/charts")
@RequiredArgsConstructor
public class ChartController {

    private final ChartService chartService;

    @GetMapping("/radar")
    public ChartService.RadarResponse getRadar(@AuthenticationPrincipal User user) {
        return chartService.getRadar(user);
    }

    @GetMapping("/growth")
    public ChartService.GrowthResponse getGrowth(@AuthenticationPrincipal User user) {
        return chartService.getGrowth(user);
    }

    @GetMapping("/heatmap")
    public ChartService.HeatmapResponse getHeatmap(@AuthenticationPrincipal User user) {
        return chartService.getHeatmap(user);
    }

    @GetMapping("/timeline")
    public ChartService.TimelineResponse getTimeline(@AuthenticationPrincipal User user) {
        return chartService.getTimeline(user);
    }
}
