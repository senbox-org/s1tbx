package org.esa.beam.framework.ui.diagram;

import org.esa.beam.util.io.CsvReader;

import java.io.Reader;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class DiagramGraphIO {

    public static DiagramGraph[] readGraphs(Reader reader) throws IOException {

        CsvReader csvReader = new CsvReader(reader, new char[]{'\t'});
        String[] headerRecord = csvReader.readRecord();
        List<double[]> doubleRecords = csvReader.readDoubleRecords();

        if (headerRecord.length < 2) {
            throw new IOException("Invalid format.");
        }
        for (double[] doubleRecord : doubleRecords) {
            if (doubleRecord.length != headerRecord.length) {
                throw new IOException("Invalid format.");
            }
        }

        DiagramGraph[] graphs = new DiagramGraph[headerRecord.length - 1];
        double[] xValues = new double[doubleRecords.size()];
        for (int j = 0; j < doubleRecords.size(); j++) {
            xValues[j] = doubleRecords.get(0)[j];
        }
        for (int i = 1; i < graphs.length; i++) {
            double[] yValues = new double[doubleRecords.size()];
            for (int j = 0; j < doubleRecords.size(); j++) {
                yValues[j] = doubleRecords.get(i)[j];
            }
            graphs[i] = new DefaultDiagramGraph(headerRecord[0], xValues, headerRecord[i], yValues);
        }
        return graphs;
    }

    public static void writeGraphs(DiagramGraph[] graphs, Writer writer) throws IOException {
        DiagramGraph graph0 = graphs[0];

        writer.write(graph0.getXName());
        for (DiagramGraph graph : graphs) {
            writer.write((int) '\t');
            writer.write(graph.getYName());
        }
        writer.write((int) '\n');

        int numValues = graph0.getNumValues();
        for (int i = 0; i < numValues; i++) {
            writer.write(String.valueOf(graph0.getXValueAt(i)));
            for (DiagramGraph graph : graphs) {
                writer.write((int) '\t');
                writer.write(String.valueOf(graph.getYValueAt(i)));
            }
            writer.write((int) '\n');
        }
    }

}
