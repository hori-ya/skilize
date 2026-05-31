package com.skilize.master.infrastructure.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static com.skilize.master.infrastructure.excel.ItSkillExcelImporter.*;

/** 参考資格マスタ Excel ファイルをパースして中間データを返す。 */
@Component
public class QualificationExcelImporter {

    private static final String SHEET_CATEGORY = "資格カテゴリ";
    private static final String SHEET_QUAL = "参考資格";

    public record CategoryRow(Integer id, String name, Boolean active, int rowNum, int siblingOrder) {}
    public record QualificationRow(Integer id, Integer categoryId, String name, String description,
                                   Boolean active, int rowNum, int siblingOrder) {}
    public record QualificationImportData(List<CategoryRow> categoryRows, List<QualificationRow> qualRows) {}

    public QualificationImportData parse(MultipartFile file) {
        try (XSSFWorkbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet catSheet = wb.getSheet(SHEET_CATEGORY);
            Sheet qualSheet = wb.getSheet(SHEET_QUAL);
            if (catSheet == null) throw new ExcelFormatException("シート「" + SHEET_CATEGORY + "」が見つかりません");
            if (qualSheet == null) throw new ExcelFormatException("シート「" + SHEET_QUAL + "」が見つかりません");

            return new QualificationImportData(parseCategories(catSheet), parseQualifications(qualSheet));
        } catch (ExcelFormatException e) {
            throw e;
        } catch (IOException e) {
            throw new ExcelFormatException("Excel ファイルの読み込みに失敗しました: " + e.getMessage());
        } catch (Exception e) {
            throw new ExcelFormatException("Excel ファイルの形式が不正です: " + e.getMessage());
        }
    }

    private List<CategoryRow> parseCategories(Sheet sheet) {
        List<CategoryRow> rows = new ArrayList<>();
        int counter = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (isEmptyRow(row, 3)) continue;

            Integer id = getIntValue(row, 0);
            String name = getStringValue(row, 1);
            Boolean active = getActiveValue(row, 2);

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

            Integer categoryId = getIntValue(row, 0);
            Integer id = getIntValue(row, 1);
            // C(2) はカテゴリ名参考列なので読み飛ばす
            String name = getStringValue(row, 3);
            String description = getStringValue(row, 4);
            Boolean active = getActiveValue(row, 5);

            int order = siblingCounters.merge(categoryId, 1, Integer::sum);
            rows.add(new QualificationRow(id, categoryId, name,
                    description.isEmpty() ? null : description, active, i + 1, order));
        }
        return rows;
    }

    private boolean isEmptyRow(Row row, int numCols) {
        if (row == null) return true;
        for (int i = 0; i < numCols; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                if (!getStringValue(cell).isEmpty()) return false;
            }
        }
        return true;
    }
}
