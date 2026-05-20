package com.skilize.charts.presentation;

import com.skilize.charts.application.ChartService;
import com.skilize.charts.dto.*;
import com.skilize.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * グラフデータの REST API コントローラー。認証済みユーザーの棚卸データを集計して返す。
 * 集計ロジックはすべて ChartService に委譲しており、Controller は HTTP のハンドリングのみを担う。
 */
@RestController
@RequestMapping("/api/charts")
@RequiredArgsConstructor
public class ChartController {

    private final ChartService chartService;

    /** レーダーチャート（ITスキル大分類ごとの平均スキルレベル）を返す。 */
    @GetMapping("/radar")
    public RadarResponse getRadar(@AuthenticationPrincipal User user) {
        return chartService.getRadar(user);
    }

    /** 成長推移グラフ（年度別のスキルレベル合計推移）を返す。 */
    @GetMapping("/growth")
    public GrowthResponse getGrowth(@AuthenticationPrincipal User user) {
        return chartService.getGrowth(user);
    }

    /** ヒートマップ（年度×スキルレベルの分布）を返す。 */
    @GetMapping("/heatmap")
    public HeatmapResponse getHeatmap(@AuthenticationPrincipal User user) {
        return chartService.getHeatmap(user);
    }

    /** タイムライン（資格取得・セミナー受講の時系列データ）を返す。 */
    @GetMapping("/timeline")
    public TimelineResponse getTimeline(@AuthenticationPrincipal User user) {
        return chartService.getTimeline(user);
    }
}
