package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.AlertaDTO;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class AlertaExcelServiceTest {

    private final AlertaExcelService excelService = new AlertaExcelService();

    // ═══════════════════════════════════════════════════
    // PRUEBA 1: Genera Excel con titulo, metadata y filas de alertas
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe generar un Excel con titulo, metadata y una fila por alerta")
    void cuandoGeneraExcel_debeCrearTituloYFilasDeDatos() throws IOException {
        AlertaDTO alerta = new AlertaDTO(
                1, 100L, 10, "LATAM", "LIM", "CUZ",
                "2026-07-01", "08:30", "basica",
                200.0, 250.0, "987654321", true,
                "2026-06-23T10:00:00", "Activa"
        );

        byte[] excel = excelService.generar(List.of(alerta), "Ana Garcia");

        assertThat(excel).isNotEmpty();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            Sheet sheet = workbook.getSheetAt(0);

            Row filaTitulo = sheet.getRow(0);
            assertThat(filaTitulo.getCell(0).getStringCellValue()).contains("PasajeYa");

            Row filaMeta = sheet.getRow(1);
            assertThat(filaMeta.getCell(0).getStringCellValue()).contains("Ana Garcia");

            Row filaDatos = sheet.getRow(4);
            assertThat(filaDatos.getCell(1).getStringCellValue()).isEqualTo("LATAM");
            assertThat(filaDatos.getCell(2).getStringCellValue()).isEqualTo("LIM");
            assertThat(filaDatos.getCell(9).getStringCellValue()).isEqualTo("Activa");
        }
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 2: Alerta pausada debe marcarse como "Pausada" en el Excel
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe marcar el estado Pausada cuando la alerta no esta activa")
    void cuandoAlertaPausada_debeMostrarEstadoPausada() throws IOException {
        AlertaDTO alertaPausada = new AlertaDTO(
                2, 101L, 11, "Sky", "LIM", "AQP",
                "2026-07-02", "10:00", "flex",
                300.0, null, "912345678", false,
                "2026-06-20T09:00:00", "Pausada"
        );

        byte[] excel = excelService.generar(List.of(alertaPausada), "Luis Torres");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row filaDatos = sheet.getRow(4);
            assertThat(filaDatos.getCell(9).getStringCellValue()).isEqualTo("Pausada");
            assertThat(filaDatos.getCell(8).getStringCellValue()).isEqualTo("—");
        }
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 3: Lista vacia genera Excel sin filas de datos
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe generar el Excel correctamente cuando no hay alertas")
    void cuandoListaVacia_debeGenerarExcelSinFilasDeDatos() throws IOException {
        byte[] excel = excelService.generar(List.of(), "Ana Garcia");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row filaTotales = sheet.getRow(4);
            assertThat(filaTotales.getCell(0).getStringCellValue()).contains("TOTAL: 0");
        }
    }
}
