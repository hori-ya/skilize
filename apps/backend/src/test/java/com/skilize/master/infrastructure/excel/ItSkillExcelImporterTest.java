package com.skilize.master.infrastructure.excel;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.*;

/**
 * ItSkillExcelImporter の単体テスト。
 * POI で xlsx をプログラム生成し、パース結果を検証する。
 */
class ItSkillExcelImporterTest {

    ItSkillExcelImporter importer;

    @BeforeEach
    void setUp() {
        importer = new ItSkillExcelImporter();
    }

    @Nested
    class 正常系 {

        @Test
        void カテゴリとスキルを正常にパースできる() throws Exception {
            byte[] bytes = buildValidExcel();
            MockMultipartFile file = new MockMultipartFile("file", "test.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

            ItSkillExcelImporter.ItSkillImportData data = importer.parse(file);

            // カテゴリ: 2行（ID付き1件 + 新規1件）
            assertThat(data.categoryRows()).hasSize(2);
            ItSkillExcelImporter.CategoryRow cat1 = data.categoryRows().get(0);
            assertThat(cat1.id()).isEqualTo(10);
            assertThat(cat1.parentId()).isNull();
            assertThat(cat1.name()).isEqualTo("プログラミング");
            assertThat(cat1.active()).isTrue();
            assertThat(cat1.siblingOrder()).isEqualTo(1); // 同parent=null内で1番目

            ItSkillExcelImporter.CategoryRow cat2 = data.categoryRows().get(1);
            assertThat(cat2.id()).isNull();
            assertThat(cat2.name()).isEqualTo("新規分類");
            assertThat(cat2.siblingOrder()).isEqualTo(2); // 同parent=null内で2番目

            // スキル: 2行
            assertThat(data.skillRows()).hasSize(2);
            ItSkillExcelImporter.SkillRow skill1 = data.skillRows().get(0);
            assertThat(skill1.id()).isEqualTo(100);
            assertThat(skill1.categoryId()).isEqualTo(10);
            assertThat(skill1.name()).isEqualTo("Java");
            assertThat(skill1.siblingOrder()).isEqualTo(1);

            ItSkillExcelImporter.SkillRow skill2 = data.skillRows().get(1);
            assertThat(skill2.id()).isNull();
            assertThat(skill2.name()).isEqualTo("新規スキル");
            assertThat(skill2.siblingOrder()).isEqualTo(2); // 同categoryId=10内で2番目
        }

        @Test
        void 空行はスキップされる() throws Exception {
            byte[] bytes = buildExcelWithEmptyRows();
            MockMultipartFile file = new MockMultipartFile("file", "test.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

            ItSkillExcelImporter.ItSkillImportData data = importer.parse(file);

            assertThat(data.categoryRows()).hasSize(1);
            assertThat(data.skillRows()).hasSize(1);
        }

        @Test
        void 有効列が無効の場合はfalseになる() throws Exception {
            byte[] bytes = buildExcelWithInactiveRows();
            MockMultipartFile file = new MockMultipartFile("file", "test.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

            ItSkillExcelImporter.ItSkillImportData data = importer.parse(file);

            assertThat(data.categoryRows().get(0).active()).isFalse();
            assertThat(data.skillRows().get(0).active()).isFalse();
        }

        @Test
        void 有効列が省略の場合はnullになる() throws Exception {
            byte[] bytes = buildExcelWithBlankActive();
            MockMultipartFile file = new MockMultipartFile("file", "test.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

            ItSkillExcelImporter.ItSkillImportData data = importer.parse(file);

            assertThat(data.categoryRows().get(0).active()).isNull();
        }
    }

    @Nested
    class 異常系 {

        @Test
        void IT分類シートが存在しない場合はExcelFormatExceptionがスローされる() throws Exception {
            byte[] bytes = buildExcelMissingCategorySheet();
            MockMultipartFile file = new MockMultipartFile("file", "test.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

            assertThatThrownBy(() -> importer.parse(file))
                    .isInstanceOf(ExcelFormatException.class)
                    .hasMessageContaining("EXCEL_SHEET_NOT_FOUND");
        }

        @Test
        void ITスキルシートが存在しない場合はExcelFormatExceptionがスローされる() throws Exception {
            byte[] bytes = buildExcelMissingSkillSheet();
            MockMultipartFile file = new MockMultipartFile("file", "test.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);

            assertThatThrownBy(() -> importer.parse(file))
                    .isInstanceOf(ExcelFormatException.class)
                    .hasMessageContaining("EXCEL_SHEET_NOT_FOUND");
        }
    }

    // ─── ヘルパー ────────────────────────────────────────────────────────────────

    private byte[] buildValidExcel() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var catSheet = wb.createSheet("IT分類");
            // ヘッダー
            var h = catSheet.createRow(0);
            h.createCell(0).setCellValue("ID");
            h.createCell(3).setCellValue("カテゴリ名");
            // row1: ID=10, 親なし, レベル(参考)=1, 名前=プログラミング, 有効
            var r1 = catSheet.createRow(1);
            r1.createCell(0).setCellValue(10);
            r1.createCell(3).setCellValue("プログラミング");
            r1.createCell(4).setCellValue("有効");
            // row2: ID空, 親なし, 名前=新規分類
            var r2 = catSheet.createRow(2);
            r2.createCell(3).setCellValue("新規分類");
            r2.createCell(4).setCellValue("有効");

            var skillSheet = wb.createSheet("ITスキル");
            // レイアウト: A=カテゴリID, B=大分類(参考), C=中分類(参考), D=小分類(参考), E=ID, F=スキル名, G=説明, H=有効
            var sh = skillSheet.createRow(0);
            sh.createCell(0).setCellValue("カテゴリID");
            sh.createCell(5).setCellValue("スキル名");
            var s1 = skillSheet.createRow(1);
            s1.createCell(0).setCellValue(10);
            s1.createCell(4).setCellValue(100);
            s1.createCell(5).setCellValue("Java");
            s1.createCell(7).setCellValue("有効");
            var s2 = skillSheet.createRow(2);
            s2.createCell(0).setCellValue(10);
            s2.createCell(5).setCellValue("新規スキル");
            s2.createCell(7).setCellValue("有効");

            return toBytes(wb);
        }
    }

    private byte[] buildExcelWithEmptyRows() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var catSheet = wb.createSheet("IT分類");
            catSheet.createRow(0); // ヘッダー
            var r1 = catSheet.createRow(1);
            r1.createCell(3).setCellValue("分類A");
            r1.createCell(4).setCellValue("有効");
            catSheet.createRow(2); // 空行

            var skillSheet = wb.createSheet("ITスキル");
            skillSheet.createRow(0);
            var s1 = skillSheet.createRow(1);
            s1.createCell(0).setCellValue(1);
            s1.createCell(5).setCellValue("スキルA");
            s1.createCell(7).setCellValue("有効");
            skillSheet.createRow(2); // 空行

            return toBytes(wb);
        }
    }

    private byte[] buildExcelWithInactiveRows() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var catSheet = wb.createSheet("IT分類");
            catSheet.createRow(0);
            var r1 = catSheet.createRow(1);
            r1.createCell(3).setCellValue("分類A");
            r1.createCell(4).setCellValue("無効");

            var skillSheet = wb.createSheet("ITスキル");
            skillSheet.createRow(0);
            var s1 = skillSheet.createRow(1);
            s1.createCell(0).setCellValue(1);
            s1.createCell(5).setCellValue("スキルA");
            s1.createCell(7).setCellValue("無効");

            return toBytes(wb);
        }
    }

    private byte[] buildExcelWithBlankActive() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var catSheet = wb.createSheet("IT分類");
            catSheet.createRow(0);
            var r1 = catSheet.createRow(1);
            r1.createCell(3).setCellValue("分類A");
            // 有効列 (E) は空白

            wb.createSheet("ITスキル").createRow(0);
            return toBytes(wb);
        }
    }

    private byte[] buildExcelMissingCategorySheet() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            wb.createSheet("ITスキル");
            return toBytes(wb);
        }
    }

    private byte[] buildExcelMissingSkillSheet() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            wb.createSheet("IT分類");
            return toBytes(wb);
        }
    }

    private byte[] toBytes(XSSFWorkbook wb) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }
}
