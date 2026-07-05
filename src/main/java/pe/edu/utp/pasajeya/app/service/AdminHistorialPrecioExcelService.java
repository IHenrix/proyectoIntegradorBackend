package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.AdminHistorialPrecioDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class AdminHistorialPrecioExcelService {

    private static final Logger log = LoggerFactory.getLogger(AdminHistorialPrecioExcelService.class);

    private static final String[] COLUMNAS = {
        "Vuelo", "Aerolínea", "Origen", "Destino", "Tipo tarifa", "Precio (S/)", "Fecha captura"
    };

    public byte[] generarExcel(List<AdminHistorialPrecioDTO> filas) throws IOException {
        log.info("Generando Excel de historial de precios para {} filas", filas.size());

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Historial de precios");

            crearEncabezado(workbook, sheet);
            llenarDatos(workbook, sheet, filas);

            for (int i = 0; i < COLUMNAS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            log.info("Excel de historial de precios generado — {} filas", filas.size());
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

    private void llenarDatos(Workbook workbook, Sheet sheet, List<AdminHistorialPrecioDTO> filas) {
        CellStyle estiloAlternado = workbook.createCellStyle();
        estiloAlternado.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        estiloAlternado.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        int numFila = 1;
        for (AdminHistorialPrecioDTO h : filas) {
            Row fila = sheet.createRow(numFila);

            if (numFila % 2 == 0) {
                for (int i = 0; i < COLUMNAS.length; i++) {
                    fila.createCell(i).setCellStyle(estiloAlternado);
                }
            }

            fila.createCell(0).setCellValue(h.idVuelo());
            fila.createCell(1).setCellValue(h.aerolinea());
            fila.createCell(2).setCellValue(h.origen());
            fila.createCell(3).setCellValue(h.destino());
            fila.createCell(4).setCellValue(h.tipoTarifa());
            fila.createCell(5).setCellValue(h.precio().doubleValue());
            fila.createCell(6).setCellValue(h.fechaCaptura());

            numFila++;
        }
    }
}
