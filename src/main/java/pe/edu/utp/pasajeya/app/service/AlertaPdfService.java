package pe.edu.utp.pasajeya.app.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import pe.edu.utp.pasajeya.app.dto.AlertaDTO;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AlertaPdfService {

    // Colores corporativos PasajeYa
    private static final BaseColor AZUL_OSCURO  = new BaseColor(0x1E, 0x40, 0xAF);
    private static final BaseColor AZUL_MEDIO   = new BaseColor(0x3B, 0x82, 0xF6);
    private static final BaseColor AZUL_CLARO   = new BaseColor(0xEF, 0xF6, 0xFF);
    private static final BaseColor VERDE_BG     = new BaseColor(0xD1, 0xFA, 0xE5);
    private static final BaseColor VERDE_TEXTO  = new BaseColor(0x05, 0x96, 0x69);
    private static final BaseColor ROJO_BG      = new BaseColor(0xFE, 0xE2, 0xE2);
    private static final BaseColor ROJO_TEXTO   = new BaseColor(0xDC, 0x26, 0x26);
    private static final BaseColor GRIS_CLARO   = new BaseColor(0xF8, 0xFA, 0xFC);
    private static final BaseColor GRIS_BORDE   = new BaseColor(0xE2, 0xE8, 0xF0);
    private static final BaseColor BLANCO       = BaseColor.WHITE;
    private static final BaseColor TEXTO_OSCURO = new BaseColor(0x0F, 0x17, 0x2A);

    private static final Font FONT_TITULO   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  18, BLANCO);
    private static final Font FONT_SUBTIT   = FontFactory.getFont(FontFactory.HELVETICA,       10, BLANCO);
    private static final Font FONT_HEAD     = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   9, BLANCO);
    private static final Font FONT_DATO     = FontFactory.getFont(FontFactory.HELVETICA,        9, TEXTO_OSCURO);
    private static final Font FONT_DATO_B   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   9, TEXTO_OSCURO);
    private static final Font FONT_EST_ACT  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   9, VERDE_TEXTO);
    private static final Font FONT_EST_PAU  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   9, ROJO_TEXTO);
    private static final Font FONT_TOTAL    = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  10, BLANCO);

    public byte[] generar(List<AlertaDTO> alertas, String nombreUsuario) throws Exception {
        Document doc = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(doc, out);

        writer.setPageEvent(new PiePageEvent());
        doc.open();

        // ── Encabezado ────────────────────────────────────────────
        agregarEncabezado(doc, nombreUsuario, alertas.size());

        // ── Tabla de datos ────────────────────────────────────────
        agregarTabla(doc, alertas);

        // ── Totales ───────────────────────────────────────────────
        agregarTotales(doc, alertas);

        doc.close();
        return out.toByteArray();
    }

    private void agregarEncabezado(Document doc, String usuario, int total) throws Exception {
        // Bloque azul título
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);
        header.setSpacingAfter(14);

        PdfPCell cTit = new PdfPCell();
        cTit.setBackgroundColor(AZUL_OSCURO);
        cTit.setPadding(14);
        cTit.setBorder(Rectangle.NO_BORDER);

        Paragraph tit = new Paragraph("PasajeYa — Reporte de Alertas de Precio", FONT_TITULO);
        tit.setAlignment(Element.ALIGN_CENTER);
        cTit.addElement(tit);

        String fechaHoy = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        Paragraph sub = new Paragraph("Usuario: " + usuario + "   |   Generado: " + fechaHoy + "   |   Total alertas: " + total, FONT_SUBTIT);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingBefore(4);
        cTit.addElement(sub);

        header.addCell(cTit);
        doc.add(header);
    }

    private void agregarTabla(Document doc, List<AlertaDTO> alertas) throws Exception {
        // 12 columnas: #, aerolínea, origen, destino, fecha, hora, tarifa, p.obj, p.act, estado, whatsapp, creada
        float[] widths = {2f, 7f, 4.5f, 4.5f, 7f, 5f, 6f, 7.5f, 7.5f, 6f, 8f, 9f};
        PdfPTable tabla = new PdfPTable(widths.length);
        tabla.setWidthPercentage(100);
        tabla.setWidths(widths);
        tabla.setSpacingAfter(10);

        String[] headers = {"#","Aerolínea","Origen","Destino","Fecha vuelo","Hora","Tarifa","Obj. (S/)","Actual (S/)","Estado","WhatsApp","Creada el"};
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, FONT_HEAD));
            c.setBackgroundColor(AZUL_OSCURO);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setVerticalAlignment(Element.ALIGN_MIDDLE);
            c.setPadding(6);
            c.setBorderColor(AZUL_MEDIO);
            tabla.addCell(c);
        }

        for (int i = 0; i < alertas.size(); i++) {
            AlertaDTO a = alertas.get(i);
            BaseColor rowBg = (i % 2 == 0) ? BLANCO : AZUL_CLARO;
            boolean activa = Boolean.TRUE.equals(a.activa());

            addCell(tabla, String.valueOf(i + 1), FONT_DATO_B, Element.ALIGN_CENTER, rowBg);
            addCell(tabla, nvl(a.aerolinea()), FONT_DATO, Element.ALIGN_LEFT, rowBg);
            addCell(tabla, nvl(a.origen()), FONT_DATO, Element.ALIGN_CENTER, rowBg);
            addCell(tabla, nvl(a.destino()), FONT_DATO, Element.ALIGN_CENTER, rowBg);
            addCell(tabla, nvl(a.fecha()), FONT_DATO, Element.ALIGN_CENTER, rowBg);
            addCell(tabla, nvl(a.horaSalida()), FONT_DATO, Element.ALIGN_CENTER, rowBg);
            addCell(tabla, capitalize(nvl(a.tipoTarifa())), FONT_DATO, Element.ALIGN_CENTER, rowBg);
            addCell(tabla, "S/ " + fmt(a.precioObjetivo()), FONT_DATO_B, Element.ALIGN_RIGHT, rowBg);
            addCell(tabla, a.precioActual() != null ? "S/ " + fmt(a.precioActual()) : "—", FONT_DATO, Element.ALIGN_RIGHT, rowBg);

            // Celda estado con color propio
            PdfPCell cEst = new PdfPCell(new Phrase(activa ? "Activa" : "Pausada", activa ? FONT_EST_ACT : FONT_EST_PAU));
            cEst.setBackgroundColor(activa ? VERDE_BG : ROJO_BG);
            cEst.setHorizontalAlignment(Element.ALIGN_CENTER);
            cEst.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cEst.setPadding(5);
            cEst.setBorderColor(GRIS_BORDE);
            tabla.addCell(cEst);

            addCell(tabla, formatTel(a.telefono()), FONT_DATO, Element.ALIGN_LEFT, rowBg);
            addCell(tabla, formatFecha(a.fechaCreacion()), FONT_DATO, Element.ALIGN_CENTER, rowBg);
        }

        doc.add(tabla);
    }

    private void agregarTotales(Document doc, List<AlertaDTO> alertas) throws Exception {
        long activas  = alertas.stream().filter(a -> Boolean.TRUE.equals(a.activa())).count();
        long pausadas = alertas.size() - activas;

        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(60);
        t.setHorizontalAlignment(Element.ALIGN_LEFT);
        t.setWidths(new float[]{1f, 1f, 1f});

        addCeldaTot(t, "Total alertas: " + alertas.size(), AZUL_OSCURO);
        addCeldaTot(t, "Activas: " + activas, VERDE_TEXTO);
        addCeldaTot(t, "Pausadas: " + pausadas, ROJO_TEXTO);

        doc.add(t);
    }

    private void addCeldaTot(PdfPTable t, String texto, BaseColor color) {
        Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BLANCO);
        PdfPCell c = new PdfPCell(new Phrase(texto, f));
        c.setBackgroundColor(color);
        c.setPadding(8);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setBorder(Rectangle.NO_BORDER);
        t.addCell(c);
    }

    private void addCell(PdfPTable t, String val, Font font, int align, BaseColor bg) {
        PdfPCell c = new PdfPCell(new Phrase(val, font));
        c.setBackgroundColor(bg);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(5);
        c.setBorderColor(GRIS_BORDE);
        t.addCell(c);
    }

    private String nvl(String v) { return v != null ? v : ""; }

    private String capitalize(String v) {
        if (v == null || v.isEmpty()) return v;
        return Character.toUpperCase(v.charAt(0)) + v.substring(1).toLowerCase();
    }

    private String fmt(Double v) {
        if (v == null) return "—";
        return String.format("%,.0f", v);
    }

    private String formatTel(String tel) {
        if (tel == null) return "";
        return tel.startsWith("+") ? tel : "+" + tel;
    }

    private String formatFecha(String iso) {
        if (iso == null || iso.isBlank()) return "";
        try { return iso.substring(0, 10).replace("-", "/"); }
        catch (Exception e) { return iso; }
    }

    // Pie de página con número
    private static class PiePageEvent extends PdfPageEventHelper {
        private static final Font FONT_PIE = FontFactory.getFont(FontFactory.HELVETICA, 8,
                new BaseColor(0x64, 0x74, 0x8B));

        @Override
        public void onEndPage(PdfWriter writer, Document doc) {
            PdfContentByte cb = writer.getDirectContent();
            Phrase pie = new Phrase("PasajeYa © 2026  |  Reporte confidencial  |  Página " + writer.getPageNumber(), FONT_PIE);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, pie,
                    (doc.left() + doc.right()) / 2, doc.bottom() - 10, 0);
        }
    }
}
