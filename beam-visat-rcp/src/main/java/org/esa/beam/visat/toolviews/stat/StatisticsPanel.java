/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import com.jidesoft.swing.FolderChooser;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.framework.datamodel.StxFactory;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.statistics.BandNameCreator;
import org.esa.beam.statistics.CsvOutputter;
import org.esa.beam.statistics.ShapefileOutputter;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.VisatApp;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.xy.XIntervalSeries;
import org.jfree.data.xy.XIntervalSeriesCollection;
import org.jfree.ui.RectangleInsets;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import javax.media.jai.Histogram;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A general pane within the statistics window.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 */
class StatisticsPanel extends PagePanel implements MultipleRoiComputePanel.ComputeMasks {

    private static final String DEFAULT_STATISTICS_TEXT = "No statistics computed yet.";  /*I18N*/
    private static final String TITLE_PREFIX = "Statistics";

    private MultipleRoiComputePanel computePanel;
    private JPanel backgroundPanel;
    private AbstractButton hideAndShowButton;
    private JPanel contentPanel;
    private boolean init;

    private final StatisticsPanel.PopupHandler popupHandler;
    private final StringBuilder resultText;
    private Histogram[] histograms;
    private JButton export;

    public StatisticsPanel(final ToolView parentDialog, String helpID) {
        super(parentDialog, helpID, TITLE_PREFIX);
        resultText = new StringBuilder();
        popupHandler = new PopupHandler();
    }

    @Override
    protected void initComponents() {
        init = true;

        computePanel = new MultipleRoiComputePanel(this, getRaster());

        final JPanel helpPanel = GridBagUtils.createPanel();
        GridBagConstraints helpPanelConstraints = GridBagUtils.createConstraints("anchor=NORTHWEST,fill=HORIZONTAL,insets.top=2,weightx=1,ipadx=0");
        GridBagUtils.addToPanel(helpPanel, new JSeparator(), helpPanelConstraints, "gridy=0,gridwidth=3,insets.left=4,insets.right=2");
        GridBagUtils.addToPanel(helpPanel, getHelpButton(), helpPanelConstraints, "gridy=1,gridwidth=1,gridx=2,anchor=EAST,fill=NONE");

        final JPanel rightPanel = GridBagUtils.createPanel();
        GridBagConstraints extendedOptionsPanelConstraints = GridBagUtils.createConstraints("anchor=NORTHWEST,fill=HORIZONTAL,insets.top=2,weightx=1,insets.right=-2");
        GridBagUtils.addToPanel(rightPanel, computePanel, extendedOptionsPanelConstraints, "gridy=0,fill=BOTH,weighty=1");
        GridBagUtils.addToPanel(rightPanel, helpPanel, extendedOptionsPanelConstraints, "gridy=1,anchor=SOUTHWEST,fill=HORIZONTAL,weighty=0");

        final ImageIcon collapseIcon = UIUtils.loadImageIcon("icons/PanelRight12.png");
        final ImageIcon collapseRolloverIcon = ToolButtonFactory.createRolloverIcon(collapseIcon);
        final ImageIcon expandIcon = UIUtils.loadImageIcon("icons/PanelLeft12.png");
        final ImageIcon expandRolloverIcon = ToolButtonFactory.createRolloverIcon(expandIcon);

        hideAndShowButton = ToolButtonFactory.createButton(collapseIcon, false);
        hideAndShowButton.setToolTipText("Collapse Options Panel");
        hideAndShowButton.setName("switchToChartButton");
        hideAndShowButton.addActionListener(new ActionListener() {

            public boolean rightPanelShown;

            @Override
            public void actionPerformed(ActionEvent e) {
                rightPanel.setVisible(rightPanelShown);
                if (rightPanelShown) {
                    hideAndShowButton.setIcon(collapseIcon);
                    hideAndShowButton.setRolloverIcon(collapseRolloverIcon);
                    hideAndShowButton.setToolTipText("Collapse Options Panel");
                } else {
                    hideAndShowButton.setIcon(expandIcon);
                    hideAndShowButton.setRolloverIcon(expandRolloverIcon);
                    hideAndShowButton.setToolTipText("Expand Options Panel");
                }
                rightPanelShown = !rightPanelShown;
            }
        });


        contentPanel = new JPanel(new GridLayout(-1, 1));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.addMouseListener(popupHandler);

        final JScrollPane contentScrollPane = new JScrollPane(contentPanel);
        contentScrollPane.setBorder(null);
        contentScrollPane.setBackground(Color.WHITE);

        export = new JButton("Export");
        export.setVisible(false);

        backgroundPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagUtils.addToPanel(backgroundPanel, contentScrollPane, gbc, "fill=BOTH, weightx=1.0, weighty=1.0, anchor=NORTH");
        GridBagUtils.addToPanel(backgroundPanel, rightPanel, gbc, "gridx=1, fill=VERTICAL, weightx=0.0, gridheight=2");
        GridBagUtils.addToPanel(backgroundPanel, export, gbc, "insets=5, gridx=0, gridy=1, anchor=WEST, weighty=0.0");

        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.add(backgroundPanel);
        layeredPane.add(hideAndShowButton);
        add(layeredPane);
    }

