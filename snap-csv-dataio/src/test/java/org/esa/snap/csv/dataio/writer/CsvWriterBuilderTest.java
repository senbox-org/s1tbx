package org.esa.snap.csv.dataio.writer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * @author Thomas Storm
 */
public class CsvWriterBuilderTest {

    @Test
    public void testTypes() {
        final CsvWriter productWriter = new CsvWriterBuilder()
                .sourceType(SourceType.PRODUCT)
                .targetType(TargetType.FILE)
                .targetFormat(OutputFormat.BEAM)
                .targetFile("target.csv")
                .build();
        assertEquals(CsvWriterBuilder.ProductCsvWriter.class, productWriter.getClass());

        final CsvWriter featureWriter = new CsvWriterBuilder()
                .sourceType(SourceType.FEATURE)
                .targetType(TargetType.FILE)
                .targetFormat(OutputFormat.ODESA)
                .targetFile("target.csv")
                .build();

    }

    @Test
    public void testValidation() {
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
