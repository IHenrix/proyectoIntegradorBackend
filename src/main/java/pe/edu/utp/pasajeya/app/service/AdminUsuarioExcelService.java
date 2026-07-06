package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.AdminUsuarioListadoDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class AdminUsuarioExcelService {

    private static final Logger log = LoggerFactory.getLogger(AdminUsuarioExcelService.class);

    private static final String[] COLUMNAS = {
        "Email", "Nombre completo", "Rol", "Activo", "Email verificado", "Fecha registro"
    };

    public byte[] generarExcel(List<AdminUsuarioListadoDTO> filas) throws IOException {
        log.info("Generando Excel de usuarios para {} filas", filas.size());

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Usuarios");

            crearEncabezado(workbook, sheet);
            llenarDatos(workbook, sheet, filas);

            for (int i = 0; i < COLUMNAS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            log.info("Excel de usuarios generado — {} filas", filas.size());
            return out.toByteArray();
        }
    }

    private void crearEncabezado(Workbook workbook, Sheet sheet) {
        CellStyle estilo = workbook.createCellStyle();
        estilo.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estilo.setAlignment(HorizontalAlignment.CENTER);

        Font fuente = workbook.createFont();
        fuente.setColor(IndexedColors.WHITE.getIndex());
        fuente.setBold(true);
        fuente.setFontHeightInPoints((short) 11);
        estilo.setFont(fuente);

        Row fila = sheet.createRow(0);
        for (int i = 0; i < COLUMNAS.length; i++) {
            Cell celda = fila.createCell(i);
            celda.setCellValue(COLUMNAS[i]);
            celda.setCellStyle(estilo);
        }
    }

    private void llenarDatos(Workbook workbook, Sheet sheet, List<AdminUsuarioListadoDTO> filas) {
        CellStyle estiloAlternado = workbook.createCellStyle();
        estiloAlternado.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        estiloAlternado.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        int numFila = 1;
        for (AdminUsuarioListadoDTO u : filas) {
            Row fila = sheet.createRow(numFila);

            if (numFila % 2 == 0) {
                for (int i = 0; i < COLUMNAS.length; i++) {
                    fila.createCell(i).setCellStyle(estiloAlternado);
                }
            }

            fila.createCell(0).setCellValue(u.email());
            fila.createCell(1).setCellValue(u.nombreCompleto());
            fila.createCell(2).setCellValue(u.rol());
            fila.createCell(3).setCellValue(Boolean.TRUE.equals(u.activo()) ? "Sí" : "No");
            fila.createCell(4).setCellValue(Boolean.TRUE.equals(u.emailVerificado()) ? "Sí" : "No");
            fila.createCell(5).setCellValue(u.fechaRegistro() != null ? u.fechaRegistro() : "");

            numFila++;
        }
    }
}
