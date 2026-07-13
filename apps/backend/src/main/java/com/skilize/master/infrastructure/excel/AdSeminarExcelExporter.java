/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * ADセミナーマスタ（ADカテゴリ・ADセミナー）をExcelファイル（.xlsx）として出力するコンポーネント。
 * ADカテゴリシートとADセミナーシートの2シートを生成する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.excel;

import com.skilize.master.domain.model.AdSeminar;
import com.skilize.master.domain.model.AdSeminarCategory;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static com.skilize.master.infrastructure.excel.ExcelCellWriter.*;

/** ADマスタ（ADカテゴリ / ADセミナー）を Excel ファイルとして出力する。 */
@Component
public class AdSeminarExcelExporter {

    /**
     * ADセミナーマスタをExcelファイルとして出力する。
     * ADカテゴリシート・ADセミナーシートの2シートを生成して返す。
     *
     * @param categories ADセミナー分類一覧
     * @param seminars   ADセミナー一覧
     * @return Excelファイルのバイト配列（.xlsx 形式）
     */
    public byte[] export(List<AdSeminarCategory> categories, List<AdSeminar> seminars) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFCellStyle headerStyle = ExcelStyleHelper.createHeaderStyle(wb);
            XSSFCellStyle refStyle = ExcelStyleHelper.createRefStyle(wb);
            XSSFCellStyle normalStyle = ExcelStyleHelper.createNormalStyle(wb);

            writeCategories(wb, headerStyle, normalStyle, categories);
            writeSeminars(wb, headerStyle, refStyle, normalStyle, seminars);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Excel 生成に失敗しました", e);
        }
    }

    private void writeCategories(XSSFWorkbook wb, XSSFCellStyle headerStyle,
                                  XSSFCellStyle normalStyle, List<AdSeminarCategory> categories) {
        Sheet sheet = wb.createSheet("ADカテゴリ");
        sheet.setColumnWidth(0, 10 * 256);
        sheet.setColumnWidth(1, 30 * 256);
        sheet.setColumnWidth(2, 10 * 256);

        Row header = sheet.createRow(0);
        setHeader(header, 0, "ID", headerStyle);
        setHeader(header, 1, "カテゴリ名", headerStyle);
        setHeader(header, 2, "有効", headerStyle);

        int rowIdx = 1;
        for (AdSeminarCategory cat : categories) {
            Row row = sheet.createRow(rowIdx++);
            setInt(row, 0, cat.getId(), normalStyle);
            setString(row, 1, cat.getName(), normalStyle);
            String activeLabel = "無効";
            if (cat.isActive()) {
                activeLabel = "有効";
            }
            setString(row, 2, activeLabel, normalStyle);
        }
    }

    private void writeSeminars(XSSFWorkbook wb, XSSFCellStyle headerStyle,
                                XSSFCellStyle refStyle, XSSFCellStyle normalStyle,
                                List<AdSeminar> seminars) {
        Sheet sheet = wb.createSheet("ADセミナー");
        sheet.setColumnWidth(0, 12 * 256);
        sheet.setColumnWidth(1, 25 * 256);
        sheet.setColumnWidth(2, 10 * 256);
        sheet.setColumnWidth(3, 40 * 256);
        sheet.setColumnWidth(4, 50 * 256);
        sheet.setColumnWidth(5, 10 * 256);

        Row header = sheet.createRow(0);
        setHeader(header, 0, "カテゴリID", headerStyle);
        setHeader(header, 1, "カテゴリ名(参考)", refStyle);
        setHeader(header, 2, "ID", headerStyle);
        setHeader(header, 3, "AD名", headerStyle);
        setHeader(header, 4, "説明", headerStyle);
        setHeader(header, 5, "有効", headerStyle);

        int rowIdx = 1;
        for (AdSeminar s : seminars) {
            Row row = sheet.createRow(rowIdx++);
            Integer categoryId = null;
            String categoryName = "";
            if (s.getCategory() != null) {
                categoryId = s.getCategory().getId();
                categoryName = s.getCategory().getName();
            }
            setIntOrBlank(row, 0, categoryId, normalStyle);
            setString(row, 1, categoryName, refStyle);
            setInt(row, 2, s.getId(), normalStyle);
            setString(row, 3, s.getName(), normalStyle);
            String description = "";
            if (s.getDescription() != null) {
                description = s.getDescription();
            }
            setString(row, 4, description, normalStyle);
            String activeLabel = "無効";
            if (s.isActive()) {
                activeLabel = "有効";
            }
            setString(row, 5, activeLabel, normalStyle);
        }
    }
}
