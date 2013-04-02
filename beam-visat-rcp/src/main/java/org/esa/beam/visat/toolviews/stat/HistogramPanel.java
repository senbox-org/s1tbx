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

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.binding.Binding;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.Enablement;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.framework.datamodel.StxFactory;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.math.MathUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.xy.XIntervalSeries;
import org.jfree.data.xy.XIntervalSeriesCollection;
import org.jfree.ui.RectangleInsets;

import javax.media.jai.Histogram;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ExecutionException;

/**
 * A pane within the statistcs window which displays a histogram.
 */
class HistogramPanel extends ChartPagePanel {

    private static final String NO_DATA_MESSAGE = "No histogram computed yet.\n" +
            "If a band is selected, a histogram can be created by hitting the 'Refresh View' button.\n" +
            HELP_TIP_MESSAGE + "\n" +
            ZOOM_TIP_MESSAGE;
    private static final String CHART_TITLE = "Histogram";

    public final static String PROPERTY_NAME_AUTO_MIN_MAX = "autoMinMax";
    public final static String PROPERTY_NAME_MIN = "min";
    public final static String PROPERTY_NAME_MAX = "max";
    public static final String PROPERTY_NAME_NUM_BINS = "numBins";
    public static final String PROPERTY_NAME_LOGARITHMIC_HISTOGRAM = "histogramLogScaled";
    public static final String PROPERTY_NAME_LOG_SCALED = "xAxisLogScaled";
    public final static String PROPERTY_NAME_USE_ROI_MASK = "useRoiMask";
    public final static String PROPERTY_NAME_ROI_MASK = "roiMask";

    private static final double HISTO_MIN_DEFAULT = 0.0;
    private static final double HISTO_MAX_DEFAULT = 100.0;
    private static final int NUM_BINS_DEFAULT = 512;

    private AxisRangeControl xAxisRangeControl;
    private XIntervalSeriesCollection dataset;
    private JFreeChart chart;

    private Stx stx;
    private HistogramPlotConfig histogramPlotConfig;
    private BindingContext bindingContext;

    private boolean isInitialized;
    private boolean histogramComputing;
    private Enablement log10AxisEnablement;
    private Enablement log10HistEnablement;

    HistogramPanel(final ToolView parentDialog, String helpID) {
        super(parentDialog, helpID, CHART_TITLE, true);
    }

    @Override
    protected void initComponents() {
        xAxisRangeControl = new AxisRangeControl("X-Axis");

        histogramPlotConfig = new HistogramPlotConfig();
        bindingContext = new BindingContext(PropertyContainer.createObjectBacked(histogramPlotConfig));

        createUI();
        initActionEnablers();
        updateComponents();
    }

    @Override
    protected void updateComponents() {
        if (!isInitialized || !isVisible()) {
            return;
        }

        super.updateComponents();
        chart.setTitle(getRaster() != null ? CHART_TITLE + " for " + getRaster().getName() : CHART_TITLE);
        updateXAxis();
        if (xAxisRangeControl.isAutoMinMax()) {
            xAxisRangeControl.getBindingContext().getPropertySet().getDescriptor("min").setDefaultValue(
                    HISTO_MIN_DEFAULT);
            xAxisRangeControl.getBindingContext().getPropertySet().getDescriptor("max").setDefaultValue(
                    HISTO_MAX_DEFAULT);
        }
        dataset = null;
        this.stx = null;
        handleStxChange();
        refreshButton.setEnabled(getRaster() != null);
    }

    @Override
    protected boolean mustHandleSelectionChange() {
        return isRasterChanged();
    }

    @Override
    public void nodeDataChanged(ProductNodeEvent event) {
        super.nodeDataChanged(event);
        if (!histogramPlotConfig.useRoiMask) {
            return;
        }
        final Mask roiMask = histogramPlotConfig.roiMask;
        if (roiMask == null) {
            return;
        }
        final ProductNode sourceNode = event.getSourceNode();
        if (!(sourceNode instanceof Mask)) {
            return;
        }
        final String maskName = ((Mask) sourceNode).getName();
        if (roiMask.getName().equals(maskName)) {
            updateComponents();
        }
    }

