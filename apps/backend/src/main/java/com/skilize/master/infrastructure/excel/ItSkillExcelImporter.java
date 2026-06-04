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

/** ITスキルマスタ Excel ファイルをパースして中間データを返す。 */
@Slf4j
@Component
public class ItSkillExcelImporter {

    private static final String SHEET_CATEGORY = "IT分類";
    private static final String SHEET_SKILL = "ITスキル";

    public record CategoryRow(Integer id, Integer parentId, String name, Boolean active, int rowNum, int siblingOrder) {}
    public record SkillRow(Integer id, Integer categoryId, String name, String description, Boolean active, int rowNum, int siblingOrder) {}
    public record ItSkillImportData(List<CategoryRow> categoryRows, List<SkillRow> skillRows) {}

    public ItSkillImportData parse(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             XSSFWorkbook wb = new XSSFWorkbook(is)) {
            Sheet catSheet = wb.getSheet(SHEET_CATEGORY);
            Sheet skillSheet = wb.getSheet(SHEET_SKILL);
            if (catSheet == null) throw new ExcelFormatException("EXCEL_SHEET_NOT_FOUND");
            if (skillSheet == null) throw new ExcelFormatException("EXCEL_SHEET_NOT_FOUND");
            return new ItSkillImportData(parseCategories(catSheet), parseSkills(skillSheet));
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
        Map<Integer, Integer> siblingCounters = new HashMap<>();
        List<CategoryRow> rows = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (isEmptyRow(row, 5)) continue;
            Integer id = getInt(row, 0);
            Integer parentId = getInt(row, 1);
            // C列(2) は参考列(階層レベル)なので読み飛ばす
            String name = getString(row, 3);
            Boolean active = getActive(row, 4);
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
            Integer categoryId = getInt(row, 0);
            // B,C,D (1,2,3) は参考列なので読み飛ばす
            Integer id = getInt(row, 4);
            String name = getString(row, 5);
            String description = getString(row, 6);
            Boolean active = getActive(row, 7);
            int order = siblingCounters.merge(categoryId, 1, Integer::sum);
            rows.add(new SkillRow(id, categoryId, name, description.isEmpty() ? null : description,
                    active, i + 1, order));
        }
        return rows;
    }
}
