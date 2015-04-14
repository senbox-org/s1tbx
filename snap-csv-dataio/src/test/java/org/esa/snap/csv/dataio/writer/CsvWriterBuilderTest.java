package org.esa.snap.csv.dataio.writer;

import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Thomas Storm
 */
public class CsvWriterBuilderTest {

    @Test
    public void testTypes() throws Exception {
        final CsvWriter productWriter = new CsvWriterBuilder()
                .sourceType(SourceType.PRODUCT)
                .targetType(TargetType.FILE)
                .targetFormat(OutputFormat.BEAM)
                .targetFile("target.csv")
                .build();
        assertEquals(CsvWriterBuilder.ProductCsvWriter.class, productWriter.getClass());

        final CsvWriter pixelWriter = new CsvWriterBuilder()
                .sourceType(SourceType.PIXEL)
                .targetType(TargetType.FILE)
                .targetFormat(OutputFormat.ODESA)
                .targetFile("target.csv")
                .build();
        assertEquals(PixelCsvWriter.class, pixelWriter.getClass());

        final CsvWriter featureWriter = new CsvWriterBuilder()
                .sourceType(SourceType.FEATURE)
                .targetType(TargetType.FILE)
                .targetFormat(OutputFormat.ODESA)
                .targetFile("target.csv")
                .build();
        assertEquals(FeatureCsvWriter.class, featureWriter.getClass());
    }

    @Test
    public void testValidation() throws Exception {
        try {
            new CsvWriterBuilder().build();
            fail();
        } catch (IllegalStateException expected) {
            validateException(expected.getMessage(), "missing source type");
        }

        try {
            new CsvWriterBuilder()
                    .sourceType(SourceType.FEATURE)
                    .targetType(TargetType.CLIPBOARD)
                    .targetFile("")
                    .build();
            fail();
        } catch (IllegalStateException expected) {
            validateException(expected.getMessage(), "both clipboard and target file");
        }

        try {
            new CsvWriterBuilder()
                    .sourceType(SourceType.FEATURE)
                    .targetType(TargetType.FILE)
                    .build();
            fail();
        } catch (IllegalStateException expected) {
            validateException(expected.getMessage(), "No target file specified");
        }
    }

    private void validateException(String message, String error) {
        assertTrue(message.contains(error));
    }
}
