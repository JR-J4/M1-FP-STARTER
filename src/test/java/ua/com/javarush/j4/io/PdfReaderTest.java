package ua.com.javarush.j4.io;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PdfReaderTest {

    private final PdfReader reader = new PdfReader();

    @Test
    void supportsPdfExtension() {
        assertTrue(reader.supports(Path.of("doc.pdf")));
        assertFalse(reader.supports(Path.of("doc.txt")));
    }

    @Test
    void extractsTextFromPdf(@TempDir Path dir) throws IOException {
        Path pdf = dir.resolve("hello.pdf");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText("Hello PDF World");
                cs.endText();
            }
            doc.save(pdf.toFile());
        }

        assertTrue(reader.read(pdf).contains("Hello PDF World"),
                "extracted text should contain the written line");
    }
}
