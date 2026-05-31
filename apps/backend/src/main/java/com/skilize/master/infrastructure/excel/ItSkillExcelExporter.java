package com.skilize.master.infrastructure.excel;

import com.skilize.master.domain.ItSkill;
import com.skilize.master.domain.ItSkillCategory;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/** ITスキルマスタ（IT分類 / ITスキル）を Excel ファイルとして出力する。 */
@Component
public class ItSkillExcelExporter {

    /**
     * 全カテゴリ・全スキルを含む xlsx バイト列を返す。
     * categories は findAllByOrderByLevelAscParentIdAscSortOrderAsc() の順、
     * skills は findAllOrderByHierarchy() の順で渡すこと。
     */
    public byte[] export(List<ItSkillCategory> categories, List<ItSkill> skills) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFCellStyle headerStyle = ExcelStyleHelper.createHeaderStyle(wb);
            XSSFCellStyle refStyle = ExcelStyleHelper.createRefStyle(wb);
            XSSFCellStyle normalStyle = ExcelStyleHelper.createNormalStyle(wb);

            writeCategories(wb, headerStyle, refStyle, normalStyle, categories);
            writeSkills(wb, headerStyle, refStyle, normalStyle, skills);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Excel 生成に失敗しました", e);
        }
    }

    private void writeCategories(XSSFWorkbook wb, XSSFCellStyle headerStyle,
                                  XSSFCellStyle refStyle, XSSFCellStyle normalStyle,
                                  List<ItSkillCategory> categories) {
        Sheet sheet = wb.createSheet("IT分類");
        sheet.setColumnWidth(0, 10 * 256); // ID
        sheet.setColumnWidth(1, 14 * 256); // 親カテゴリID
        sheet.setColumnWidth(2, 12 * 256); // 階層レベル (参考)
        sheet.setColumnWidth(3, 30 * 256); // カテゴリ名
        sheet.setColumnWidth(4, 10 * 256); // 有効

        // ヘッダー行
        Row header = sheet.createRow(0);
        String[] headers = {"ID", "親カテゴリID", "階層レベル", "カテゴリ名", "有効"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            // 参考列 (C = index 2) のみ参考スタイル、他はヘッダースタイル
            cell.setCellStyle(i == 2 ? refStyle : headerStyle);
        }
        // ヘッダーの参考列ラベルにもヘッダー背景を重ねる（視認性のためヘッダースタイルで上書き）
        header.getCell(2).setCellStyle(headerStyle); // 階層レベルもヘッダー色

        int rowIdx = 1;
        for (ItSkillCategory cat : categories) {
            Row row = sheet.createRow(rowIdx++);
            setInt(row, 0, cat.getId(), normalStyle);
            setIntOrBlank(row, 1, cat.getParentId(), normalStyle);
            setInt(row, 2, (int) cat.getLevel(), refStyle);  // 参考列
            setString(row, 3, cat.getName(), normalStyle);
            setString(row, 4, cat.isActive() ? "有効" : "無効", normalStyle);
        }
    }

    private void writeSkills(XSSFWorkbook wb, XSSFCellStyle headerStyle,
                              XSSFCellStyle refStyle, XSSFCellStyle normalStyle,
                              List<ItSkill> skills) {
        Sheet sheet = wb.createSheet("ITスキル");
        sheet.setColumnWidth(0, 12 * 256); // カテゴリID
        sheet.setColumnWidth(1, 20 * 256); // 大分類 (参考)
        sheet.setColumnWidth(2, 20 * 256); // 中分類 (参考)
        sheet.setColumnWidth(3, 20 * 256); // 小分類 (参考)
        sheet.setColumnWidth(4, 10 * 256); // ID
        sheet.setColumnWidth(5, 30 * 256); // スキル名
        sheet.setColumnWidth(6, 40 * 256); // 説明
        sheet.setColumnWidth(7, 10 * 256); // 有効

        // ヘッダー行
        Row header = sheet.createRow(0);
        String[] headers = {"カテゴリID", "大分類", "中分類", "小分類", "ID", "スキル名", "説明", "有効"};
        XSSFCellStyle[] styles = {headerStyle, headerStyle, headerStyle, headerStyle,
                headerStyle, headerStyle, headerStyle, headerStyle};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles[i]);
        }

        int rowIdx = 1;
        for (ItSkill skill : skills) {
            Row row = sheet.createRow(rowIdx++);
            setInt(row, 0, skill.getCategory().getId(), normalStyle);
            setString(row, 1, resolveCategory1Name(skill), refStyle);  // 参考
            setString(row, 2, resolveCategory2Name(skill), refStyle);  // 参考
            setString(row, 3, resolveCategory3Name(skill), refStyle);  // 参考
            setInt(row, 4, skill.getId(), normalStyle);
            setString(row, 5, skill.getName(), normalStyle);
            setString(row, 6, skill.getDescription() != null ? skill.getDescription() : "", normalStyle);
            setString(row, 7, skill.isActive() ? "有効" : "無効", normalStyle);
        }
    }

    // 大分類名（L1）の解決
    private String resolveCategory1Name(ItSkill skill) {
        ItSkillCategory cat = skill.getCategory();
        return switch (cat.getLevel()) {
            case 1 -> cat.getName();
            case 2 -> cat.getParent() != null ? cat.getParent().getName() : "";
            case 3 -> {
                ItSkillCategory p = cat.getParent();
                yield (p != null && p.getParent() != null) ? p.getParent().getName() : "";
            }
            default -> "";
        };
    }

    // 中分類名（L2）の解決
    private String resolveCategory2Name(ItSkill skill) {
        ItSkillCategory cat = skill.getCategory();
        return switch (cat.getLevel()) {
            case 2 -> cat.getName();
            case 3 -> cat.getParent() != null ? cat.getParent().getName() : "";
            default -> "";
        };
    }

    // 小分類名（L3）の解決
    private String resolveCategory3Name(ItSkill skill) {
        ItSkillCategory cat = skill.getCategory();
        return cat.getLevel() == 3 ? cat.getName() : "";
    }

    private void setInt(Row row, int col, int value, CellStyle style) {
        Cell cell = row.createCell(col, CellType.NUMERIC);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void setIntOrBlank(Row row, int col, Integer value, CellStyle style) {
        if (value == null) {
            Cell cell = row.createCell(col, CellType.BLANK);
            cell.setCellStyle(style);
        } else {
            setInt(row, col, value, style);
        }
    }

    private void setString(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col, CellType.STRING);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }
}
