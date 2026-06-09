package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.AlertaDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AlertaExcelService {

    private static final String[] HEADERS = {
        "#", "Aerolínea", "Origen", "Destino", "Fecha vuelo",
        "Hora salida", "Tarifa", "Precio objetivo (S/)",
        "Precio actual (S/)", "Estado", "WhatsApp", "Creada el"
    };

    public byte[] generar(List<AlertaDTO> alertas, String nombreUsuario) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Mis Alertas");
            sheet.setDefaultColumnWidth(18);

            // Paleta de colores
            XSSFColor azulOscuro  = color(wb, 0x1E, 0x40, 0xAF); // blue-800
            XSSFColor azulMedio   = color(wb, 0x3B, 0x82, 0xF6); // blue-500
            XSSFColor verdeCelda  = color(wb, 0xD1, 0xFA, 0xE5); // green-100
            XSSFColor rojoClaro   = color(wb, 0xFE, 0xE2, 0xE2); // red-100
            XSSFColor grisClaro   = color(wb, 0xF8, 0xFA, 0xFC); // slate-50
            XSSFColor grisAlter   = color(wb, 0xEF, 0xF6, 0xFF); // blue-50
            XSSFColor verdeEstado = color(wb, 0x05, 0x96, 0x69); // emerald-600
            XSSFColor rojoEstado  = color(wb, 0xDC, 0x26, 0x26); // red-600

            // ── Fila 1: título ────────────────────────────────────────
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));
            Row rowTitulo = sheet.createRow(0);
            rowTitulo.setHeightInPoints(36);
            Cell cTitulo = rowTitulo.createCell(0);
            cTitulo.setCellValue("PasajeYa — Reporte de Alertas de Precio");
            cTitulo.setCellStyle(estiloTitulo(wb, azulOscuro));

            // ── Fila 2: subtítulo / metadata ──────────────────────────
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, HEADERS.length - 1));
            Row rowMeta = sheet.createRow(1);
            rowMeta.setHeightInPoints(20);
            Cell cMeta = rowMeta.createCell(0);
            String fechaHoy = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            cMeta.setCellValue("Usuario: " + nombreUsuario + "   |   Generado: " + fechaHoy + "   |   Total alertas: " + alertas.size());
            cMeta.setCellStyle(estiloMeta(wb, azulMedio));

            // ── Fila 3: vacía separadora ──────────────────────────────
            sheet.createRow(2).setHeightInPoints(6);

            // ── Fila 4: encabezados ───────────────────────────────────
            Row rowHead = sheet.createRow(3);
            rowHead.setHeightInPoints(26);
            XSSFCellStyle estiloHead = estiloHeader(wb, azulOscuro);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell c = rowHead.createCell(i);
                c.setCellValue(HEADERS[i]);
                c.setCellStyle(estiloHead);
            }

            // ── Filas de datos ────────────────────────────────────────
            int rowNum = 4;
            for (int idx = 0; idx < alertas.size(); idx++) {
                AlertaDTO a = alertas.get(idx);
                Row row = sheet.createRow(rowNum++);
                row.setHeightInPoints(20);

                boolean par = idx % 2 == 0;
                XSSFCellStyle estiloBase = estiloDato(wb, par ? grisClaro : grisAlter);
                XSSFCellStyle estiloNum  = estiloDatoNumero(wb, par ? grisClaro : grisAlter);
                XSSFCellStyle estiloEst  = estiloEstado(wb,
                    Boolean.TRUE.equals(a.activa()) ? verdeCelda : rojoClaro,
                    Boolean.TRUE.equals(a.activa()) ? verdeEstado : rojoEstado);

                celda(row, 0, String.valueOf(idx + 1), estiloBase);
                celda(row, 1, nvl(a.aerolinea()), estiloBase);
                celda(row, 2, nvl(a.origen()), estiloBase);
                celda(row, 3, nvl(a.destino()), estiloBase);
                celda(row, 4, nvl(a.fecha()), estiloBase);
                celda(row, 5, nvl(a.horaSalida()), estiloBase);
                celda(row, 6, capitalize(nvl(a.tipoTarifa())), estiloBase);
                celdaNum(row, 7, a.precioObjetivo(), estiloNum);
                if (a.precioActual() != null) {
                    celdaNum(row, 8, a.precioActual(), estiloNum);
                } else {
                    celda(row, 8, "—", estiloBase);
                }
                celda(row, 9, Boolean.TRUE.equals(a.activa()) ? "Activa" : "Pausada", estiloEst);
                celda(row, 10, formatTel(a.telefono()), estiloBase);
                celda(row, 11, formatFecha(a.fechaCreacion()), estiloBase);
            }

            // ── Fila de totales ───────────────────────────────────────
            Row rowTotal = sheet.createRow(rowNum);
            rowTotal.setHeightInPoints(22);
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, 6));
            XSSFCellStyle estiloTot = estiloTotales(wb, azulOscuro);
            Cell cTot = rowTotal.createCell(0);
            long activas  = alertas.stream().filter(a -> Boolean.TRUE.equals(a.activa())).count();
            long pausadas = alertas.size() - activas;
            cTot.setCellValue("TOTAL: " + alertas.size() + " alertas   |   Activas: " + activas + "   |   Pausadas: " + pausadas);
            cTot.setCellStyle(estiloTot);

            // ── Anchos específicos ────────────────────────────────────
            sheet.setColumnWidth(0, 4 * 256);   // #
            sheet.setColumnWidth(1, 14 * 256);  // aerolínea
            sheet.setColumnWidth(2, 8 * 256);   // origen
            sheet.setColumnWidth(3, 8 * 256);   // destino
            sheet.setColumnWidth(4, 14 * 256);  // fecha vuelo
            sheet.setColumnWidth(5, 12 * 256);  // hora
            sheet.setColumnWidth(6, 12 * 256);  // tarifa
            sheet.setColumnWidth(7, 16 * 256);  // precio objetivo
            sheet.setColumnWidth(8, 16 * 256);  // precio actual
            sheet.setColumnWidth(9, 12 * 256);  // estado
            sheet.setColumnWidth(10, 16 * 256); // WhatsApp
            sheet.setColumnWidth(11, 20 * 256); // creada

            // Freeze encabezados
            sheet.createFreezePane(0, 4);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── Estilos ───────────────────────────────────────────────────

    private XSSFCellStyle estiloTitulo(XSSFWorkbook wb, XSSFColor bg) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(bg);
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 16);
        f.setColor(new XSSFColor(new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF}, null));
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle estiloMeta(XSSFWorkbook wb, XSSFColor bg) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(bg);
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        f.setColor(new XSSFColor(new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF}, null));
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle estiloHeader(XSSFWorkbook wb, XSSFColor bg) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(bg);
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(s, BorderStyle.THIN, IndexedColors.WHITE.getIndex());
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 11);
        f.setColor(new XSSFColor(new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF}, null));
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle estiloDato(XSSFWorkbook wb, XSSFColor bg) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(bg);
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorderLight(s);
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle estiloDatoNumero(XSSFWorkbook wb, XSSFColor bg) {
        XSSFCellStyle s = estiloDato(wb, bg);
        s.setAlignment(HorizontalAlignment.RIGHT);
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        return s;
    }

    private XSSFCellStyle estiloEstado(XSSFWorkbook wb, XSSFColor bg, XSSFColor fg) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(bg);
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorderLight(s);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 10);
        f.setColor(fg);
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle estiloTotales(XSSFWorkbook wb, XSSFColor bg) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(bg);
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 10);
        f.setColor(new XSSFColor(new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF}, null));
        s.setFont(f);
        return s;
    }

    // ── Helpers ───────────────────────────────────────────────────

    private XSSFColor color(XSSFWorkbook wb, int r, int g, int b) {
        return new XSSFColor(new byte[]{(byte)r,(byte)g,(byte)b}, null);
    }

    private void setBorder(XSSFCellStyle s, BorderStyle bs, short color) {
        s.setBorderTop(bs);    s.setTopBorderColor(color);
        s.setBorderBottom(bs); s.setBottomBorderColor(color);
        s.setBorderLeft(bs);   s.setLeftBorderColor(color);
        s.setBorderRight(bs);  s.setRightBorderColor(color);
    }

    private void setBorderLight(XSSFCellStyle s) {
        setBorder(s, BorderStyle.THIN, IndexedColors.GREY_25_PERCENT.getIndex());
    }

    private void celda(Row row, int col, String val, XSSFCellStyle estilo) {
        Cell c = row.createCell(col);
        c.setCellValue(val);
        c.setCellStyle(estilo);
    }

    private void celdaNum(Row row, int col, double val, XSSFCellStyle estilo) {
        Cell c = row.createCell(col);
        c.setCellValue(val);
        c.setCellStyle(estilo);
    }

    private String nvl(String v) { return v != null ? v : ""; }

    private String capitalize(String v) {
        if (v == null || v.isEmpty()) return v;
        return Character.toUpperCase(v.charAt(0)) + v.substring(1).toLowerCase();
    }

    private String formatTel(String tel) {
        if (tel == null) return "";
        return tel.startsWith("+") ? tel : "+" + tel;
    }

    private String formatFecha(String iso) {
        if (iso == null || iso.isBlank()) return "";
        try {
            return iso.substring(0, 10).replace("-", "/");
        } catch (Exception e) {
            return iso;
        }
    }
}
