package com.pasajeya.service;

import com.pasajeya.dto.VueloDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class VueloExcelService {

    private static final Logger log = LoggerFactory.getLogger(VueloExcelService.class);

    private static final String[] COLUMNAS = {
        "Aerolínea", "Origen", "Destino", "Fecha",
        "Hora Salida", "Hora Llegada", "Duración",
        "Precio (S/)", "Tipo Tarifa", "Equipaje", "Semáforo"
    };

    public byte[] generarExcel(List<VueloDTO> vuelos) throws IOException {
        log.info("Generando Excel para {} vuelos", vuelos.size());

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Vuelos PasajeYa");

            crearEncabezado(workbook, sheet);
            llenarDatos(workbook, sheet, vuelos);

            for (int i = 0; i < COLUMNAS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            log.info("Excel generado correctamente — {} filas", vuelos.size());
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

    private void llenarDatos(Workbook workbook, Sheet sheet, List<VueloDTO> vuelos) {
        CellStyle estiloAlternado = workbook.createCellStyle();
        estiloAlternado.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        estiloAlternado.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        int numFila = 1;
        for (VueloDTO v : vuelos) {
            Row fila = sheet.createRow(numFila);

            if (numFila % 2 == 0) {
                for (int i = 0; i < COLUMNAS.length; i++) {
                    fila.createCell(i).setCellStyle(estiloAlternado);
                }
            }

            fila.createCell(0).setCellValue(v.aerolinea());
            fila.createCell(1).setCellValue(v.origen());
            fila.createCell(2).setCellValue(v.destino());
            fila.createCell(3).setCellValue(v.fecha());
            fila.createCell(4).setCellValue(v.horaSalida());
            fila.createCell(5).setCellValue(v.horaLlegada());
            fila.createCell(6).setCellValue(v.duracion());
            fila.createCell(7).setCellValue(v.precio());
            fila.createCell(8).setCellValue(v.tipoTarifa());
            fila.createCell(9).setCellValue(Boolean.TRUE.equals(v.incluyeEquipaje()) ? "Sí" : "No");
            fila.createCell(10).setCellValue(v.semaforo().toUpperCase());

            numFila++;
        }
    }
}
