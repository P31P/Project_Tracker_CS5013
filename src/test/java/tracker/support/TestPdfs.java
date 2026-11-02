package tracker.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/** Builds small one-page PDFs for tests, so no binary fixtures are needed. */
public final class TestPdfs {

    private TestPdfs() {
    }

    /** A PDF whose page shows these lines of text; no lines gives a page with no text, like a scan. */
    public static byte[] withLines(String... lines) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            if (lines.length > 0) {
                try (PDPageContentStream text = new PDPageContentStream(document, page)) {
                    text.beginText();
                    text.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    text.newLineAtOffset(72, 700);
                    for (String line : lines) {
                        text.showText(line);
                        text.newLineAtOffset(0, -16);
                    }
                    text.endText();
                }
            }
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
