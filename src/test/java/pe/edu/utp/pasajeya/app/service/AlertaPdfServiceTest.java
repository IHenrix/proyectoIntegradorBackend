package pe.edu.utp.pasajeya.app.service;

import pe.edu.utp.pasajeya.app.dto.AlertaDTO;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class AlertaPdfServiceTest {

    private final AlertaPdfService pdfService = new AlertaPdfService();

    // ═══════════════════════════════════════════════════
    // PRUEBA 1: Genera un PDF valido con cabecera %PDF
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe generar un archivo PDF valido con cabecera %PDF")
    void cuandoGeneraPdf_debeProducirArchivoValido() throws Exception {
        AlertaDTO alerta = new AlertaDTO(
                1, 100L, 10, "LATAM", "LIM", "CUZ",
                "2026-07-01", "08:30", "basica",
                200.0, 250.0, "987654321", true,
                "2026-06-23T10:00:00", "Activa"
        );

        byte[] pdf = pdfService.generar(List.of(alerta), "Ana Garcia");

        assertThat(pdf).isNotEmpty();
        String cabecera = new String(pdf, 0, 4);
        assertThat(cabecera).isEqualTo("%PDF");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 2: El contenido del PDF incluye los datos de la alerta
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("El texto del PDF debe contener los datos de la alerta y del usuario")
    void cuandoGeneraPdf_debeContenerLosDatos() throws Exception {
        AlertaDTO alerta = new AlertaDTO(
                1, 100L, 10, "LATAM", "LIM", "CUZ",
                "2026-07-01", "08:30", "basica",
                200.0, 250.0, "987654321", true,
                "2026-06-23T10:00:00", "Activa"
        );

        byte[] pdf = pdfService.generar(List.of(alerta), "Ana Garcia");

        PdfReader reader = new PdfReader(pdf);
        String texto = PdfTextExtractor.getTextFromPage(reader, 1);
        reader.close();

        assertThat(texto).contains("Ana Garcia");
        assertThat(texto).contains("LATAM");
        assertThat(texto).contains("LIM");
        assertThat(texto).contains("Activa");
    }

    // ═══════════════════════════════════════════════════
    // PRUEBA 3: Lista vacia no debe lanzar excepcion
    // ═══════════════════════════════════════════════════
    @Test
    @DisplayName("Debe generar el PDF correctamente cuando no hay alertas")
    void cuandoListaVacia_debeGenerarPdfSinError() throws Exception {
        byte[] pdf = pdfService.generar(List.of(), "Ana Garcia");
        assertThat(pdf).isNotEmpty();
    }
}
