package org.esa.snap.csv.dataio.writer;

import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;

import java.io.IOException;

/**
 * Implementation of {@link CsvWriter} capable of writing single pixels of a given product. The expected input is
 * an array containing 1) the product 2) an array of instances of {@link PixelPos}.
 *
 * @author Thomas Storm
 */
class PixelCsvWriter implements CsvWriter {

    private WriteStrategy targetType;
    private final OutputFormatStrategy targetFormat;

    private Product inputProduct;
    private PixelPos[] pixelPositions;

    PixelCsvWriter(WriteStrategy writer, OutputFormatStrategy targetFormat) {
        this.targetType = writer;
        this.targetFormat = targetFormat;
    }

    @Override
    public void writeCsv(Object... input) throws IOException {
        validate(input);
        extract(input);
        
        final StringBuilder stringBuilder = new StringBuilder();
        targetType.writeCsv(stringBuilder.toString());
    }

    @Override
    public boolean isValidInput(Object... input) {
        return input != null && input.length == 2 && input[0] instanceof Product && input[1] instanceof PixelPos[];
    }


    private void validate(Object[] input) {
        if (!isValidInput(input)) {
            final StringBuilder message = new StringBuilder("Illegal input for writing pixels as CSV file: '");
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

    private void extract(Object[] input) {
        inputProduct = (Product) input[0];
        pixelPositions = (PixelPos[]) input[1];
    }
}
