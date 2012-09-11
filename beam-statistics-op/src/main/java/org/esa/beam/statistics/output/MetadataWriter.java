package org.esa.beam.statistics.output;

import org.esa.beam.framework.datamodel.Product;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

public class MetadataWriter implements StatisticsOutputter {

    private final PrintStream printStream;

    public MetadataWriter(PrintStream printStream) {
        this.printStream = printStream;
    }
    @Override
    public void initialiseOutput(StatisticsOutputContext statisticsOutputContext) {
        printStream.append("# BEAM Statistics export\n")
                .append("#\n")
                .append("# Products:\n");
        for (Product sourceProduct : statisticsOutputContext.sourceProducts) {
            printStream.append("#              ")
                    .append(sourceProduct.getName())
                    .append("\n");
        }
        if (statisticsOutputContext.startDate != null) {
            printStream
                    .append("#\n")
                    .append("# Start Date: ")
                    .append(statisticsOutputContext.startDate.format())
                    .append("\n");
        }
        if (statisticsOutputContext.endDate != null) {
            printStream
                    .append("#\n")
                    .append("# End Date: ")
                    .append(statisticsOutputContext.endDate.format())
                    .append("\n");
        }
        printStream.append("#\n");
        printStream.append("# Regions:\n");
        for (String regionId : statisticsOutputContext.regionIds) {
            printStream.append("#              ")
                    .append(regionId)
                    .append("\n");
        }
    }

    @Override
    public void addToOutput(String bandName, String regionId, Map<String, Number> statistics) {
    }

    @Override
    public void finaliseOutput() throws IOException {
    }
}
