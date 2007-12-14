package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.util.math.Histogram;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.util.math.Range;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import javax.media.jai.ROI;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Random;

/**
 * A pane within the statistcs window which displays a histogram.
 *
 * @author Marco Peters
 */
class HistogramPane extends PagePane {

    private final static String _DEFAULT_HISTOGRAM_TEXT = "No histogram computed yet.";  /*I18N*/
    private final static String _TITLE_PREFIX = "Histogram";    /*I18N*/


    private Parameter _numBinsParam;
    private Parameter _autoMinMaxEnabledParam;
    private Parameter _histoMinParam;
    private Parameter _histoMaxParam;
    private ChartPanel _histogramDisplay;
    private boolean _histogramComputing;
    private ComputePane _computePane;
    private XYSeriesCollection dataset;
    private JFreeChart chart;
    private Histogram _histogram;


    public HistogramPane(final ToolView parentDialog) {
        super(parentDialog);
    }

    @Override
    protected String getTitlePrefix() {
        return _TITLE_PREFIX;
    }

    @Override
    protected void initContent() {
        initParameters();
        createUI();
        updateContent();
    }

    @Override
    protected void updateContent() {
        if (_computePane != null) {
            _computePane.setRaster(getRaster());
            setRaster(getRaster());
            if (!(Boolean) _autoMinMaxEnabledParam.getValue()) {
                return;
            }
            _histoMinParam.setDefaultValue();
            _histoMaxParam.setDefaultValue();
        }
    }

    @Override
    protected boolean mustUpdateContent() {
        return isRasterChanged();
    }

    private void initParameters() {
        ParamGroup paramGroup = new ParamGroup();

        _numBinsParam = new Parameter("histo.numBins", 500);
        _numBinsParam.getProperties().setLabel("#Bins:");   /*I18N*/
        _numBinsParam.getProperties().setDescription("Set the number of bins in the histogram");    /*I18N*/
        _numBinsParam.getProperties().setMinValue(2);
        _numBinsParam.getProperties().setMaxValue(2000);
        _numBinsParam.getProperties().setNumCols(5);
        paramGroup.addParameter(_numBinsParam);

        _autoMinMaxEnabledParam = new Parameter("histo.autoMinMax", Boolean.TRUE);
        _autoMinMaxEnabledParam.getProperties().setLabel("Auto min/max");   /*I18N*/
        _autoMinMaxEnabledParam.getProperties().setDescription("Automatically detect min/max"); /*I18N*/
        paramGroup.addParameter(_autoMinMaxEnabledParam);

        _histoMinParam = new Parameter("histo.min", 0.0);
        _histoMinParam.getProperties().setLabel("Min:");    /*I18N*/
        _histoMinParam.getProperties().setDescription("Histogram minimum sample value");    /*I18N*/
        _histoMinParam.getProperties().setNumCols(7);
        paramGroup.addParameter(_histoMinParam);

        _histoMaxParam = new Parameter("histo.max", 100.0);
        _histoMaxParam.getProperties().setLabel("Max:");    /*I18N*/
        _histoMaxParam.getProperties().setDescription("Histogram maximum sample value");    /*I18N*/
        _histoMaxParam.getProperties().setNumCols(7);
        paramGroup.addParameter(_histoMaxParam);

        paramGroup.addParamChangeListener(new ParamChangeListener() {

            public void parameterValueChanged(final ParamChangeEvent event) {
                updateUIState();
            }
        });
    }

