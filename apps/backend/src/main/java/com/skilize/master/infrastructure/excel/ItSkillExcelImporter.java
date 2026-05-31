package com.skilize.master.infrastructure.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/** ITスキルマスタ Excel ファイルをパースして中間データを返す。 */
@Component
public class ItSkillExcelImporter {

    private static final String SHEET_CATEGORY = "IT分類";
    private static final String SHEET_SKILL = "ITスキル";

    public record CategoryRow(Integer id, Integer parentId, String name, Boolean active, int rowNum, int siblingOrder) {}
    public record SkillRow(Integer id, Integer categoryId, String name, String description, Boolean active, int rowNum, int siblingOrder) {}
    public record ItSkillImportData(List<CategoryRow> categoryRows, List<SkillRow> skillRows) {}

    public ItSkillImportData parse(MultipartFile file) {
        try (XSSFWorkbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet catSheet = wb.getSheet(SHEET_CATEGORY);
            Sheet skillSheet = wb.getSheet(SHEET_SKILL);
            if (catSheet == null) throw new ExcelFormatException("シート「" + SHEET_CATEGORY + "」が見つかりません");
            if (skillSheet == null) throw new ExcelFormatException("シート「" + SHEET_SKILL + "」が見つかりません");

            return new ItSkillImportData(parseCategories(catSheet), parseSkills(skillSheet));
        } catch (ExcelFormatException e) {
            throw e;
        } catch (IOException e) {
            throw new ExcelFormatException("Excel ファイルの読み込みに失敗しました: " + e.getMessage());
        } catch (Exception e) {
            throw new ExcelFormatException("Excel ファイルの形式が不正です: " + e.getMessage());
        }
    }

    private List<CategoryRow> parseCategories(Sheet sheet) {
        // parentId → 次の siblingOrder カウンター (null はキーとして使用可)
        Map<Integer, Integer> siblingCounters = new HashMap<>();

        List<CategoryRow> rows = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (isEmptyRow(row, 5)) continue;

            Integer id = getIntValue(row, 0);
            Integer parentId = getIntValue(row, 1);
            // C列(2)は参考列(階層レベル)なので読み飛ばす
            String name = getStringValue(row, 3);
            Boolean active = getActiveValue(row, 4);

            int order = siblingCounters.merge(parentId, 1, Integer::sum);
            rows.add(new CategoryRow(id, parentId, name, active, i + 1, order));
        }
        return rows;
    }

    private List<SkillRow> parseSkills(Sheet sheet) {
        // レイアウト: A=カテゴリID, B=大分類(参考), C=中分類(参考), D=小分類(参考), E=ID, F=スキル名, G=説明, H=有効
        Map<Integer, Integer> siblingCounters = new HashMap<>();

        List<SkillRow> rows = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (isEmptyRow(row, 8)) continue;

            Integer categoryId = getIntValue(row, 0);
            // B,C,D (1,2,3) は参考列なので読み飛ばす
            Integer id = getIntValue(row, 4);
            String name = getStringValue(row, 5);
            String description = getStringValue(row, 6);
            Boolean active = getActiveValue(row, 7);

            int order = siblingCounters.merge(categoryId, 1, Integer::sum);
            rows.add(new SkillRow(id, categoryId, name, description.isEmpty() ? null : description,
                    active, i + 1, order));
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

    static Integer getIntValue(Row row, int col) {
        return getIntValue(row.getCell(col));
    }

    static Integer getIntValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> {
                String s = cell.getStringCellValue().trim();
                try { yield s.isEmpty() ? null : Integer.parseInt(s); }
                catch (NumberFormatException e) { yield null; }
            }
            default -> null;
        };
    }

    static String getStringValue(Row row, int col) {
        return getStringValue(row.getCell(col));
    }

    static String getStringValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    static Boolean getActiveValue(Row row, int col) {
        String val = getStringValue(row, col);
        if ("有効".equals(val)) return Boolean.TRUE;
        if ("無効".equals(val)) return Boolean.FALSE;
        return null;
    }
}