    @Override
    protected void updateComponents() {
        if (!init) {
            initComponents();
        }

        final RasterDataNode raster = getRaster();
        computePanel.setRaster(raster);
        contentPanel.removeAll();
        resultText.setLength(0);

        if (raster != null && raster.isStxSet() && raster.getStx().getResolutionLevel() == 0) {
            resultText.append(createText(raster.getStx(), null));
            contentPanel.add(createStatPanel(raster.getStx(), null));
        } else {
            contentPanel.add(new JLabel(DEFAULT_STATISTICS_TEXT));
            export.setVisible(false);
        }

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private static class ComputeResult {

        final Stx stx;
        final Mask mask;

        ComputeResult(Stx stx, Mask mask) {
            this.stx = stx;
            this.mask = mask;
        }
    }

    @Override
    public void compute(final Mask[] selectedMasks) {
        this.histograms = new Histogram[selectedMasks.length];
        final String title = "Computing Statistics";
        SwingWorker<Object, ComputeResult> swingWorker = new ProgressMonitorSwingWorker<Object, ComputeResult>(this, title) {

            @Override
            protected Object doInBackground(ProgressMonitor pm) {
                pm.beginTask(title, selectedMasks.length);
                try {
                    for (int i = 0; i < selectedMasks.length; i++) {
                        final Mask mask = selectedMasks[i];
                        final Stx stx;
                        ProgressMonitor subPm = SubProgressMonitor.create(pm, 1);
                        if (mask == null) {
                            stx = new StxFactory().withResolutionLevel(0).create(getRaster(), subPm);
                            getRaster().setStx(stx);
                        } else {
                            stx = new StxFactory().withRoiMask(mask).create(getRaster(), subPm);
                        }
                        histograms[i] = stx.getHistogram();
                        publish(new ComputeResult(stx, mask));
                    }
                } finally {
                    pm.done();
                }
                return null;
            }

            @Override
            protected void process(List<ComputeResult> chunks) {

                for (ComputeResult result : chunks) {

                    final Stx stx = result.stx;
                    final Mask mask = result.mask;

                    if (resultText.length() > 0) {
                        resultText.append("\n");
                    }
                    resultText.append(createText(stx, mask));

                    JPanel statPanel = createStatPanel(stx, mask);
                    contentPanel.add(statPanel);
                    contentPanel.revalidate();
                    contentPanel.repaint();
                }

            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(getParentDialogContentPane(),
                                                  "Failed to compute statistics.\nAn error occurred:" + e.getMessage(),
                                                  /*I18N*/
                                                  "Statistics", /*I18N*/
                                                  JOptionPane.ERROR_MESSAGE);
                }
                export.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        JPopupMenu viewPopup = new JPopupMenu("Export");
                        viewPopup.add(new ExportAsCsvAction(selectedMasks));
                        viewPopup.add(new ExportAsShapefileAction(selectedMasks));
                        final Rectangle buttonBounds = export.getBounds();
                        viewPopup.show(export, 1, buttonBounds.height + 1);
                    }
                });
                export.setVisible(true);
            }
        };

        resultText.setLength(0);
        contentPanel.removeAll();

        swingWorker.execute();
    }

    private JPanel createStatPanel(Stx stx, final Mask mask) {
        final Histogram histogram = stx.getHistogram();

        XIntervalSeries histogramSeries = new XIntervalSeries("Histogram");
        int[] bins = histogram.getBins(0);
        for (int j = 0; j < bins.length; j++) {
            histogramSeries.add(histogram.getBinLowValue(0, j),
                                histogram.getBinLowValue(0, j),
                                j < bins.length - 1 ? histogram.getBinLowValue(0, j + 1) : histogram.getHighValue(0),
                                bins[j]);
        }
        ChartPanel histogramPanel = createChartPanel(histogramSeries, "Value", "#Pixels", new Color(0, 0, 127));

        XIntervalSeries percentileSeries = new XIntervalSeries("Percentile");
        percentileSeries.add(0,
                             0,
                             1,
                             histogram.getLowValue(0));
        for (int j = 1; j < 99; j++) {
            percentileSeries.add(j,
                                 j,
                                 j + 1,
                                 histogram.getPTileThreshold(j / 100.0)[0]);
        }
        percentileSeries.add(99,
                             99,
                             100,
                             histogram.getHighValue(0));

        ChartPanel percentilePanel = createChartPanel(percentileSeries, "Percentile (%)", "Value Threshold", new Color(127, 0, 0));

        Object[][] tableData = new Object[][]{
                new Object[]{"#Pixels total:", histogram.getTotals()[0]},
                new Object[]{"Minimum:", histogram.getLowValue()[0]},
                new Object[]{"Maximum:", histogram.getHighValue()[0]},
                new Object[]{"Mean:", histogram.getMean()[0]},
                new Object[]{"Sigma:", histogram.getStandardDeviation()[0]},
                new Object[]{"Median:", histogram.getPTileThreshold(0.5)[0]},
                new Object[]{"P75 threshold:", histogram.getPTileThreshold(0.75)[0]},
                new Object[]{"P80 threshold:", histogram.getPTileThreshold(0.80)[0]},
                new Object[]{"P85 threshold:", histogram.getPTileThreshold(0.85)[0]},
                new Object[]{"P90 threshold:", histogram.getPTileThreshold(0.90)[0]},
                new Object[]{"P95 threshold:", histogram.getPTileThreshold(0.95)[0]},
        };

        JPanel plotContainerPanel = new JPanel(new GridLayout(1, 2));
        plotContainerPanel.add(histogramPanel);
        plotContainerPanel.add(percentilePanel);

        TableModel tableModel = new DefaultTableModel(tableData, new String[]{"Name", "Value"}) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? String.class : Number.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        final JTable table = new JTable(tableModel);
        table.setDefaultRenderer(Number.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final Component label = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                Number number = (Number) value;
                if (value instanceof Float || value instanceof Double) {
                    setHorizontalTextPosition(RIGHT);
                    setText(String.format("%.4f", number.doubleValue()));
                }
                return label;
            }
        });
        table.addMouseListener(popupHandler);

        JPanel textContainerPanel = new JPanel(new BorderLayout(2, 2));
        textContainerPanel.setBackground(Color.WHITE);
        textContainerPanel.add(table, BorderLayout.CENTER);

        JPanel statPanel = new JPanel(new BorderLayout(4, 4));
        statPanel.setBorder(new EmptyBorder(10, 2, 10, 2));
        statPanel.setBackground(Color.WHITE);
        statPanel.add(new JLabel(getSubPanelTitle(mask)), BorderLayout.NORTH);
        statPanel.add(textContainerPanel, BorderLayout.WEST);
        statPanel.add(plotContainerPanel, BorderLayout.CENTER);

        return statPanel;
    }

    private String getSubPanelTitle(Mask mask) {
        final String title;
        if (mask != null) {
            title = String.format("<html><b>%s</b> with ROI-mask <b>%s</b></html>", getRaster().getName(), mask.getName());
        } else {
            title = String.format("<html><b>%s</b></html>", getRaster().getName());
        }
        return title;
    }

    @Override
    protected String getDataAsText() {
        return resultText.toString();
    }

    private String createText(final Stx stx, final Mask mask) {

        if (stx.getSampleCount() == 0) {
            if (mask != null) {
                return "The ROI-Mask '" + mask.getName() + "' is empty.";
            } else {
                return "The scene contains no valid pixels.";
            }
        }

        RasterDataNode raster = getRaster();
        boolean maskUsed = mask != null;
        final String unit = (StringUtils.isNotNullAndNotEmpty(raster.getUnit()) ? raster.getUnit() : "1");
        final long numPixelTotal = (long) raster.getSceneRasterWidth() * (long) raster.getSceneRasterHeight();
        final StringBuilder sb = new StringBuilder(1024);

        sb.append("Only ROI-mask pixels considered:\t");
        sb.append(maskUsed ? "Yes" : "No");
        sb.append("\n");

        if (maskUsed) {
            sb.append("ROI-mask name:\t");
            sb.append(mask.getName());
            sb.append("\n");
        }

        sb.append("Number of pixels total:\t");
        sb.append(numPixelTotal);
        sb.append("\n");

        sb.append("Number of considered pixels:\t");
        sb.append(stx.getSampleCount());
        sb.append("\n");

        sb.append("Ratio of considered pixels:\t");
        sb.append(100.0 * stx.getSampleCount() / numPixelTotal);
        sb.append("\t");
        sb.append("%");
        sb.append("\n");

        sb.append("Minimum:\t");
        sb.append(stx.getMinimum());
        sb.append("\t");
        sb.append(unit);
        sb.append("\n");

        sb.append("Maximum:\t");
        sb.append(stx.getMaximum());
        sb.append("\t");
        sb.append(unit);
        sb.append("\n");

        sb.append("Mean:\t");
        sb.append(stx.getMean());
        sb.append("\t");
        sb.append(unit);
        sb.append("\n");

        sb.append("Standard deviation:\t");
        sb.append(stx.getStandardDeviation());
        sb.append("\t");
        sb.append(unit);
        sb.append("\n");

        sb.append("Coefficient of variation:\t");
        sb.append(getCoefficientOfVariation(stx));
        sb.append("\t");
        sb.append("");
        sb.append("\n");

        sb.append("Median:\t");
        sb.append(stx.getMedian());
        sb.append("\t ");
        sb.append(unit);
        sb.append("\n");

        for (int percentile = 5; percentile <= 95; percentile += 5) {
            sb.append("P").append(percentile).append(" threshold:\t");
            sb.append(stx.getHistogram().getPTileThreshold(percentile / 100.0)[0]);
            sb.append("\t");
            sb.append(unit);
            sb.append("\n");
        }

        return sb.toString();
    }

    private double getCoefficientOfVariation(Stx stx) {
        return stx.getStandardDeviation() / stx.getMean();
    }

    @Override
    public void doLayout() {
        super.doLayout();
        backgroundPanel.setBounds(0, 0, getWidth() - 8, getHeight() - 8);
        hideAndShowButton.setBounds(getWidth() - hideAndShowButton.getWidth() - 12, 6, 24, 24);
    }


    private static ChartPanel createChartPanel(XIntervalSeries percentileSeries, String xAxisLabel, String yAxisLabel, Color color) {
        XIntervalSeriesCollection percentileDataset = new XIntervalSeriesCollection();
        percentileDataset.addSeries(percentileSeries);
        return getHistogramPlotPanel(percentileDataset, xAxisLabel, yAxisLabel, color);
    }

    private static ChartPanel getHistogramPlotPanel(XIntervalSeriesCollection dataset, String xAxisLabel, String yAxisLabel, Color color) {
        JFreeChart chart = ChartFactory.createHistogram(
                null,
                xAxisLabel,
                yAxisLabel,
                dataset,
                PlotOrientation.VERTICAL,
                false,  // Legend?
                true,   // tooltips
                false   // url
        );
        final XYPlot xyPlot = chart.getXYPlot();
        //xyPlot.setForegroundAlpha(0.85f);
        xyPlot.setNoDataMessage("No data");
        xyPlot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));

        final XYBarRenderer renderer = (XYBarRenderer) xyPlot.getRenderer();
        renderer.setDrawBarOutline(false);
        renderer.setShadowVisible(false);
        renderer.setSeriesPaint(0, color);
        StandardXYBarPainter painter = new StandardXYBarPainter();
        renderer.setBarPainter(painter);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(300, 200));