    private void createUI() {
        dataset = new XYSeriesCollection();
        chart = ChartFactory.createHistogram(
                "Histogram",
                null,
                null,
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setForegroundAlpha(0.85f);
        XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false);

        final ActionListener actionAll = new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                computeHistogram(false);
            }
        };
        final ActionListener actionROI = new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                computeHistogram(true);
            }
        };
        _computePane = ComputePane.createComputePane(actionAll, actionROI, getRaster());
        _histogramDisplay = new ChartPanel(chart);
        // _histogramDisplay.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        // _histogramDisplay.addMouseListener(new PopupHandler());
        final JMenuItem menuItem = new JMenuItem("Copy Data to Clipboard"); /*I18N*/
        menuItem.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent actionEvent) {
                if (checkDataToClipboardCopy()) {
                    copyToClipboardImpl();
                }
            }
        });
        _histogramDisplay.getPopupMenu().add(menuItem);
        this.add(_histogramDisplay, BorderLayout.CENTER);
        this.add(_computePane, BorderLayout.SOUTH);
        this.add(createOptionsPane(), BorderLayout.EAST);
        updateUIState();
    }

    private void updateUIState() {
        final double min = ((Number) _histoMinParam.getValue()).doubleValue();
        final double max = ((Number) _histoMaxParam.getValue()).doubleValue();
        if (!_histogramComputing && min > max) {
            _histoMinParam.setValue(max, null);
            _histoMaxParam.setValue(min, null);
        }
        final boolean autoMinMaxEnabled = (Boolean) _autoMinMaxEnabledParam.getValue();
        _histoMinParam.setUIEnabled(!autoMinMaxEnabled);
        _histoMaxParam.setUIEnabled(!autoMinMaxEnabled);
    }

    private JPanel createOptionsPane() {
        final JPanel optionsPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("anchor=WEST,fill=BOTH");

        GridBagUtils.setAttributes(gbc, "gridwidth=1");

        GridBagUtils.setAttributes(gbc, "gridwidth=1,gridy=0,insets.top=2");
        GridBagUtils.addToPanel(optionsPane, _numBinsParam.getEditor().getLabelComponent(), gbc,
                                "gridx=0,weightx=1");
        GridBagUtils.addToPanel(optionsPane, _numBinsParam.getEditor().getComponent(), gbc, "gridx=1,weightx=0");

        GridBagUtils.setAttributes(gbc, "gridwidth=2,gridy=1,insets.top=7");
        GridBagUtils.addToPanel(optionsPane, _autoMinMaxEnabledParam.getEditor().getComponent(), gbc,
                                "gridwidth=2,gridx=0,weightx=1");

        GridBagUtils.setAttributes(gbc, "gridwidth=1,gridy=2,insets.top=2");
        GridBagUtils.addToPanel(optionsPane, _histoMinParam.getEditor().getLabelComponent(), gbc,
                                "gridx=0,weightx=1");
        GridBagUtils.addToPanel(optionsPane, _histoMinParam.getEditor().getComponent(), gbc, "gridx=1,weightx=0");

        GridBagUtils.setAttributes(gbc, "gridwidth=1,gridy=3,insets.top=2");
        GridBagUtils.addToPanel(optionsPane, _histoMaxParam.getEditor().getLabelComponent(), gbc,
                                "gridx=0,weightx=1");
        GridBagUtils.addToPanel(optionsPane, _histoMaxParam.getEditor().getComponent(), gbc, "gridx=1,weightx=0");

        // Dummy
        GridBagUtils.setAttributes(gbc, "gridwidth=1,gridy=4,insets.top=2");
        GridBagUtils.addToPanel(optionsPane, new JLabel(" "), gbc, "gridx=0,weightx=1,weighty=1");

        optionsPane.setBorder(BorderFactory.createTitledBorder("X"));

        return optionsPane;
    }

    private void computeHistogram(final boolean useROI) {
        final ROI roi;
        try {
            roi = useROI ? getRaster().createROI(ProgressMonitor.NULL) : null;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(getParentComponent(),
                                          "Failed to compute histogram.\nAn I/O error occured:\n" + e.getMessage(),
                                          /*I18N*/
                                          "Histogram", /*I18N*/
                                          JOptionPane.ERROR_MESSAGE);
            setHistogram(null);
            return;
        }
        final int numBins = ((Number) _numBinsParam.getValue()).intValue();
        final boolean autoMinMaxEnabled = getAutoMinMaxEnabled();
        final Range range;
        if (autoMinMaxEnabled) {
            range = null; // auto compute range
        } else {
            final double min = ((Number) _histoMinParam.getValue()).doubleValue();
            final double max = ((Number) _histoMaxParam.getValue()).doubleValue();
            range = new Range(getRaster().scaleInverse(min), getRaster().scaleInverse(max));
        }

        final SwingWorker<Histogram, Object> swingWorker = new ProgressMonitorSwingWorker<Histogram, Object>(this._histogramDisplay, "Computing histogram") {
            @Override
            protected Histogram doInBackground(ProgressMonitor pm) throws Exception {
                return getRaster().computeRasterDataHistogram(roi, numBins, range, pm);
            }

            @Override
            public void done() {
                Histogram histo = null;
                if (isCancelled()) {
                    JOptionPane.showMessageDialog(getParentComponent(),
                                                  "Failed to compute histogram.\nThe user has cancelled the calculation.",
                                                  /*I18N*/
                                                  "Statistics", /*I18N*/
                                                  JOptionPane.INFORMATION_MESSAGE);
                } else {
                    try {
                        histo = get();
                        if (histo != null) {
                            if (histo.getMaxBinCount() > 0) {
                                if (autoMinMaxEnabled) {
                                    final double min = getRaster().scale(histo.getMin());
                                    final double max = getRaster().scale(histo.getMax());
                                    final double v = MathUtils.computeRoundFactor(min, max, 4);
                                    _histogramComputing = true;
                                    _histoMinParam.setValue(StatisticsUtils.round(min, v), null);
                                    _histoMaxParam.setValue(StatisticsUtils.round(max, v), null);
                                    _histogramComputing = false;
                                }
                            }
                        } else {
                            JOptionPane.showMessageDialog(getParentComponent(),
                                                          "The ROI is empty or no pixels found between min/max.\n"
                                                                  + "A valid histogram could not be computed.",
                                                          "Histogram",
                                                          JOptionPane.WARNING_MESSAGE);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(getParentComponent(),
                                                      "Failed to compute histogram.\nAn internal error occured:\n" + e.getMessage(),
                                                      "Histogram",
                                                      JOptionPane.ERROR_MESSAGE);
                    }
                }
                setHistogram(histo);
            }
        };
        swingWorker.execute();
    }

    private void setHistogram(Histogram histogram) {
        _histogram = histogram;
        dataset.removeAllSeries();
        if (_histogram != null) {
            final int[] binCounts = _histogram.getBinCounts();
            final XYSeries series = new XYSeries(getRaster().getName());
            for (int i = 0; i < binCounts.length; i++) {
                series.add(i, binCounts[i]);
            }
            dataset.addSeries(series);
        }
    }

    private Container getParentComponent() {
        return getParentDialog().getContext().getPane().getControl();
    }

    private boolean getAutoMinMaxEnabled() {
        return (Boolean) _autoMinMaxEnabledParam.getValue();
    }


    public String getDataAsText() {
        if (_histogram == null) {
            return null;
        }

        final int[] binVals = _histogram.getBinCounts();
        final int numBins = _histogram.getNumBins();
        final double min = getRaster().scale(_histogram.getMin());
        final double max = getRaster().scale(_histogram.getMax());

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

}


/**
 * A demo of the {@link HistogramDataset} class.
 */
class HistogramDemo1 extends ApplicationFrame {

    /**
     * Creates a new demo.
     *
     * @param title the frame title.
     */
    public HistogramDemo1(String title) {
        super(title);
        JPanel chartPanel = createDemoPanel();
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
        setContentPane(chartPanel);
    }

    /**
     * Creates a sample {@link HistogramDataset}.
     *
     * @return the dataset.
     */
    private static IntervalXYDataset createDataset() {
        HistogramDataset dataset = new HistogramDataset();
        double[] values = new double[1000];
        Random generator = new Random(12345678L);
        for (int i = 0; i < 1000; i++) {
            values[i] = generator.nextGaussian() + 5;
        }
        dataset.addSeries("H1", values, 100, 2.0, 8.0);
        values = new double[1000];
        for (int i = 0; i < 1000; i++) {
            values[i] = generator.nextGaussian() + 7;
        }
        dataset.addSeries("H2", values, 100, 4.0, 10.0);
        return dataset;
    }

    /**
     * Creates a chart.
     *
     * @param dataset a dataset.
     * @return The chart.
     */
    private static JFreeChart createChart(IntervalXYDataset dataset) {
        JFreeChart chart = ChartFactory.createHistogram(
                "Histogram Demo 1",
                null,
                null,
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setForegroundAlpha(0.85f);
        XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false);
        return chart;
    }

    /**
     * Creates a panel for the demo (used by SuperDemo.java).
     *
     * @return A panel.
     */
    public static JPanel createDemoPanel() {
        JFreeChart chart = createChart(createDataset());
        return new ChartPanel(chart);
    }

    /**
     * The starting point for the demo.
     *
     * @param args ignored.
     * @throws IOException if there is a problem saving the file.
     */
    public static void main(String[] args) throws IOException {

        HistogramDemo1 demo = new HistogramDemo1("JFreeChart : HistogramDemo1");
        demo.pack();
        RefineryUtilities.centerFrameOnScreen(demo);
        demo.setVisible(true);

    }

}
