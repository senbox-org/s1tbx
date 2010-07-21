/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.framework.ui.diagram;

import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.CsvReader;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DiagramGraphIO {
    public static final BeamFileFilter CSV_FILE_FILTER = new BeamFileFilter("CSV", ".csv", "CSV (plain text)");
    public static final BeamFileFilter SPECTRA_CSV_FILE_FILTER = new BeamFileFilter("Spectra-CSV", ".csv", "Spectra CSV");

    public static final String DIAGRAM_GRAPH_IO_LAST_DIR_KEY = "diagramGraphIO.lastDir";

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


    public static DiagramGraph[] readGraphs(Component parentComponent,
                                            String title,
                                            BeamFileFilter[] fileFilters,
                                            PropertyMap preferences) {
        File selectedFile = selectGraphFile(parentComponent, title, fileFilters, preferences, true);
        if (selectedFile != null) {
            try {
                FileReader fileReader = new FileReader(selectedFile);
                try {
                    return readGraphs(fileReader);
                } finally {
                    fileReader.close();
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(parentComponent, "I/O error: " + e.getMessage());
            }
        }
        return new DiagramGraph[0];
    }

    public static void writeGraphs(Component parentComponent,
                                   String title,
                                   BeamFileFilter[] fileFilters,
                                   PropertyMap preferences,
                                   DiagramGraph[] graphs) {
        if (graphs.length == 0) {
            JOptionPane.showMessageDialog(parentComponent, "Nothing to save.");
            return;
        }
        File selectedFile = selectGraphFile(parentComponent, title, fileFilters, preferences, false);
        if (selectedFile != null) {

            try {
                FileWriter fileWriter = new FileWriter(selectedFile);
                try {
                    writeGraphs(graphs, fileWriter);
                } finally {
                    fileWriter.close();
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(parentComponent, "I/O error: " + e.getMessage());
            }
        }
    }

    private static File selectGraphFile(Component parentComponent,
                                        String title,
                                        BeamFileFilter[] fileFilters,
                                        PropertyMap preferences,
                                        boolean open) {
        String lastDirPath = preferences.getPropertyString(DIAGRAM_GRAPH_IO_LAST_DIR_KEY, ".");
        BeamFileChooser fileChooser = new BeamFileChooser(new File(lastDirPath));
        fileChooser.setAcceptAllFileFilterUsed(true);
        fileChooser.setDialogTitle(title);
        for (BeamFileFilter fileFilter : fileFilters) {
            fileChooser.addChoosableFileFilter(fileFilter);
        }
        fileChooser.setFileFilter(fileFilters[0]);
        if (open) {
            fileChooser.setDialogType(BeamFileChooser.OPEN_DIALOG);
        } else {
            fileChooser.setDialogType(BeamFileChooser.SAVE_DIALOG);
        }
        File selectedFile;
        while (true) {
            int i = fileChooser.showDialog(parentComponent, null);
            if (i == BeamFileChooser.APPROVE_OPTION) {
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
            preferences.setPropertyString(DIAGRAM_GRAPH_IO_LAST_DIR_KEY, selectedFile.getParent());
        }
        return selectedFile;
    }
}