//        chartPanel.getPopupMenu().add(createCopyDataToClipboardMenuItem());
        return chartPanel;
    }

    private class PopupHandler extends MouseAdapter {

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() == 2 || e.isPopupTrigger()) {
                final JPopupMenu menu = new JPopupMenu();
                menu.add(createCopyDataToClipboardMenuItem());
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    private class ExportAsCsvAction extends AbstractAction {

        private static final String PROPERTY_KEY_EXPORT_DIR = "user.statistics.export.dir";
        private final Mask[] masks;

        public ExportAsCsvAction(Mask[] masks) {
            super("Export as CSV");
            this.masks = masks;
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
            File outputAsciiFile;
            int result = fileChooser.showOpenDialog(StatisticsPanel.this);
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

                CsvOutputter csvOutputter = new CsvOutputter(metadataOutputStream, csvOutputStream);

                String[] regionIds = new String[masks.length];
                for (int i = 0; i < masks.length; i++) {
                    if (masks[i] != null) {
                        regionIds[i] = masks[i].getName();
                    } else {
                        regionIds[i] = "\t";
                    }
                }
                csvOutputter.initialiseOutput(
                        new Product[]{getRaster().getProduct()},
                        new String[]{getRaster().getName()},
                        new String[]{
                                "minimum",
                                "maximum",
                                "median",
                                "average",
                                "sigma",
                                "p90",
                                "p95",
                                "total"
                        },
                        null,
                        null,
                        regionIds);

                for (int i = 0; i < histograms.length; i++) {
                    final Histogram histogram = histograms[i];
                    HashMap<String, Number> statistics = new HashMap<String, Number>();
                    statistics.put("minimum", histogram.getLowValue(0));
                    statistics.put("maximum", histogram.getHighValue(0));
                    statistics.put("median", histogram.getPTileThreshold(0.5)[0]);
                    statistics.put("average", histogram.getMean()[0]);
                    statistics.put("sigma", histogram.getStandardDeviation()[0]);
                    statistics.put("p90", histogram.getPTileThreshold(0.9)[0]);
                    statistics.put("p95", histogram.getPTileThreshold(0.95)[0]);
                    statistics.put("total", histogram.getTotals()[0]);
                    csvOutputter.addToOutput(getRaster().getName(), regionIds[i], statistics);
                }


                csvOutputter.finaliseOutput();
            } catch (IOException exception) {
                JOptionPane.showMessageDialog(getParentDialogContentPane(),
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
            JOptionPane.showMessageDialog(getParentDialogContentPane(),
                                          "The statistics have successfully been exported to '" + outputAsciiFile +
                                          "'.",
                                          "Statistics export",
                                          JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private class ExportAsShapefileAction extends AbstractAction {

        private static final String PROPERTY_KEY_EXPORT_DIR = "user.statistics.export.dir";

        private final Mask[] selectedMasks;
        private final Map<SimpleFeatureType, List<VectorDataNode>> featureType2VDN = new HashMap<SimpleFeatureType, List<VectorDataNode>>();
        private final Map<SimpleFeatureType, List<Mask>> featureType2Mask = new HashMap<SimpleFeatureType, List<Mask>>();
        private final Map<Mask, Histogram> mask2Histogram = new HashMap<Mask, Histogram>();
        private final Map<Mask, String> mask2RegionName = new HashMap<Mask, String>();

        public ExportAsShapefileAction(Mask[] selectedMasks) {
            super("Export as Shapefile");
            this.selectedMasks = selectedMasks;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (selectedMasks[0] == null) {
                return;
            }
            boolean hasErrors = false;
            boolean hasSuccess = false;
            String exportDir = VisatApp.getApp().getPreferences().getPropertyString(PROPERTY_KEY_EXPORT_DIR);
            File baseDir = null;
            if (exportDir != null) {
                baseDir = new File(exportDir);
            }
            FolderChooser folderChooser = new FolderChooser(baseDir);
            File outputDirectory;
            int result = folderChooser.showOpenDialog(StatisticsPanel.this);
            if (result == FolderChooser.APPROVE_OPTION) {
                outputDirectory = folderChooser.getSelectedFile();
                VisatApp.getApp().getPreferences().setPropertyString(PROPERTY_KEY_EXPORT_DIR, outputDirectory.toString());
            } else {
                return;
            }

            for (final SimpleFeatureType featureType : getFeatureTypes()) {
                String targetShapefile =
                        outputDirectory.getAbsolutePath() + File.separator + featureType.getTypeName() + ".shp";
                ShapefileOutputter shapefileOutputter = ShapefileOutputter.createShapefileOutputter(
                        featureType,
                        getFeatureCollection(featureType),
                        targetShapefile,
                        new BandNameCreator());

                shapefileOutputter.initialiseOutput(
                        new Product[]{getRaster().getProduct()},
                        new String[]{getRaster().getName()},
                        new String[]{
                                "minimum",
                                "maximum",
                                "median",
                                "average",
                                "sigma",
                                "p90",
                                "p95",
                                "total"
                        },
                        null,
                        null,
                        getRegionIds(featureType));
                for (final Mask mask : getMasks(featureType)) {
                    HashMap<String, Number> statistics = new HashMap<String, Number>();
                    Histogram histogram = getHistogram(mask);
                    statistics.put("minimum", histogram.getLowValue(0));
                    statistics.put("maximum", histogram.getHighValue(0));
                    statistics.put("median", histogram.getPTileThreshold(0.5)[0]);
                    statistics.put("average", histogram.getMean()[0]);
                    statistics.put("sigma", histogram.getStandardDeviation()[0]);
                    statistics.put("p90", histogram.getPTileThreshold(0.9)[0]);
                    statistics.put("p95", histogram.getPTileThreshold(0.95)[0]);
                    statistics.put("total", histogram.getTotals()[0]);
                    shapefileOutputter.addToOutput(getRaster().getName(), mask2RegionName.get(mask), statistics);
                }

                try {
                    shapefileOutputter.finaliseOutput();
                    hasSuccess = true;
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(getParentDialogContentPane(),
                                                  "Unable to export statistics to '" + targetShapefile + "'. Error:\n" +
                                                  e1.getMessage(),
                                                  "Statistics export",
                                                  JOptionPane.ERROR_MESSAGE);
                    hasErrors = true;
                }
            }

            JOptionPane.showMessageDialog(getParentDialogContentPane(),
                                          createErrorMessage(hasErrors, hasSuccess, outputDirectory),
                                          "Statistics export",
                                          JOptionPane.INFORMATION_MESSAGE);
        }

        private String createErrorMessage(boolean hasErrors, boolean hasSuccess, File outputDirectory) {
            StringBuilder message = new StringBuilder();
            if (hasSuccess) {
                message.append("The statistics have successfully been exported to '")
                       .append(outputDirectory)
                       .append("'.");
            }
            if (hasErrors && hasSuccess) {
                message.append("\nHowever, there ");
            }
            if (hasErrors && !hasSuccess) {
                message.append("\nThere ");
            }
            if (hasErrors) {
                message.append("were issues exporting some of the statistics.");
            }
            return message.toString();
        }

        private Histogram getHistogram(Mask mask) {
            return mask2Histogram.get(mask);
        }

        private Mask[] getMasks(SimpleFeatureType featureType) {
            final List<Mask> masks = featureType2Mask.get(featureType);
            return masks.toArray(new Mask[masks.size()]);
        }

        private String[] getRegionIds(SimpleFeatureType featureType) {
            List<String> result = new ArrayList<String>();
            for (VectorDataNode vectorDataNode : featureType2VDN.get(featureType)) {
                // assuming only a single feature per VDN
                FeatureIterator<SimpleFeature> features = vectorDataNode.getFeatureCollection().features();
                SimpleFeature feature = features.next();
                result.add(feature.getIdentifier().toString());
                features.close();
            }
            return result.toArray(new String[result.size()]);
        }

        private FeatureCollection<SimpleFeatureType, SimpleFeature> getFeatureCollection(SimpleFeatureType featureType) {
            DefaultFeatureCollection result = null;
            for (VectorDataNode vectorDataNode : featureType2VDN.get(featureType)) {
                if (result == null) {
                    result = new DefaultFeatureCollection(vectorDataNode.getFeatureCollection());
                } else {
                    result.addAll(vectorDataNode.getFeatureCollection());
                }
            }
            return result;
        }

        private SimpleFeatureType[] getFeatureTypes() {
            List<SimpleFeatureType> result = new ArrayList<SimpleFeatureType>();
            for (int i = 0; i < selectedMasks.length; i++) {
                final Mask selectedMask = selectedMasks[i];
                mask2Histogram.put(selectedMask, histograms[i]);
                if (selectedMask.getImageType().getName().equals(Mask.VectorDataType.TYPE_NAME)) {
                    VectorDataNode vectorDataNode = Mask.VectorDataType.getVectorData(selectedMask);
                    SimpleFeatureType featureType = vectorDataNode.getFeatureType();
                    if (!result.contains(featureType)) {
                        result.add(featureType);
                    }
                    if (!featureType2VDN.containsKey(featureType)) {
                        featureType2VDN.put(featureType, new ArrayList<VectorDataNode>());
                    }
                    if (!featureType2Mask.containsKey(featureType)) {
                        featureType2Mask.put(featureType, new ArrayList<Mask>());
                    }
                    featureType2Mask.get(featureType).add(selectedMask);
                    featureType2VDN.get(featureType).add(vectorDataNode);
                    setMaskRegionName(selectedMask, vectorDataNode);
                }
            }

            return result.toArray(new SimpleFeatureType[result.size()]);
        }

        private void setMaskRegionName(Mask selectedMask, VectorDataNode vectorDataNode) {
            FeatureIterator<SimpleFeature> features = vectorDataNode.getFeatureCollection().features();
            mask2RegionName.put(selectedMask, features.next().getIdentifier().toString());
            features.close();
        }
    }
}
