/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.s1tbx.analysis.rcp.toolviews.timeseries;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.io.CsvReader;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.ui.SnapFileChooser;
import org.esa.snap.ui.diagram.DefaultDiagramGraph;
import org.esa.snap.ui.diagram.DiagramGraph;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class DiagramGraphIO {
    public static final SnapFileFilter CSV_FILE_FILTER = new SnapFileFilter("CSV", ".csv", "CSV (plain text)");

    public static final String DIAGRAM_GRAPH_IO_LAST_DIR_KEY = "diagramGraphIO.lastDir";

    public static DiagramGraph[] readGraphs(Reader reader) throws IOException {

        CsvReader csvReader = new CsvReader(reader, new char[]{'\t'});
        List<DiagramGraph> graphGroup = new ArrayList<>(5);
        List<double[]> dataRecords = new ArrayList<>(20);

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

    public static double[] toDoubles(String[] textRecord) {
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
        List<List<DiagramGraph>> graphGroups = new ArrayList<>(3);
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
                ArrayList<DiagramGraph> graphGroup = new ArrayList<>(3);
                graphGroup.add(graph);
                graphGroups.add(graphGroup);
            }
        }
        return graphGroups;
    }

    private static void writeGraphGroup(List<DiagramGraph> graphGroup, Writer writer) throws IOException {
        DiagramGraph graph0 = graphGroup.get(0);

        writer.write("Date");
        writer.write((int) '\t');
        writer.write(graph0.getXName());

        for (DiagramGraph graph : graphGroup) {
            writer.write((int) '\t');
            writer.write(graph.getYName());
        }
        writer.write((int) '\n');

        int numValues = graph0.getNumValues();
        for (int i = 0; i < numValues; i++) {
            writer.write(toDate(graph0.getXValueAt(i)));
            writer.write((int) '\t');
            writer.write(String.valueOf(graph0.getXValueAt(i)));

            for (DiagramGraph graph : graphGroup) {
                writer.write((int) '\t');
                writer.write(String.valueOf(graph.getYValueAt(i)));
            }
            writer.write((int) '\n');
        }
    }

    private static String toDate(double time) {
        final ProductData.UTC newTime = new ProductData.UTC(time);
        return DateAxis.dateFormat.format(newTime.getAsDate());
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

    public static DiagramGraph[] readGraphs(Component parentComponent,
                                            String title,
                                            SnapFileFilter[] fileFilters,
                                            Preferences preferences) {
        File selectedFile = selectGraphFile(parentComponent, title, fileFilters, preferences, true);
        if (selectedFile != null) {
            try {
                try (FileReader fileReader = new FileReader(selectedFile)) {
                    return readGraphs(fileReader);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(parentComponent, "I/O error: " + e.getMessage());
            }
        }
        return new DiagramGraph[0];
    }

    public static void writeGraphs(Component parentComponent,
                                   String title,
                                   SnapFileFilter[] fileFilters,
                                   Preferences preferences,
                                   DiagramGraph[] graphs) {
        if (graphs.length == 0) {
            JOptionPane.showMessageDialog(parentComponent, "Nothing to save.");
            return;
        }
        File selectedFile = selectGraphFile(parentComponent, title, fileFilters, preferences, false);
        if (selectedFile != null) {
            try {
                writeCSV(selectedFile, graphs);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(parentComponent, "I/O error: " + e.getMessage());
            }
        }
    }

    private static void writeCSV(final File selectedFile, final DiagramGraph[] graphs) throws IOException {
        try (FileWriter fileWriter = new FileWriter(selectedFile)) {
            writeGraphs(graphs, fileWriter);
        }
    }

    private static File selectGraphFile(Component parentComponent,
                                        String title,
                                        SnapFileFilter[] fileFilters,
                                        Preferences preferences,
                                        boolean open) {
        String lastDirPath = preferences.get(DIAGRAM_GRAPH_IO_LAST_DIR_KEY, ".");
        SnapFileChooser fileChooser = new SnapFileChooser(new File(lastDirPath));
        fileChooser.setAcceptAllFileFilterUsed(true);
        fileChooser.setDialogTitle(title);
        for (SnapFileFilter fileFilter : fileFilters) {
            fileChooser.addChoosableFileFilter(fileFilter);
        }
        fileChooser.setFileFilter(fileFilters[0]);
        if (open) {
            fileChooser.setDialogType(SnapFileChooser.OPEN_DIALOG);
        } else {
            fileChooser.setDialogType(SnapFileChooser.SAVE_DIALOG);
        }
        File selectedFile;
        while (true) {
            int i = fileChooser.showDialog(parentComponent, null);
            if (i == SnapFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                if (open || !selectedFile.exists()) {
                    break;
                }
                i = JOptionPane.showConfirmDialog(parentComponent,
                                                  "The file\n" + selectedFile + "\nalready exists.\nOverwrite?",
                                                  "File exists", JOptionPane.YES_NO_CANCEL_OPTION);
                if (i == JOptionPane.CANCEL_OPTION) {
                    // Canceled
                    selectedFile = null;
                    break;
                } else if (i == JOptionPane.YES_OPTION) {
                    // Overwrite existing file
                    break;
                }
            } else {
                // Canceled
                selectedFile = null;
                break;
            }
        }
        if (selectedFile != null) {
            preferences.put(DIAGRAM_GRAPH_IO_LAST_DIR_KEY, selectedFile.getParent());
        }
        return selectedFile;
    }
}
