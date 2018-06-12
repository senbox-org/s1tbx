package org.esa.snap.statistics.output;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import org.esa.snap.statistics.tools.TimeInterval;

/**
 * Writes some metadata about the statistics to the given instance of {@link PrintStream}.
 *
 * @author Sabine Embacher
 * @author Thomas Storm
 */
public class MetadataWriter implements StatisticsOutputter {

    private final PrintStream printStream;

    /**
     * Creates a new instance.
     *
     * @param printStream The stream the metadata shall be written to.
     */
    public MetadataWriter(PrintStream printStream) {
        this.printStream = printStream;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation writes the complete metadata.
     *
     *
     * @param statisticsOutputContext A context providing meta-information about the statistics.
     */
    @Override
    public void initialiseOutput(StatisticsOutputContext statisticsOutputContext) {
        printStream.append("# SNAP Statistics export\n")
                .append("#\n")
                .append("# Products:\n");
        for (String sourceProductName : statisticsOutputContext.sourceProductNames) {
            printStream.append("#              ")
                    .append(sourceProductName)
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
        if (statisticsOutputContext.timeIntervals != null) {
            printStream
                    .append("#\n")
                    .append("# Time Intervals:")
                    .append("\n");;
            for (TimeInterval timeInterval : statisticsOutputContext.timeIntervals) {
                printStream
                        .append("#              From ")
                        .append(timeInterval.getIntervalStart().format())
                        .append(" to ")
                        .append(timeInterval.getIntervalEnd().format())
                        .append("\n");
            }

        }
        printStream.append("#\n");
        printStream.append("# Regions:\n");
        for (String regionId : statisticsOutputContext.regionIds) {
            printStream.append("#              ")
                    .append(regionId)
                    .append("\n");
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation does nothing.
     *
     *
     * @param bandName   The name of the band the statistics have been computed for.
     * @param regionId   The id of the region the statistics have been computed for.
     * @param statistics The actual statistics as map. Keys are the measure names, values are the actual statistical values.
     */
    @Override
    public void addToOutput(String bandName, String regionId, Map<String, Object> statistics) {
    }

    @Override
    public void addToOutput(String bandName, TimeInterval interval, String regionId, Map<String, Object> statistics) {

    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation does nothing.
     *
     *
     * @throws IOException Never.
     */
    @Override
    public void finaliseOutput() throws IOException {
    }
}
