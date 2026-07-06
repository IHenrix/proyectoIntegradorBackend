package pe.edu.utp.pasajeya.app.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import pe.edu.utp.pasajeya.app.dto.AdminDashboardDTO;
import pe.edu.utp.pasajeya.app.dto.AdminReporteResumenDTO;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class AdminDashboardPdfService {

    // Colores corporativos PasajeYa — mismos que AlertaPdfService, para consistencia visual.
    private static final BaseColor AZUL_OSCURO  = new BaseColor(0x1E, 0x40, 0xAF);
    private static final BaseColor AZUL_MEDIO   = new BaseColor(0x3B, 0x82, 0xF6);
    private static final BaseColor AZUL_CLARO   = new BaseColor(0xEF, 0xF6, 0xFF);
    private static final BaseColor GRIS_BORDE   = new BaseColor(0xE2, 0xE8, 0xF0);
    private static final BaseColor BLANCO       = BaseColor.WHITE;
    private static final BaseColor TEXTO_OSCURO = new BaseColor(0x0F, 0x17, 0x2A);

    private static final Font FONT_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BLANCO);
    private static final Font FONT_SUBTIT = FontFactory.getFont(FontFactory.HELVETICA, 10, BLANCO);
    private static final Font FONT_SECCION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, TEXTO_OSCURO);
    private static final Font FONT_HEAD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BLANCO);
    private static final Font FONT_DATO = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXTO_OSCURO);
    private static final Font FONT_DATO_B = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, TEXTO_OSCURO);

    public byte[] generar(AdminDashboardDTO dashboard, AdminReporteResumenDTO reporte) throws Exception {
        Document doc = new Document(PageSize.A4, 32, 32, 32, 32);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        writer.setPageEvent(new PiePageEvent());
        doc.open();

        agregarEncabezado(doc);
        agregarMetricasGenerales(doc, dashboard);
        agregarUsuariosPorRol(doc, dashboard);
        agregarKpis(doc, reporte);

        doc.close();
        return out.toByteArray();
    }

    private void agregarEncabezado(Document doc) throws Exception {
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);
        header.setSpacingAfter(16);

        PdfPCell cTit = new PdfPCell();
        cTit.setBackgroundColor(AZUL_OSCURO);
        cTit.setPadding(14);
        cTit.setBorder(Rectangle.NO_BORDER);

        Paragraph tit = new Paragraph("PasajeYa — Reporte ejecutivo del sistema", FONT_TITULO);
        tit.setAlignment(Element.ALIGN_CENTER);
        cTit.addElement(tit);

        String fechaHoy = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        Paragraph sub = new Paragraph("Generado: " + fechaHoy, FONT_SUBTIT);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingBefore(4);
        cTit.addElement(sub);

        header.addCell(cTit);
        doc.add(header);
    }

    private void agregarMetricasGenerales(Document doc, AdminDashboardDTO d) throws Exception {
        Paragraph titulo = new Paragraph("Métricas generales", FONT_SECCION);
        titulo.setSpacingAfter(8);
        doc.add(titulo);

        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setSpacingAfter(16);

        agregarFilaMetrica(t, "Usuarios activos", String.valueOf(d.usuariosActivos()));
        agregarFilaMetrica(t, "Usuarios inactivos", String.valueOf(d.usuariosInactivos()));
        agregarFilaMetrica(t, "Ingresos totales", "S/ " + d.ingresosTotales());
        agregarFilaMetrica(t, "Alertas activas", String.valueOf(d.alertasActivas()));
        agregarFilaMetrica(t, "Suscripciones activas", String.valueOf(d.suscripcionesActivas()));
        agregarFilaMetrica(t, "Suscripciones vencidas", String.valueOf(d.suscripcionesVencidas()));
        agregarFilaMetrica(t, "Suscripciones canceladas", String.valueOf(d.suscripcionesCanceladas()));

        doc.add(t);
    }

    private void agregarUsuariosPorRol(Document doc, AdminDashboardDTO d) throws Exception {
        Paragraph titulo = new Paragraph("Usuarios por rol", FONT_SECCION);
        titulo.setSpacingAfter(8);
        doc.add(titulo);

        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setSpacingAfter(16);

        PdfPCell hRol = new PdfPCell(new Phrase("Rol", FONT_HEAD));
        hRol.setBackgroundColor(AZUL_OSCURO);
        hRol.setPadding(6);
        t.addCell(hRol);
        PdfPCell hCant = new PdfPCell(new Phrase("Cantidad", FONT_HEAD));
        hCant.setBackgroundColor(AZUL_OSCURO);
        hCant.setPadding(6);
        t.addCell(hCant);

        int i = 0;
        for (Map.Entry<String, Long> e : d.usuariosPorRol().entrySet()) {
            BaseColor bg = (i % 2 == 0) ? BLANCO : AZUL_CLARO;
            addCell(t, e.getKey(), FONT_DATO, Element.ALIGN_LEFT, bg);
            addCell(t, String.valueOf(e.getValue()), FONT_DATO_B, Element.ALIGN_CENTER, bg);
            i++;
        }

        doc.add(t);
    }

    private void agregarKpis(Document doc, AdminReporteResumenDTO r) throws Exception {
        Paragraph titulo = new Paragraph("Indicadores clave", FONT_SECCION);
        titulo.setSpacingAfter(8);
        doc.add(titulo);

        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);

        agregarFilaMetrica(t, "Tasa de conversión a Premium", r.tasaConversionPremium() + "%");
        agregarFilaMetrica(t, "Ingreso promedio por suscripción", "S/ " + r.ingresoPromedioPorSuscripcion());
        agregarFilaMetrica(t, "Ruta más consultada", r.rutaMasConsultada());
        agregarFilaMetrica(t, "Usuarios nuevos (mes actual)", String.valueOf(r.usuariosNuevosMesActual()));
        agregarFilaMetrica(t, "Usuarios nuevos (mes anterior)", String.valueOf(r.usuariosNuevosMesAnterior()));
        agregarFilaMetrica(t, "Ingresos mes actual", "S/ " + r.ingresosMesActual());
        agregarFilaMetrica(t, "Ingresos mes anterior", "S/ " + r.ingresosMesAnterior());

        doc.add(t);
    }

    private void agregarFilaMetrica(PdfPTable t, String label, String valor) {
        PdfPCell cLabel = new PdfPCell(new Phrase(label, FONT_DATO));
        cLabel.setPadding(6);
        cLabel.setBorderColor(GRIS_BORDE);
        t.addCell(cLabel);

        PdfPCell cValor = new PdfPCell(new Phrase(valor, FONT_DATO_B));
        cValor.setPadding(6);
        cValor.setBorderColor(GRIS_BORDE);
        cValor.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(cValor);
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