    private void createUI() {
        dataset = new XIntervalSeriesCollection();
        chart = ChartFactory.createHistogram(
                CHART_TITLE,
                "Values",
                "Frequency in #pixels",
                dataset,
                PlotOrientation.VERTICAL,
                false,  // Legend?
                true,   // tooltips
                false   // url
        );
        final XYPlot xyPlot = chart.getXYPlot();
        xyPlot.setDomainZeroBaselineStroke(new BasicStroke(0.2f));

        final XYBarRenderer renderer = (XYBarRenderer) xyPlot.getRenderer();
        renderer.setDrawBarOutline(false);
        renderer.setShadowVisible(false);
        renderer.setShadowYOffset(-4.0);
        renderer.setBaseToolTipGenerator(new XYPlotToolTipGenerator());
        renderer.setBarPainter(new StandardXYBarPainter());
        renderer.setSeriesPaint(0, new Color(0, 0, 200));

        createUI(createChartPanel(chart), createOptionsPanel(), bindingContext);

        isInitialized = true;

        final Binding minBinding = xAxisRangeControl.getBindingContext().getBinding("min");
        final double min = (Double) minBinding.getPropertyValue();
        final Binding maxBinding = xAxisRangeControl.getBindingContext().getBinding("max");
        final double max = (Double) maxBinding.getPropertyValue();
        if (!histogramComputing && min > max) {
            minBinding.setPropertyValue(max);
            maxBinding.setPropertyValue(min);
        }
        updateXAxis();
    }

    private void initActionEnablers() {
        RefreshActionEnabler rangeControlActionEnabler = new RefreshActionEnabler(refreshButton, PROPERTY_NAME_MIN,
                                                                                  PROPERTY_NAME_AUTO_MIN_MAX,
                                                                                  PROPERTY_NAME_MAX,
                                                                                  PROPERTY_NAME_LOGARITHMIC_HISTOGRAM);
        xAxisRangeControl.getBindingContext().addPropertyChangeListener(rangeControlActionEnabler);
        RefreshActionEnabler componentsActionEnabler = new RefreshActionEnabler(refreshButton, PROPERTY_NAME_NUM_BINS,
                                                                                PROPERTY_NAME_USE_ROI_MASK,
                                                                                PROPERTY_NAME_ROI_MASK);
        bindingContext.addPropertyChangeListener(componentsActionEnabler);
    }

