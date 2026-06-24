package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.VueloDTO;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class VueloExcelServiceTest {

    private final VueloExcelService excelService = new VueloExcelService();

    // ═══════════════════════════════════════════════════
    // PRUEBA 1: Genera un Excel con encabezado y una fila por vuelo
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe generar un Excel con encabezado y una fila de datos por cada vuelo")
    void cuandoGeneraExcel_debeCrearEncabezadoYFilasDeDatos() throws IOException {
        VueloDTO vuelo = new VueloDTO(
                100L, "LATAM", "LIM", "CUZ", "2026-07-01",
                "08:30", "09:50", "1h 20m", 250.0, "basica",
                false, 0, 8, false, false, "verde", "https://latam.com"
        );

        byte[] excel = excelService.generarExcel(List.of(vuelo));

        assertThat(excel).isNotEmpty();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row encabezado = sheet.getRow(0);
            assertThat(encabezado.getCell(0).getStringCellValue()).isEqualTo("Aerolínea");

            Row fila = sheet.getRow(1);
            assertThat(fila.getCell(0).getStringCellValue()).isEqualTo("LATAM");
            assertThat(fila.getCell(1).getStringCellValue()).isEqualTo("LIM");
            assertThat(fila.getCell(7).getNumericCellValue()).isEqualTo(250.0);
        }
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 2: Sin vuelos genera un Excel solo con encabezado
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe generar un Excel solo con encabezado cuando la lista esta vacia")
    void cuandoListaVacia_debeGenerarSoloEncabezado() throws IOException {
        byte[] excel = excelService.generarExcel(List.of());

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0)).isNotNull();
            assertThat(sheet.getRow(1)).isNull();
        }
    }
}
