package org.esa.beam.statistics;

import java.io.PrintStream;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

public class MetadataWriter {

    private final PrintStream printStream;

    public MetadataWriter(PrintStream printStream) {
        this.printStream = printStream;
    }

    public void writeMetadata(Product[] sourceProducts, ProductData.UTC startDate, ProductData.UTC endDate, String[] regionIds) {
        printStream.append("# BEAM Statistics export\n")
                    .append("#\n")
                    .append("# Products:\n");
        for (Product sourceProduct : sourceProducts) {
            printStream.append("#              ")
                        .append(sourceProduct.getName())
                        .append("\n");
        }
        if (startDate != null) {
            printStream
                        .append("#\n")
                        .append("# Start Date: ")
                        .append(startDate.format())
                        .append("\n");
        }
        if (endDate != null) {
            printStream
                        .append("#\n")
                        .append("# End Date: ")
                        .append(endDate.format())
                        .append("\n");
        }
        printStream.append("#\n");
        printStream.append("# Regions:\n");
        for (String regionId : regionIds) {
            printStream.append("#              ")
                        .append(regionId)
                        .append("\n");
        }
    }
}
