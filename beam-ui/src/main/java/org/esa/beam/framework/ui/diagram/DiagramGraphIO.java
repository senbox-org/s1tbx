package org.esa.beam.framework.ui.diagram;

import org.esa.beam.util.io.CsvReader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class DiagramGraphIO {

    public static DiagramGraph[] readGraphs(Reader reader) throws IOException {

        CsvReader csvReader = new CsvReader(reader, new char[]{'\t'});
        List<DiagramGraph> graphGroup = new ArrayList<DiagramGraph>(5);
        List<double[]> dataRecords = new ArrayList<double[]>(20);

        String[] headerRecord = csvReader.readRecord();
        while (true) {
            if (headerRecord.length < 2) {
                throw new IOException("Invalid format.");
            }
            String[] record = csvReader.readRecord();
            if (record == null) {
                break;
            }
            double[] dataRecord = toDoubles(record);
            if (dataRecord != null) {
                if (dataRecord.length != headerRecord.length) {
                    throw new IOException("Invalid format.");
                }
                dataRecords.add(dataRecord);
            } else {
                readGraphGroup(headerRecord, dataRecords, graphGroup);
                headerRecord = record;
            }
        }
        readGraphGroup(headerRecord, dataRecords, graphGroup);
        return graphGroup.toArray(new DiagramGraph[0]);
    }

    public static void writeGraphs(DiagramGraph[] graphs, Writer writer) throws IOException {
        List<List<DiagramGraph>> graphGroups = computeGraphGroups(graphs);
        for (List<DiagramGraph> graphGroup : graphGroups) {
            writeGraphGroup(graphGroup, writer);
        }
    }

    private static void readGraphGroup(String[] headerRecord, List<double[]> dataRecords, List<DiagramGraph> graphs) {
        if (dataRecords.size() > 0) {
            double[] xValues = new double[dataRecords.size()];
            for (int j = 0; j < dataRecords.size(); j++) {
                xValues[j] = dataRecords.get(j)[0];
            }
            double[] dataRecord0 = dataRecords.get(0);
            for (int i = 1; i < dataRecord0.length; i++) {
                double[] yValues = new double[dataRecords.size()];
                for (int j = 0; j < dataRecords.size(); j++) {
                    yValues[j] = dataRecords.get(j)[i];
                }
                graphs.add(new DefaultDiagramGraph(headerRecord[0], xValues,
                                                   headerRecord[i], yValues));
            }
        }
        dataRecords.clear();
    }

    public static double[] toDoubles(String[] textRecord) throws IOException {
        double[] doubleRecord = new double[textRecord.length];
        for (int i = 0; i < textRecord.length; i++) {
            try {
                doubleRecord[i] = Double.valueOf(textRecord[i]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return doubleRecord;
    }

    private static List<List<DiagramGraph>> computeGraphGroups(DiagramGraph[] graphs) {
        List<List<DiagramGraph>> graphGroups = new ArrayList<List<DiagramGraph>>(3);
        for (DiagramGraph graph : graphs) {
            boolean found = false;
            for (List<DiagramGraph> graphGroup : graphGroups) {
                if (equalXValues(graph, graphGroup.get(0))) {
                    graphGroup.add(graph);
                    found = true;
                    break;
                }
            }
            if (!found) {
                ArrayList<DiagramGraph> graphGroup = new ArrayList<DiagramGraph>(3);
                graphGroup.add(graph);
                graphGroups.add(graphGroup);
            }
        }
        return graphGroups;
    }

    private static void writeGraphGroup(List<DiagramGraph> graphGroup, Writer writer) throws IOException {
        DiagramGraph graph0 = graphGroup.get(0);

        writer.write(graph0.getXName());
        for (DiagramGraph graph : graphGroup) {
            writer.write((int) '\t');
            writer.write(graph.getYName());
        }
        writer.write((int) '\n');

        int numValues = graph0.getNumValues();
        for (int i = 0; i < numValues; i++) {
            writer.write(String.valueOf(graph0.getXValueAt(i)));
            for (DiagramGraph graph : graphGroup) {
                writer.write((int) '\t');
                writer.write(String.valueOf(graph.getYValueAt(i)));
            }
            writer.write((int) '\n');
        }
    }

    public static boolean equalXValues(DiagramGraph g1, DiagramGraph g2) {
        if (g1.getNumValues() != g2.getNumValues()) {
            return false;
        }
        for (int i = 0; i < g1.getNumValues(); i++) {
            if (Math.abs(g1.getXValueAt(i) - g2.getXValueAt(i)) > 1.0e-10) {
                return false;
            }
        }
        return true;
    }

}
