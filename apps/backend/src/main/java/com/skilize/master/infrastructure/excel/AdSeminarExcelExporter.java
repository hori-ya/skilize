package com.skilize.master.infrastructure.excel;

import com.skilize.master.domain.AdSeminar;
import com.skilize.master.domain.AdSeminarCategory;
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
            setString(row, 2, cat.isActive() ? "有効" : "無効", normalStyle);
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
            setIntOrBlank(row, 0, s.getCategory() != null ? s.getCategory().getId() : null, normalStyle);
            setString(row, 1, s.getCategory() != null ? s.getCategory().getName() : "", refStyle);
            setInt(row, 2, s.getId(), normalStyle);
            setString(row, 3, s.getName(), normalStyle);
            setString(row, 4, s.getDescription() != null ? s.getDescription() : "", normalStyle);
            setString(row, 5, s.isActive() ? "有効" : "無効", normalStyle);
        }
    }
}
