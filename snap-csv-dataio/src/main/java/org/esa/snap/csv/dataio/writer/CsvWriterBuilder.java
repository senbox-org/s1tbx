package org.esa.snap.csv.dataio.writer;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.csv.dataio.Constants;

import java.io.IOException;

/**
 * Class for configuring the CSV export mechanism. Builds an instance of {@link CsvWriter}.
 *
 * @author Thomas Storm
 */
public class CsvWriterBuilder {

    private SourceType sourceType;
    private TargetType targetType;
    private OutputFormat targetFormat;
    private String targetFile;

    CsvWriterBuilder sourceType(SourceType sourceType) {
        this.sourceType = sourceType;
        return this;
    }

    CsvWriterBuilder targetType(TargetType targetType) {
        this.targetType = targetType;
        return this;
    }

    CsvWriterBuilder targetFormat(OutputFormat targetFormat) {
        this.targetFormat = targetFormat;
        return this;
    }

    CsvWriterBuilder targetFile(String targetFile) {
        this.targetFile = targetFile;
        return this;
    }

    CsvWriter build() {
        validateState();
        final OutputFormatStrategy outputFormat;
        final WriteStrategy writer;
        switch (targetFormat) {
            case ODESA: {
                outputFormat = new OdesaOutputFormat();
                break;
            }
            default: {
                outputFormat = new BeamOutputFormat();
                break;
            }
        }

        switch (targetType) {
            case CLIPBOARD: {
                writer = new ClipboardWriter();
                break;
            }
            default: {
                writer = new CsvFileWriter(targetFile);
                break;
            }
        }

        switch (sourceType) {
            case PRODUCT: {
                return new ProductCsvWriter(targetFile);
            }
            case PIXEL: {
                return new PixelCsvWriter(writer, outputFormat);
            }
            case FEATURE: {
                return new FeatureCsvWriter(writer, outputFormat);
            }
            default:
                throw new IllegalStateException("Illegal source type: '" + sourceType + "'.");
        }
    }

    private void validateState() {
        if (sourceType == null) {
            throw new IllegalStateException("Unable to create CSV writer due to missing source type.");
        }
        if (targetType.equals(TargetType.CLIPBOARD) && targetFile != null) {
            throw new IllegalStateException("Unable to create CSV writer since both clipboard and target file are specified.");
        }
        if (targetType.equals(TargetType.FILE) && targetFile == null) {
            throw new IllegalStateException("No target file specified while target type is '" + TargetType.FILE + "'.");
        }
    }

    private static class ClipboardWriter implements WriteStrategy {
        @Override
        public void writeCsv(String fullOutput) {
            // todo
        }
    }

    private static class CsvFileWriter implements WriteStrategy {

        private final String targetFile;

        private CsvFileWriter(String targetFile) {
            this.targetFile = targetFile;
        }

        @Override
        public void writeCsv(String fullOutput) throws IOException {
//            new FileWriter(targetFile);
            // todo
        }
    }

    class ProductCsvWriter implements CsvWriter {
        // todo - let CsvProductWriter implement the WriteStrategy interface, too
        private String targetFile;

        ProductCsvWriter(String targetFile) {
            this.targetFile = targetFile;
        }

        @Override
        public void writeCsv(Object... input) throws IOException {
            validateInput(input);
            Product sourceProduct = (Product) input[0];
            final int config = CsvProductWriter.WRITE_FEATURES | CsvProductWriter.WRITE_PROPERTIES;
            final ProductWriter productWriter = new CsvProductWriter(null, config, null);
            productWriter.writeProductNodes(sourceProduct, targetFile);
            productWriter.writeBandRasterData(null, -1, -1, -1, -1, null, ProgressMonitor.NULL);
        }

        @Override
        public boolean isValidInput(Object... input) {
            return input != null && input.length == 1 && input[0] instanceof Product;
        }

        private void validateInput(Object[] input) {
            if (isValidInput(input)) {
                final StringBuilder message = new StringBuilder("Illegal input for writing a product as CSV file: '");
                for (int i = 0; i < input.length; i++) {
                    Object o = input[i];
                    message.append(o.toString());
                    if (i == input.length - 1) {
                        message.append(", ");
                    }
                }
                message.append("'");
                throw new IllegalArgumentException(message.toString());
            }
        }
    }

    static class BeamOutputFormat implements OutputFormatStrategy {

        private static final String separator = Constants.DEFAULT_SEPARATOR;

        @Override
        public String formatProperty(String key, String value) {
            return key + "=" + value;
        }

        @Override
        public String formatHeader(String[] attributes, Class[] types) {
            final StringBuilder builder = new StringBuilder();
            builder.append("featureId");
            for (int i = 0; i < attributes.length; i++) {
                builder.append(separator);
                builder.append(attributes[i]);
                builder.append(":");
                builder.append(types[i].getSimpleName().toLowerCase());
            }
            return builder.toString();
        }

        @Override
        public String formatRecord(String recordId, String[] values) {
            final StringBuilder builder = new StringBuilder();
            builder.append(recordId);
            for (String value : values) {
                builder.append(separator);
                builder.append(value);
            }
            return builder.toString();
        }
    }

    static class OdesaOutputFormat implements OutputFormatStrategy {

        @Override
        public String formatProperty(String key, String value) {
            return key + "=" + value;
        }

        @Override
        public String formatHeader(String[] attributes, Class[] types) {
            final StringBuilder builder = new StringBuilder();
            builder.append("feature_id");
            return builder.toString();
        }

        @Override
        public String formatRecord(String recordId, String[] values) {
            return "";
        }
    }

}
