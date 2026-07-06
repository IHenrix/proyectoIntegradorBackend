package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.AdminSuscripcionDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class AdminSuscripcionExcelService {

    private static final Logger log = LoggerFactory.getLogger(AdminSuscripcionExcelService.class);

    private static final String[] COLUMNAS = {
        "Email", "Usuario", "Plan", "Tipo", "Monto (S/)", "Inicio", "Vencimiento", "Estado", "Método pago", "Auto-renovar"
    };

    public byte[] generarExcel(List<AdminSuscripcionDTO> filas) throws IOException {
        log.info("Generando Excel de suscripciones para {} filas", filas.size());

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Suscripciones");

            crearEncabezado(workbook, sheet);
            llenarDatos(workbook, sheet, filas);

            for (int i = 0; i < COLUMNAS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            log.info("Excel de suscripciones generado — {} filas", filas.size());
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

    private void llenarDatos(Workbook workbook, Sheet sheet, List<AdminSuscripcionDTO> filas) {
        CellStyle estiloAlternado = workbook.createCellStyle();
        estiloAlternado.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        estiloAlternado.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        int numFila = 1;
        for (AdminSuscripcionDTO s : filas) {
            Row fila = sheet.createRow(numFila);

            if (numFila % 2 == 0) {
                for (int i = 0; i < COLUMNAS.length; i++) {
                    fila.createCell(i).setCellStyle(estiloAlternado);
                }
            }

            fila.createCell(0).setCellValue(s.emailUsuario());
            fila.createCell(1).setCellValue(s.nombreUsuario());
            fila.createCell(2).setCellValue(s.planNombre());
            fila.createCell(3).setCellValue(s.tipoPlan());
            fila.createCell(4).setCellValue(s.monto().doubleValue());
            fila.createCell(5).setCellValue(s.fechaInicio());
            fila.createCell(6).setCellValue(s.fechaFin());
            fila.createCell(7).setCellValue(s.estado());
            fila.createCell(8).setCellValue(s.metodoPago());
            fila.createCell(9).setCellValue(Boolean.TRUE.equals(s.autoRenovar()) ? "Sí" : "No");

            numFila++;
        }
    }
}
