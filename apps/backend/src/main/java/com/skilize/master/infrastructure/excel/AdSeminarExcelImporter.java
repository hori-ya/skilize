package com.skilize.master.infrastructure.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static com.skilize.master.infrastructure.excel.ItSkillExcelImporter.*;

/** ADマスタ Excel ファイルをパースして中間データを返す。 */
@Component
public class AdSeminarExcelImporter {

    private static final String SHEET_CATEGORY = "ADカテゴリ";
    private static final String SHEET_SEMINAR = "ADセミナー";

    public record CategoryRow(Integer id, String name, Boolean active, int rowNum, int siblingOrder) {}
    public record SeminarRow(Integer id, Integer categoryId, String name, String description,
                             Boolean active, int rowNum, int siblingOrder) {}
    public record AdSeminarImportData(List<CategoryRow> categoryRows, List<SeminarRow> seminarRows) {}

    public AdSeminarImportData parse(MultipartFile file) {
        try (XSSFWorkbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet catSheet = wb.getSheet(SHEET_CATEGORY);
            Sheet semSheet = wb.getSheet(SHEET_SEMINAR);
            if (catSheet == null) throw new ExcelFormatException("シート「" + SHEET_CATEGORY + "」が見つかりません");
            if (semSheet == null) throw new ExcelFormatException("シート「" + SHEET_SEMINAR + "」が見つかりません");

            return new AdSeminarImportData(parseCategories(catSheet), parseSeminars(semSheet));
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

    private List<SeminarRow> parseSeminars(Sheet sheet) {
        // レイアウト: A=カテゴリID, B=カテゴリ名(参考), C=ID, D=AD名, E=説明, F=有効
        Map<Integer, Integer> siblingCounters = new HashMap<>();

        List<SeminarRow> rows = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (isEmptyRow(row, 6)) continue;

            Integer categoryId = getIntValue(row, 0);
            // B(1) はカテゴリ名参考列なので読み飛ばす
            Integer id = getIntValue(row, 2);
            String name = getStringValue(row, 3);
            String description = getStringValue(row, 4);
            Boolean active = getActiveValue(row, 5);

            int order = siblingCounters.merge(categoryId, 1, Integer::sum);
            rows.add(new SeminarRow(id, categoryId, name,
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
