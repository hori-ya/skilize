/**************************************************************************************************************
 * 機能ID      ：MST
 * 機能名      ：マスタ管理
 * 作成日      ：2026/06/08
 * 作成者      ：hori-ya
 * ----------------------------------------------------------------------------------------------------------
 * 機能概要：
 * 資格マスタ ExcelファイルをパースしてカテゴリRow・資格Rowの中間データとして返すコンポーネント。
 * バリデーションはMasterExcelServiceで行う。
 * ----------------------------------------------------------------------------------------------------------
 * 更新履歴：
 * 2026/06/08 hori-ya 初版作成
 * ----------------------------------------------------------------------------------------------------------
 * Copyright (C) 2026 Skilize Project. All Rights Reserved.
 **************************************************************************************************************/
package com.skilize.master.infrastructure.excel;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static com.skilize.master.infrastructure.excel.ExcelCellReader.*;

/** 参考資格マスタ Excel ファイルをパースして中間データを返す。 */
@Slf4j
@Component
public class QualificationExcelImporter {

    private static final String SHEET_CATEGORY = "資格カテゴリ";
    private static final String SHEET_QUAL = "参考資格";

    public record CategoryRow(Integer id, String name, Boolean active, int rowNum, int siblingOrder) {}
    public record QualificationRow(Integer id, Integer categoryId, String name, String description,
                                   Boolean active, int rowNum, int siblingOrder) {}
    public record QualificationImportData(List<CategoryRow> categoryRows, List<QualificationRow> qualRows) {}

    /**
     * 資格マスタExcelファイルをパースして中間データを返す。
     * ファイル形式不正の場合は ExcelFormatException をスローする。
     *
     * @param file アップロードされたExcelファイル（.xlsx）
     * @return パース済み中間データ（資格カテゴリ行リスト・資格行リスト）
     */
    public QualificationImportData parse(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             XSSFWorkbook wb = new XSSFWorkbook(is)) {
            Sheet catSheet = wb.getSheet(SHEET_CATEGORY);
            Sheet qualSheet = wb.getSheet(SHEET_QUAL);
            if (catSheet == null) throw new ExcelFormatException("EXCEL_SHEET_NOT_FOUND");
            if (qualSheet == null) throw new ExcelFormatException("EXCEL_SHEET_NOT_FOUND");
            return new QualificationImportData(parseCategories(catSheet), parseQualifications(qualSheet));
        } catch (ExcelFormatException e) {
            throw e;
        } catch (IOException e) {
            log.error("Excel file read failed", e);
            throw new ExcelFormatException("EXCEL_READ_ERROR", e);
        } catch (Exception e) {
            log.error("Excel file parse failed", e);
            throw new ExcelFormatException("EXCEL_INVALID_FORMAT", e);
        }
    }

    private List<CategoryRow> parseCategories(Sheet sheet) {
        List<CategoryRow> rows = new ArrayList<>();
        int counter = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (isEmptyRow(row, 3)) continue;
            Integer id = getInt(row, 0);
            String name = getString(row, 1);
            Boolean active = getActive(row, 2);
            rows.add(new CategoryRow(id, name, active, i + 1, ++counter));
        }
        return rows;
    }

    private List<QualificationRow> parseQualifications(Sheet sheet) {
        // レイアウト: A=カテゴリID, B=ID, C=カテゴリ名(参考), D=資格名, E=説明, F=有効
        Map<Integer, Integer> siblingCounters = new HashMap<>();
        List<QualificationRow> rows = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (isEmptyRow(row, 6)) continue;
            Integer categoryId = getInt(row, 0);
            Integer id = getInt(row, 1);
            // C(2) はカテゴリ名参考列なので読み飛ばす
            String name = getString(row, 3);
            String description = getString(row, 4);
            Boolean active = getActive(row, 5);
            int order = siblingCounters.merge(categoryId, 1, Integer::sum);
            rows.add(new QualificationRow(id, categoryId, name,
                    description.isEmpty() ? null : description, active, i + 1, order));
        }
        return rows;
    }
}