    private JPanel createOptionsPanel() {
        final JLabel numBinsLabel = new JLabel("#Bins:");
        JTextField numBinsField = new JTextField(Integer.toString(NUM_BINS_DEFAULT));
        numBinsField.setPreferredSize(new Dimension(50, numBinsField.getPreferredSize().height));
        final JCheckBox histoLogCheck = new JCheckBox("Log10 scaled bins");

        bindingContext.getPropertySet().getDescriptor(PROPERTY_NAME_NUM_BINS).setDescription(
                "Set the number of bins in the histogram");
        bindingContext.getPropertySet().getDescriptor(PROPERTY_NAME_NUM_BINS).setValueRange(
                new ValueRange(2.0, 2048.0));
        bindingContext.getPropertySet().getDescriptor(PROPERTY_NAME_NUM_BINS).setDefaultValue(NUM_BINS_DEFAULT);
        bindingContext.bind(PROPERTY_NAME_NUM_BINS, numBinsField);

        bindingContext.getPropertySet().getDescriptor(PROPERTY_NAME_LOGARITHMIC_HISTOGRAM).setDescription(
                "Use log-10 scaled values for computation of histogram");
        bindingContext.getPropertySet().getDescriptor(PROPERTY_NAME_LOGARITHMIC_HISTOGRAM).setDefaultValue(false);
        bindingContext.bind(PROPERTY_NAME_LOGARITHMIC_HISTOGRAM, histoLogCheck);
        log10HistEnablement = bindingContext.bindEnabledState(PROPERTY_NAME_LOGARITHMIC_HISTOGRAM, true, new Enablement.Condition() {
            @Override
            public boolean evaluate(BindingContext bindingContext) {
                return getRaster() != null && getRaster().getStx().getMaximum() > 0;
            }
        });

        PropertyChangeListener logChangeListener = new AxisControlChangeListener();

        xAxisRangeControl.getBindingContext().addPropertyChangeListener(logChangeListener);
        xAxisRangeControl.getBindingContext().getPropertySet().addProperty(
                bindingContext.getPropertySet().getProperty(PROPERTY_NAME_LOGARITHMIC_HISTOGRAM));
        xAxisRangeControl.getBindingContext().getPropertySet().addProperty(
                bindingContext.getPropertySet().getProperty(PROPERTY_NAME_LOG_SCALED));
        xAxisRangeControl.getBindingContext().getPropertySet().getDescriptor(PROPERTY_NAME_LOG_SCALED).setDescription(
                "Toggle whether to use a logarithmic x-axis");
        log10AxisEnablement = xAxisRangeControl.getBindingContext().bindEnabledState(PROPERTY_NAME_LOG_SCALED, true, new Enablement.Condition() {
            @Override
            public boolean evaluate(BindingContext bindingContext) {
                return dataset != null && stx != null && stx.getMinimum() > 0 && !stx.isLogHistogram();
            }
        });


        JPanel dataSourceOptionsPanel = GridBagUtils.createPanel();
        GridBagConstraints dataSourceOptionsConstraints = GridBagUtils.createConstraints(
                "anchor=NORTHWEST,fill=HORIZONTAL,insets.top=2");
        GridBagUtils.addToPanel(dataSourceOptionsPanel, new JLabel(" "), dataSourceOptionsConstraints,
                                "gridwidth=2,gridy=0,gridx=0,weightx=0");
        GridBagUtils.addToPanel(dataSourceOptionsPanel, numBinsLabel, dataSourceOptionsConstraints,
                                "insets.top=2,insets.left=4,gridwidth=1,gridy=1,gridx=0,weightx=1");
        GridBagUtils.addToPanel(dataSourceOptionsPanel, numBinsField, dataSourceOptionsConstraints,
                                "insets.top=0,insets.left=0,insets.right=2,gridwidth=1,gridy=1,gridx=1");
        GridBagUtils.addToPanel(dataSourceOptionsPanel, histoLogCheck, dataSourceOptionsConstraints,
                                "insets.right=0,gridwidth=2,gridy=2,gridx=0");

        xAxisRangeControl.getBindingContext().bind(PROPERTY_NAME_LOG_SCALED, new JCheckBox("Log10 scaled"));
        xAxisRangeControl.getBindingContext().addPropertyChangeListener(PROPERTY_NAME_LOG_SCALED, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                ValueAxis oldAxis = chart.getXYPlot().getDomainAxis();
                ValueAxis newAxis = StatisticChartStyling.updateScalingOfAxis((Boolean) evt.getNewValue(), oldAxis, true);
                chart.getXYPlot().setDomainAxis(newAxis);
            }
        });

        JPanel displayOptionsPanel = GridBagUtils.createPanel();
        GridBagConstraints displayOptionsConstraints = GridBagUtils.createConstraints(
                "anchor=SOUTH,fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(displayOptionsPanel, xAxisRangeControl.getPanel(), displayOptionsConstraints,
                                "gridy=2");

        JPanel optionsPanel = GridBagUtils.createPanel();
        GridBagConstraints gbc = GridBagUtils.createConstraints(
                "anchor=NORTHWEST,fill=HORIZONTAL,insets.top=2,weightx=1");
        GridBagUtils.addToPanel(optionsPanel, dataSourceOptionsPanel, gbc, "gridy=0");
        GridBagUtils.addToPanel(optionsPanel, new JPanel(), gbc, "gridy=1,fill=VERTICAL,weighty=1");
        GridBagUtils.addToPanel(optionsPanel, displayOptionsPanel, gbc, "gridy=2,fill=HORIZONTAL,weighty=0");
        GridBagUtils.addToPanel(optionsPanel, new JPanel(), gbc, "gridy=3,fill=VERTICAL,weighty=1");
        GridBagUtils.addToPanel(optionsPanel, xAxisRangeControl.getBindingContext().getBinding(
                PROPERTY_NAME_LOG_SCALED).getComponents()[0], gbc, "gridy=4");
        return optionsPanel;
    }

    private ChartPanel createChartPanel(JFreeChart chart) {
        XYPlot plot = chart.getXYPlot();

        plot.setForegroundAlpha(0.85f);
        plot.setNoDataMessage(NO_DATA_MESSAGE);
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));

        ChartPanel chartPanel = new ChartPanel(chart);

        MaskSelectionToolSupport maskSelectionToolSupport = new MaskSelectionToolSupport(this,
                                                                                         chartPanel,
                                                                                         "histogram_plot_area",
                                                                                         "Mask generated from selected histogram plot area",
                                                                                         Color.RED,
                                                                                         PlotAreaSelectionTool.AreaType.X_RANGE) {

            @Override
            protected String createMaskExpression(PlotAreaSelectionTool.AreaType areaType, Shape shape) {
                Rectangle2D bounds = shape.getBounds2D();
                return createMaskExpression(bounds.getMinX(), bounds.getMaxX());
            }

            protected String createMaskExpression(double x1, double x2) {
                String bandName = BandArithmetic.createExternalName(getRaster().getName());
                return String.format("%s >= %s && %s <= %s",
                                     bandName,
                                     stx != null ? stx.getHistogramScaling().scaleInverse(x1) : x1,
                                     bandName,
                                     stx != null ? stx.getHistogramScaling().scaleInverse(x2) : x2);
            }
        };

        chartPanel.getPopupMenu().addSeparator();
        chartPanel.getPopupMenu().add(maskSelectionToolSupport.createMaskSelectionModeMenuItem());
        chartPanel.getPopupMenu().add(maskSelectionToolSupport.createDeleteMaskMenuItem());
        chartPanel.getPopupMenu().addSeparator();
        chartPanel.getPopupMenu().add(createCopyDataToClipboardMenuItem());
        return chartPanel;
    }

    private static class HistogramPlotConfig {

        private boolean xAxisLogScaled;
        private boolean histogramLogScaled;
        private int numBins = NUM_BINS_DEFAULT;
        private boolean useRoiMask;
        private Mask roiMask;
    }

    @Override
    public void updateChartData() {
        final boolean autoMinMaxEnabled = getAutoMinMaxEnabled();
        final Double min;
        final Double max;
        if (autoMinMaxEnabled) {
            min = null;
            max = null;
        } else {
            min = (Double) xAxisRangeControl.getBindingContext().getBinding("min").getPropertyValue();
            max = (Double) xAxisRangeControl.getBindingContext().getBinding("max").getPropertyValue();
        }
        ProgressMonitorSwingWorker<Stx, Object> swingWorker = new StxWorker(min, max, autoMinMaxEnabled);
        swingWorker.execute();
    }

    private void setStx(Stx stx) {
        if (stx != null) {
            this.stx = stx;
            dataset = new XIntervalSeriesCollection();
            final int[] binCounts = this.stx.getHistogramBins();
            final RasterDataNode raster = getRaster();
            final XIntervalSeries series = new XIntervalSeries(raster.getName());
            final Histogram histogram = this.stx.getHistogram();
            for (int i = 0; i < binCounts.length; i++) {
                final double xMin = histogram.getBinLowValue(0, i);
                final double xMax = i < binCounts.length - 1 ? histogram.getBinLowValue(0,
                                                                                        i + 1) : histogram.getHighValue(
                        0);
                series.add(xMin, xMin, xMax, binCounts[i]);
            }
            dataset.addSeries(series);
        }
        handleStxChange();
    }

    private void handleStxChange() {
        boolean noStx = this.stx == null;
        if ((!noStx && !this.stx.isLogHistogram()) || (noStx && histogramPlotConfig.histogramLogScaled)) {
            bindingContext.getBinding(PROPERTY_NAME_LOGARITHMIC_HISTOGRAM).setPropertyValue(Boolean.FALSE);
            bindingContext.adjustComponents();
            if (!noStx && !this.stx.isLogHistogram()) {
                refreshButton.setEnabled(false);
            }
        }
        log10HistEnablement.apply();
        updateLogXAxisCheckBox();
        chart.getXYPlot().setDataset(dataset);
        updateXAxis();
        chart.fireChartChanged();
    }

    private String getAxisLabel() {
        boolean logScaled = (Boolean) bindingContext.getBinding(PROPERTY_NAME_LOGARITHMIC_HISTOGRAM).getPropertyValue();
        return StatisticChartStyling.getAxisLabel(getRaster(), "X", logScaled);
    }

    private Container getParentComponent() {
        return getParentDialog().getContext().getPane().getControl();
    }

    private boolean getAutoMinMaxEnabled() {
        return xAxisRangeControl.isAutoMinMax();
    }

    @Override
    public String getDataAsText() {
        if (stx == null) {
            return null;
        }

        final int[] binVals = stx.getHistogramBins();
        final int numBins = binVals.length;
        final double min = stx.getMinimum();
        final double max = stx.getMaximum();

        final StringBuilder sb = new StringBuilder(16000);

        sb.append("Product name:\t").append(getRaster().getProduct().getName()).append("\n");
        sb.append("Dataset name:\t").append(getRaster().getName()).append("\n");
        sb.append('\n');
        sb.append("Histogram minimum:\t").append(min).append("\t").append(getRaster().getUnit()).append("\n");
        sb.append("Histogram maximum:\t").append(max).append("\t").append(getRaster().getUnit()).append("\n");
        sb.append("Histogram bin size:\t").append(
                getRaster().isLog10Scaled() ? ("NA\t") : ((max - min) / numBins + "\t") +
                        getRaster().getUnit() + "\n");
        sb.append("Histogram #bins:\t").append(numBins).append("\n");
        sb.append('\n');

        sb.append("Bin center value");
        sb.append('\t');
        sb.append("Bin counts");
        sb.append('\n');

        for (int i = 0; i < numBins; i++) {
            sb.append(min + ((i + 0.5) * (max - min)) / numBins);
            sb.append('\t');
            sb.append(binVals[i]);
            sb.append('\n');
        }

        return sb.toString();
    }

    private void updateLogXAxisCheckBox() {
        final boolean enabled = dataset != null && this.stx.getMinimum() > 0 && !this.stx.isLogHistogram();
        Binding binding = xAxisRangeControl.getBindingContext().getBinding(PROPERTY_NAME_LOG_SCALED);
        if (!enabled) {
            binding.setPropertyValue(enabled);
        }
        log10AxisEnablement.apply();
        binding.adjustComponents();
    }

    private void updateXAxis() {
        final XYPlot plot = chart.getXYPlot();
        plot.getDomainAxis().setLabel(getAxisLabel());
    }

    private class AxisControlChangeListener implements PropertyChangeListener {

        boolean adjusting;

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (!adjusting) {
                adjusting = true;
                if (evt.getPropertyName().equals(PROPERTY_NAME_LOGARITHMIC_HISTOGRAM)) {
                    if (evt.getNewValue().equals(Boolean.TRUE)) {
                        xAxisRangeControl.adjustComponents(Stx.LOG10_SCALING.scale(xAxisRangeControl.getMin()), Stx.LOG10_SCALING.scale(xAxisRangeControl.getMax()), 3);
                    } else {
                        xAxisRangeControl.adjustComponents(Stx.LOG10_SCALING.scaleInverse(xAxisRangeControl.getMin()), Stx.LOG10_SCALING.scaleInverse(xAxisRangeControl.getMax()), 3);
                    }
                }
                adjusting = false;
            }
        }
    }

    private class StxWorker extends ProgressMonitorSwingWorker<Stx, Object> {

        private final Double min;
        private final Double max;
        private final boolean autoMinMaxEnabled;

        public StxWorker(Double min, Double max, boolean autoMinMaxEnabled) {
            super(HistogramPanel.this, "Computing Histogram");
            this.min = min;
            this.max = max;
            this.autoMinMaxEnabled = autoMinMaxEnabled;
        }

        @Override
        protected Stx doInBackground(ProgressMonitor pm) throws Exception {
            final Stx stx;
            if (histogramPlotConfig.useRoiMask || histogramPlotConfig.numBins != Stx.DEFAULT_BIN_COUNT || histogramPlotConfig.histogramLogScaled || min != null || max != null) {
                final StxFactory factory = new StxFactory();
                if (histogramPlotConfig.useRoiMask) {
                    /*if(histogramPlotConfig.roiMask.getValidShape() == null){
                        handleError("The selected mask is empty.\n"+
                            "No valid histogram could be computed.");
                    }*/
                    factory.withRoiMask(histogramPlotConfig.roiMask);
                }
                factory.withHistogramBinCount(histogramPlotConfig.numBins);
                factory.withLogHistogram(histogramPlotConfig.histogramLogScaled);
                if (min != null) {
                    if (histogramPlotConfig.histogramLogScaled) {
                        factory.withMinimum(Stx.LOG10_SCALING.scaleInverse(min));
                    } else {
                        factory.withMinimum(min);
                    }
                }
                if (max != null) {
                    if (histogramPlotConfig.histogramLogScaled) {
                        factory.withMaximum(Stx.LOG10_SCALING.scaleInverse(max));
                    } else {
                        factory.withMaximum(max);
                    }
                }
                stx = factory.create(getRaster(), pm);
            } else {
                stx = getRaster().getStx(true, pm);
            }
            return stx;
        }

        @Override
        public void done() {
            try {
                Stx stx = get();
                if (stx.getSampleCount() > 0) {
                    if (autoMinMaxEnabled) {
                        final double min = stx.getHistogramScaling().scale(stx.getMinimum());
                        final double max = stx.getHistogramScaling().scale(stx.getMaximum());
                        histogramComputing = true;
                        xAxisRangeControl.adjustComponents(min, max, 4);
                        histogramComputing = false;
                    }
                    setStx(stx);
                } else {
                    handleError("Either the selected ROI is empty or no pixels have been found within the minimum and maximum values specified.\n" +
                                        "No valid histogram could be computed.\n");
                    handleStxChange();
                }
            } catch (ExecutionException e) {
                if (histogramPlotConfig.useRoiMask) {
                    handleError("An internal error occurred.\n" +
                                        "No valid histogram could be computed.\n" +
                                        "Possible reason: The selected ROI is empty.");
                } else {
                    handleError("An internal error occurred.\n" +
                                        "No valid histogram could be computed. Reason:\n" + e.getMessage());
                }
                handleStxChange();
            } catch (InterruptedException e) {
                handleError("The histogram computation has been interrupted.");
                handleStxChange();
            }
        }

        private void handleError(String message) {
            JOptionPane.showMessageDialog(getParentComponent(),
                                          message,
                                          CHART_TITLE,
                                          JOptionPane.WARNING_MESSAGE);
        }

    }
}

