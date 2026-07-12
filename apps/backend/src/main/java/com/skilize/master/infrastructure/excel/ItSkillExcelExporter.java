/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ITスキルマスタ（IT分類・ITスキル）をExcelファイル（.xlsx）として出力するコンポーネント。
 * IT分類シートとITスキルシートの2シートを生成する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.excel;

import com.skilize.master.domain.model.ItSkill;
import com.skilize.master.domain.model.ItSkillCategory;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static com.skilize.master.infrastructure.excel.ExcelCellWriter.*;

/** ITスキルマスタ（IT分類 / ITスキル）を Excel ファイルとして出力する。 */
@Component
public class ItSkillExcelExporter {

    /**
     * ITスキルマスタをExcelファイルとして出力する。
     * IT分類シート・ITスキルシートの2シートを生成して返す。
     *
     * @param categories IT分類一覧（全階層）
     * @param skills     ITスキル一覧
     * @return Excelファイルのバイト配列（.xlsx 形式）
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
        sheet.setColumnWidth(0, 10 * 256);
        sheet.setColumnWidth(1, 14 * 256);
        sheet.setColumnWidth(2, 14 * 256);
        sheet.setColumnWidth(3, 30 * 256);
        sheet.setColumnWidth(4, 10 * 256);

        Row header = sheet.createRow(0);
        setHeader(header, 0, "ID", headerStyle);
        setHeader(header, 1, "親カテゴリID", headerStyle);
        setHeader(header, 2, "階層レベル(参考)", refStyle);  // 参考列はグレーヘッダー
        setHeader(header, 3, "カテゴリ名", headerStyle);
        setHeader(header, 4, "有効", headerStyle);

        int rowIdx = 1;
        for (ItSkillCategory cat : categories) {
            Row row = sheet.createRow(rowIdx++);
            setIntOrBlank(row, 0, cat.getId(), normalStyle);
            setIntOrBlank(row, 1, cat.getParentId(), normalStyle);
            setInt(row, 2, (int) cat.getLevel(), refStyle);
            setString(row, 3, cat.getName(), normalStyle);
            setString(row, 4, cat.isActive() ? "有効" : "無効", normalStyle);
        }
    }

    private void writeSkills(XSSFWorkbook wb, XSSFCellStyle headerStyle,
                              XSSFCellStyle refStyle, XSSFCellStyle normalStyle,
                              List<ItSkill> skills) {
        Sheet sheet = wb.createSheet("ITスキル");
        sheet.setColumnWidth(0, 12 * 256);
        sheet.setColumnWidth(1, 20 * 256);
        sheet.setColumnWidth(2, 20 * 256);
        sheet.setColumnWidth(3, 20 * 256);
        sheet.setColumnWidth(4, 10 * 256);
        sheet.setColumnWidth(5, 30 * 256);
        sheet.setColumnWidth(6, 40 * 256);
        sheet.setColumnWidth(7, 10 * 256);

        Row header = sheet.createRow(0);
        setHeader(header, 0, "カテゴリID", headerStyle);
        setHeader(header, 1, "大分類(参考)", refStyle);
        setHeader(header, 2, "中分類(参考)", refStyle);
        setHeader(header, 3, "小分類(参考)", refStyle);
        setHeader(header, 4, "ID", headerStyle);
        setHeader(header, 5, "スキル名", headerStyle);
        setHeader(header, 6, "説明", headerStyle);
        setHeader(header, 7, "有効", headerStyle);

        int rowIdx = 1;
        for (ItSkill skill : skills) {
            Row row = sheet.createRow(rowIdx++);
            setInt(row, 0, skill.getCategory().getId(), normalStyle);
            setString(row, 1, resolveCategory1Name(skill), refStyle);
            setString(row, 2, resolveCategory2Name(skill), refStyle);
            setString(row, 3, resolveCategory3Name(skill), refStyle);
            setIntOrBlank(row, 4, skill.getId(), normalStyle);
            setString(row, 5, skill.getName(), normalStyle);
            setString(row, 6, skill.getDescription() != null ? skill.getDescription() : "", normalStyle);
            setString(row, 7, skill.isActive() ? "有効" : "無効", normalStyle);
        }
    }

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

    private String resolveCategory2Name(ItSkill skill) {
        ItSkillCategory cat = skill.getCategory();
        return switch (cat.getLevel()) {
            case 2 -> cat.getName();
            case 3 -> cat.getParent() != null ? cat.getParent().getName() : "";
            default -> "";
        };
    }

    private String resolveCategory3Name(ItSkill skill) {
        ItSkillCategory cat = skill.getCategory();
        return cat.getLevel() == 3 ? cat.getName() : "";
    }
}
