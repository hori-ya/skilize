/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 資格マスタ（資格カテゴリ・参考資格）をExcelファイル（.xlsx）として出力するコンポーネント。
 * 資格カテゴリシートと参考資格シートの2シートを生成する。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.excel;

import com.skilize.master.domain.model.Qualification;
import com.skilize.master.domain.model.QualificationCategory;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static com.skilize.master.infrastructure.excel.ExcelCellWriter.*;

/** 参考資格マスタ（資格カテゴリ / 参考資格）を Excel ファイルとして出力する。 */
@Component
public class QualificationExcelExporter {

    /**
     * 資格マスタをExcelファイルとして出力する。
     * 資格カテゴリシート・参考資格シートの2シートを生成して返す。
     *
     * @param categories    資格カテゴリ一覧
     * @param qualifications 資格一覧
     * @return Excelファイルのバイト配列（.xlsx 形式）
     */
    public byte[] export(List<QualificationCategory> categories, List<Qualification> qualifications) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFCellStyle headerStyle = ExcelStyleHelper.createHeaderStyle(wb);
            XSSFCellStyle refStyle = ExcelStyleHelper.createRefStyle(wb);
            XSSFCellStyle normalStyle = ExcelStyleHelper.createNormalStyle(wb);

            writeCategories(wb, headerStyle, normalStyle, categories);
            writeQualifications(wb, headerStyle, refStyle, normalStyle, qualifications);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Excel 生成に失敗しました", e);
        }
    }

    private void writeCategories(XSSFWorkbook wb, XSSFCellStyle headerStyle,
                                  XSSFCellStyle normalStyle, List<QualificationCategory> categories) {
        Sheet sheet = wb.createSheet("資格カテゴリ");
        sheet.setColumnWidth(0, 10 * 256);
        sheet.setColumnWidth(1, 30 * 256);
        sheet.setColumnWidth(2, 10 * 256);

        Row header = sheet.createRow(0);
        setHeader(header, 0, "ID", headerStyle);
        setHeader(header, 1, "カテゴリ名", headerStyle);
        setHeader(header, 2, "有効", headerStyle);

        int rowIdx = 1;
        for (QualificationCategory cat : categories) {
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

    private void writeQualifications(XSSFWorkbook wb, XSSFCellStyle headerStyle,
                                      XSSFCellStyle refStyle, XSSFCellStyle normalStyle,
                                      List<Qualification> qualifications) {
        Sheet sheet = wb.createSheet("参考資格");
        sheet.setColumnWidth(0, 12 * 256);
        sheet.setColumnWidth(1, 10 * 256);
        sheet.setColumnWidth(2, 25 * 256);
        sheet.setColumnWidth(3, 30 * 256);
        sheet.setColumnWidth(4, 40 * 256);
        sheet.setColumnWidth(5, 10 * 256);

        Row header = sheet.createRow(0);
        setHeader(header, 0, "カテゴリID", headerStyle);
        setHeader(header, 1, "ID", headerStyle);
        setHeader(header, 2, "カテゴリ名(参考)", refStyle);
        setHeader(header, 3, "資格名", headerStyle);
        setHeader(header, 4, "説明", headerStyle);
        setHeader(header, 5, "有効", headerStyle);

        int rowIdx = 1;
        for (Qualification q : qualifications) {
            Row row = sheet.createRow(rowIdx++);
            Integer categoryId = null;
            String categoryName = "";
            if (q.getCategory() != null) {
                categoryId = q.getCategory().getId();
                categoryName = q.getCategory().getName();
            }
            setIntOrBlank(row, 0, categoryId, normalStyle);
            setInt(row, 1, q.getId(), normalStyle);
            setString(row, 2, categoryName, refStyle);
            setString(row, 3, q.getName(), normalStyle);
            String description = "";
            if (q.getDescription() != null) {
                description = q.getDescription();
            }
            setString(row, 4, description, normalStyle);
            String activeLabel = "無効";
            if (q.isActive()) {
                activeLabel = "有効";
            }
            setString(row, 5, activeLabel, normalStyle);
        }
    }
}
