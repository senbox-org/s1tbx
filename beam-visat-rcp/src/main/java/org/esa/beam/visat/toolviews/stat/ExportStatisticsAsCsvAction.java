/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.visat.toolviews.stat;

import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.statistics.output.CsvStatisticsWriter;
import org.esa.beam.statistics.output.MetadataWriter;
import org.esa.beam.statistics.output.StatisticsOutputContext;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.VisatApp;

import javax.media.jai.Histogram;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Thomas Storm
 */
class ExportStatisticsAsCsvAction extends AbstractAction {

    private static final String PROPERTY_KEY_EXPORT_DIR = "user.statistics.export.dir";
    private Mask[] selectedMasks;
    private final StatisticalExportContext dataProvider;

    public ExportStatisticsAsCsvAction(StatisticalExportContext dataProvider) {
        super("Export as CSV");
        this.dataProvider = dataProvider;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        PrintStream metadataOutputStream = null;
        PrintStream csvOutputStream = null;
        String exportDir = VisatApp.getApp().getPreferences().getPropertyString(PROPERTY_KEY_EXPORT_DIR);
        File baseDir = null;
        if (exportDir != null) {
            baseDir = new File(exportDir);
        }
        BeamFileChooser fileChooser = new BeamFileChooser(baseDir);
        final BeamFileFilter beamFileFilter = new BeamFileFilter("CSV", new String[]{".csv", ".txt"}, "CSV files");
        fileChooser.setFileFilter(beamFileFilter);
        File outputAsciiFile;
        int result = fileChooser.showSaveDialog(VisatApp.getApp().getApplicationWindow());
        if (result == JFileChooser.APPROVE_OPTION) {
            outputAsciiFile = fileChooser.getSelectedFile();
            VisatApp.getApp().getPreferences().setPropertyString(PROPERTY_KEY_EXPORT_DIR, outputAsciiFile.getParent());
        } else {
            return;
        }
        try {
            final StringBuilder metadataFileName = new StringBuilder(FileUtils.getFilenameWithoutExtension(outputAsciiFile));
            metadataFileName.append("_metadata.txt");
            final File metadataFile = new File(outputAsciiFile.getParent(), metadataFileName.toString());
            metadataOutputStream = new PrintStream(new FileOutputStream(metadataFile));
            csvOutputStream = new PrintStream(new FileOutputStream(outputAsciiFile));

            CsvStatisticsWriter csvStatisticsWriter = new CsvStatisticsWriter(csvOutputStream);
            final MetadataWriter metadataWriter = new MetadataWriter(metadataOutputStream);

            String[] regionIds;
            if (selectedMasks != null) {
                regionIds = new String[selectedMasks.length];
                for (int i = 0; i < selectedMasks.length; i++) {
                    if (selectedMasks[i] != null) {
                        regionIds[i] = selectedMasks[i].getName();
                    } else {
                        regionIds[i] = "\t";
                    }
                }
            } else {
                regionIds = new String[]{"full_scene"};
            }
            final String[] algorithmNames = {
                    "minimum",
                    "maximum",
                    "median",
                    "average",
                    "sigma",
                    "p90_threshold",
                    "p95_threshold",
                    "total"
            };
            final StatisticsOutputContext outputContext = StatisticsOutputContext.create(
                    new Product[]{dataProvider.getRasterDataNode().getProduct()}, algorithmNames, regionIds);
            metadataWriter.initialiseOutput(outputContext);
            csvStatisticsWriter.initialiseOutput(outputContext);

            final Map<String, Number> statistics = new HashMap<String, Number>();
            final Histogram[] histograms = dataProvider.getHistograms();
            for (int i = 0; i < histograms.length; i++) {
                final Histogram histogram = histograms[i];
                statistics.put("minimum", histogram.getLowValue(0));
                statistics.put("maximum", histogram.getHighValue(0));
                statistics.put("median", histogram.getPTileThreshold(0.5)[0]);
                statistics.put("average", histogram.getMean()[0]);
                statistics.put("sigma", histogram.getStandardDeviation()[0]);
                statistics.put("p90_threshold", histogram.getPTileThreshold(0.9)[0]);
                statistics.put("p95_threshold", histogram.getPTileThreshold(0.95)[0]);
                statistics.put("total", histogram.getTotals()[0]);
                csvStatisticsWriter.addToOutput(dataProvider.getRasterDataNode().getName(), regionIds[i], statistics);
                metadataWriter.addToOutput(dataProvider.getRasterDataNode().getName(), regionIds[i], statistics);
                statistics.clear();
            }
            csvStatisticsWriter.finaliseOutput();
            metadataWriter.finaliseOutput();
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(VisatApp.getApp().getApplicationWindow(),
                                          "Failed to export statistics.\nAn error occurred:" +
                                                  exception.getMessage(),
                                          "Statistics export",
                                          JOptionPane.ERROR_MESSAGE);
        } finally {
            if (metadataOutputStream != null) {
                metadataOutputStream.close();
            }
            if (csvOutputStream != null) {
                csvOutputStream.close();
            }
        }
        JOptionPane.showMessageDialog(VisatApp.getApp().getApplicationWindow(),
                                      "The statistics have successfully been exported to '" + outputAsciiFile +
                                              "'.",
                                      "Statistics export",
                                      JOptionPane.INFORMATION_MESSAGE);
    }

    public void setSelectedMasks(Mask[] selectedMasks) {
        this.selectedMasks = selectedMasks;
    }
}
