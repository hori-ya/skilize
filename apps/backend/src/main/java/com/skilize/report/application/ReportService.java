package com.skilize.report.application;

import com.skilize.inventory.domain.*;
import com.skilize.master.domain.ItSkillCategory;
import com.skilize.report.application.query.*;
import com.skilize.shared.domain.exception.AuthException;
import com.skilize.user.domain.User;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.fonts.FontFamily;
import net.sf.jasperreports.engine.fonts.SimpleFontExtensionHelper;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.pdf.JRPdfExporter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private SimpleJasperReportsContext jasperCtx;

    @PostConstruct
    void initFonts() {
        SimpleJasperReportsContext ctx = new SimpleJasperReportsContext();
        ctx.setParent(DefaultJasperReportsContext.getInstance());
        try (InputStream is = getClass().getResourceAsStream("/fonts/fonts.xml")) {
            if (is != null) {
                List<FontFamily> families = SimpleFontExtensionHelper.getInstance()
                        .loadFontFamilies(ctx, is);
                ctx.setExtensions(FontFamily.class, families);
                log.info("日本語フォント拡張を登録しました: {} ファミリー", families.size());
            } else {
                log.warn("fonts/fonts.xml がクラスパスに見つかりません");
            }
        } catch (Exception e) {
            log.warn("フォント拡張の登録に失敗しました", e);
        }
        jasperCtx = ctx;
    }

    private final InventoryRepository inventoryRepository;
    private final ItSkillDetailRepository itSkillDetailRepository;
    private final SeminarDetailRepository seminarDetailRepository;
    private final InventoryGoalRepository inventoryGoalRepository;

    @Transactional(readOnly = true)
    public byte[] generateInventoryReport(Long inventoryId, User loginUser) {
        Inventory inv = inventoryRepository.findByIdWithAssociations(inventoryId.intValue())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "INVENTORY_NOT_FOUND"));

        checkAccess(inv, loginUser);

        int id = inventoryId.intValue();
        List<ItSkillDetail> itSkillDetails = itSkillDetailRepository.findByInventoryIdWithCategories(id);
        List<SeminarDetail> seminarDetails = seminarDetailRepository.findByInventoryId(id);
        List<InventoryGoal> goals = inventoryGoalRepository.findByInventoryIdForReport(id);

        Map<String, Object> params = new HashMap<>();
        params.put("USER_NAME", inv.getUser().getName());
        params.put("AD_SEMINAR_GOAL_DATA", new JRBeanCollectionDataSource(buildAdSeminarGoalRows(goals)));
        params.put("AD_SEMINAR_ACTUAL_DATA", new JRBeanCollectionDataSource(buildAdSeminarActualRows(seminarDetails)));
        params.put("IT_SKILL_GOAL_DATA", new JRBeanCollectionDataSource(buildItSkillGoalRows(goals)));
        params.put("IT_SKILL_ACTUAL_DATA", new JRBeanCollectionDataSource(buildItSkillActualRows(itSkillDetails)));

        try (InputStream jrxml = getClass().getResourceAsStream("/reports/inventory/inventoryReport.jrxml")) {
            if (jrxml == null) {
                log.error("帳票テンプレートが見つかりません: /reports/inventory/inventoryReport.jrxml");
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "REPORT_TEMPLATE_NOT_FOUND");
            }
            log.debug("帳票コンパイル開始 inventoryId={}", inventoryId);
            JasperReport report = JasperCompileManager.compileReport(jrxml);
            log.debug("帳票フィル開始 inventoryId={}", inventoryId);
            JasperPrint print = JasperFillManager.getInstance(jasperCtx).fill(report, params, new JREmptyDataSource());
            log.debug("PDF エクスポート開始 inventoryId={}", inventoryId);
            JRPdfExporter exporter = new JRPdfExporter(jasperCtx);
            exporter.setExporterInput(new SimpleExporterInput(print));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(baos));
            exporter.exportReport();
            log.info("帳票生成完了 inventoryId={}", inventoryId);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("帳票生成エラー inventoryId={}", inventoryId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "REPORT_GENERATION_ERROR");
        }
    }

    private void checkAccess(Inventory inv, User loginUser) {
        if (!inv.getUser().getId().equals(loginUser.getId())) {
            String role = loginUser.getRole().name();
            if (!"TL".equals(role) && !"ADMIN".equals(role)) {
                throw new AuthException("FORBIDDEN", "");
            }
        }
    }

    private List<AdSeminarGoalRow> buildAdSeminarGoalRows(List<InventoryGoal> goals) {
        return goals.stream()
                .filter(g -> g.getGoalCategory() == GoalCategory.AD && g.getAdSeminar() != null)
                .map(g -> new AdSeminarGoalRow(
                        g.getAdSeminar().getCategory() != null ? g.getAdSeminar().getCategory().getName() : "",
                        g.getAdSeminar().getName() != null ? g.getAdSeminar().getName() : "",
                        g.getTargetPeriod() != null ? g.getTargetPeriod().getMonthValue() : null,
                        g.getReason() != null ? g.getReason() : ""))
                .toList();
    }

    private List<AdSeminarActualRow> buildAdSeminarActualRows(List<SeminarDetail> seminarDetails) {
        return seminarDetails.stream()
                .filter(d -> d.getAdSeminar() != null)
                .map(d -> new AdSeminarActualRow(
                        d.getAdSeminar().getCategory() != null ? d.getAdSeminar().getCategory().getName() : "",
                        d.getAdSeminar().getName() != null ? d.getAdSeminar().getName() : "",
                        d.getAttendedYearMonth() != null ? d.getAttendedYearMonth().getMonthValue() : null,
                        d.getRemarks() != null ? d.getRemarks() : ""))
                .toList();
    }

    private List<ItSkillGoalRow> buildItSkillGoalRows(List<InventoryGoal> goals) {
        return goals.stream()
                .filter(g -> g.getGoalCategory() == GoalCategory.IT_SKILL)
                .map(g -> {
                    String cat1 = "";
                    String cat2 = "";
                    String skillName;
                    if (g.getItSkill() != null) {
                        ItSkillCategory cat = g.getItSkill().getCategory();
                        if (cat != null) {
                            if (cat.getParent() != null) {
                                cat1 = cat.getParent().getName();
                                cat2 = cat.getName();
                            } else {
                                cat1 = cat.getName();
                            }
                        }
                        skillName = g.getItSkill().getName();
                    } else {
                        skillName = g.getCustomName() != null ? g.getCustomName() : "";
                    }
                    return new ItSkillGoalRow(cat1, cat2, skillName,
                            g.getTargetPeriod() != null ? g.getTargetPeriod().getMonthValue() : null,
                            g.getReason() != null ? g.getReason() : "");
                })
                .toList();
    }

    private List<ItSkillActualRow> buildItSkillActualRows(List<ItSkillDetail> details) {
        return details.stream()
                .map(d -> {
                    String cat1 = "";
                    String cat2 = "";
                    String skillName;
                    if (d.getItSkill() != null) {
                        ItSkillCategory cat = d.getItSkill().getCategory();
                        if (cat != null) {
                            if (cat.getParent() != null) {
                                cat1 = cat.getParent().getName();
                                cat2 = cat.getName();
                            } else {
                                cat1 = cat.getName();
                            }
                        }
                        skillName = d.getItSkill().getName();
                    } else {
                        skillName = d.getCustomSkillName() != null ? d.getCustomSkillName() : "";
                    }
                    return new ItSkillActualRow(cat1, cat2, skillName,
                            d.getSkillLevel() != null ? d.getSkillLevel().getLevelValue().intValue() : null,
                            d.getRemarks() != null ? d.getRemarks() : "");
                })
                .toList();
    }
}
