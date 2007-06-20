package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.DialogProgressMonitor;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.util.math.Histogram;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.util.math.Range;

import javax.media.jai.ROI;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

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
    private HistogramDisplay _histogramDisplay;
    private boolean _histogramComputing;
    private ComputePane _computePane;

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
            _histogramDisplay.setRaster(getRaster());
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
        _histogramDisplay = new HistogramDisplay(getRaster());
        _histogramDisplay.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        _histogramDisplay.addMouseListener(new PopupHandler());
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
            _histogramDisplay.setHistogram(null);
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

        final SwingWorker swingWorker = new SwingWorker() {
            final ProgressMonitor pm = new DialogProgressMonitor(getParentComponent(), "Compute Statistic",
                                                                 Dialog.ModalityType.APPLICATION_MODAL);    /*I18N*/

            @Override
            protected Object doInBackground() throws Exception {
                try {
                    return getRaster().computeRasterDataHistogram(roi, numBins, range, pm);
                } catch (IOException e) {
                    return e;
                }
            }

            @Override
            public void done() {
                if (pm.isCanceled()) {
                    JOptionPane.showMessageDialog(getParentComponent(),
                                                  "Failed to compute histogram.\nThe user has cancelled the calculation.",
                                                  /*I18N*/
                                                  "Statistics", /*I18N*/
                                                  JOptionPane.INFORMATION_MESSAGE);
                    _histogramDisplay.setHistogram(null);
                } else {
                    Histogram histo = null;
                    Object value;
                    try {
                        value = get();
                    } catch (Exception e) {
                        value = e;
                    }
                    if (value instanceof Histogram) {
                        histo = (Histogram) value;
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
                    } else if (value instanceof Exception) {
                        final Exception e = (Exception) value;
                        JOptionPane.showMessageDialog(getParentComponent(),
                                                      "Failed to compute histogram.\nAn internal error occured:\n" + e.getMessage(),
                                                      /*I18N*/
                                                      "Histogram", /*I18N*/
                                                      JOptionPane.ERROR_MESSAGE);
                        _histogramDisplay.setHistogram(null);
                        return;
                    }

                    if (histo != null) {
                        _histogramDisplay.setHistogram(histo);
                    } else {
                        JOptionPane.showMessageDialog(getParentComponent(),
                                                      "The ROI is empty or no pixels found between min/max.\n"
                                                      + "A valid histogram could not be computed.", /*I18N*/
                                                                                                    "Histogram",
                                                                                                    /*I18N*/
                                                                                                    JOptionPane.WARNING_MESSAGE);
                        _histogramDisplay.setHistogram(null);
                    }
                }
            }
        };
        swingWorker.execute();
    }

    private Container getParentComponent() {
        return getParentDialog().getContext().getPane().getControl();
    }

    private boolean getAutoMinMaxEnabled() {
        return (Boolean) _autoMinMaxEnabledParam.getValue();
    }

    @Override
    protected String getDataAsText() {
        return _histogramDisplay.getDataAsText();
    }

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Histogram Display
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static class HistogramDisplay extends JPanel {


        private RasterDataNode _raster;
        private Histogram _histogram;

        public HistogramDisplay(final RasterDataNode raster) {
            super(null);
            _raster = raster;
            setBackground(Color.white);
            setForeground(Color.black);
        }

        public RasterDataNode getRaster() {
            return _raster;
        }

        public void setRaster(final RasterDataNode raster) {
            _raster = raster;
            setHistogram(null);
        }

        public Histogram getHistogram() {
            return _histogram;
        }

        public void setHistogram(final Histogram histogram) {
            _histogram = histogram;
            repaint();
        }

        /**
         * If the UI delegate is non-null, calls its paint method.  We pass the delegate a copy of the Graphics object
         * to protect the rest of the paint code from irrevocable changes (for example, Graphics.translate()).
         *
         * @param g the Graphics object to protect
         *
         * @see #paint
         */
        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);
            draw((Graphics2D) g);
        }


        private void draw(final Graphics2D g2d) {

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            final Insets insets = getInsets();

            g2d.setColor(getBackground());
            g2d.fillRect(insets.left,
                         insets.top,
                         getWidth() - insets.left - insets.right,
                         getHeight() - insets.top - insets.bottom);

            final FontMetrics fm = g2d.getFontMetrics();
            final int fontH = fm.getHeight();

            if (_histogram == null || _raster == null) {
                g2d.setColor(StatisticsToolView.DIAGRAM_TEXT_COLOR);
                g2d.drawString(_DEFAULT_HISTOGRAM_TEXT, insets.left + 1, insets.top + fontH);
                return;
            }

            final int diagX0 = StatisticsToolView.DIAGRAM_MIN_INSETS + insets.left;
            final int diagY0 = StatisticsToolView.DIAGRAM_MIN_INSETS + +insets.top;
            final int diagW = getWidth() - 2 * StatisticsToolView.DIAGRAM_MIN_INSETS - (insets.left + insets.right) - 1;
            final int diagH = getHeight() - 2 * StatisticsToolView.DIAGRAM_MIN_INSETS - (insets.top + insets.bottom) - 2 * fontH - 1;

            drawHistogram(g2d, diagX0, diagY0, diagW, diagH);
            drawDiagramText(g2d, diagX0, diagY0, diagW, diagH, fm);
        }

        private void drawHistogram(final Graphics2D g2d, final int diagX0, final int diagY0, final int diagW,
                                   final int diagH) {
            final int[] binVals = _histogram.getBinCounts();
            final int numBins = _histogram.getNumBins();

            final double xScale = (1.00 * diagW) / (numBins - 1);
            final double yScale = (0.95 * diagH) / _histogram.getMaxBinCount();

            g2d.setColor(StatisticsToolView.DIAGRAM_BG_COLOR);
            g2d.fillRect(diagX0 - 1, diagY0 - 1, diagW + 2, diagH + 2);

            g2d.setColor(StatisticsToolView.DIAGRAM_FG_COLOR);
            int x1, y1, x2 = 0, y2 = 0;
            for (int i = 0; i < numBins; i++) {
                x1 = x2;
                y1 = y2;
                x2 = diagX0 + (int) (xScale * i);
                y2 = diagY0 + diagH - (int) (yScale * binVals[i]);
                if (i > 0) {
                    g2d.drawLine(x1, y1, x2, y2);
                }
            }

            g2d.setColor(getForeground());
            g2d.drawRect(diagX0 - 1, diagY0 - 1, diagW + 2, diagH + 2);
        }

        private void drawDiagramText(final Graphics2D g2d, final int diagX0, final int diagY0, final int diagW,
                                     final int diagH, final FontMetrics fm) {

            final int fontH = fm.getHeight();

            String text;
            int textW;

            final double min = getRaster().scale(_histogram.getMin());
            final double max = getRaster().scale(_histogram.getMax());
            final double rF = MathUtils.computeRoundFactor(min, max, 5);
            g2d.setColor(StatisticsToolView.DIAGRAM_TEXT_COLOR);
            text = String.valueOf(MathUtils.round(min, rF));
            g2d.drawString(text, diagX0, diagY0 + diagH + fontH);
            text = String.valueOf(MathUtils.round(max, rF));
            textW = fm.stringWidth(text);
            g2d.drawString(text, diagX0 + diagW - textW, diagY0 + diagH + fontH);
            final double centerValue = computeCenterValue(min, max);
            text = String.valueOf(MathUtils.round(centerValue, rF));
            textW = fm.stringWidth(text);
            g2d.drawString(text, diagX0 + (diagW - textW) / 2, diagY0 + diagH + fontH);

            text = StatisticsUtils.getDiagramLabel(getRaster());
            textW = fm.stringWidth(text);
            g2d.drawString(text, diagX0 + (diagW - textW) / 2, diagY0 + diagH + 2 * fontH);
        }

        private double computeCenterValue(double min, double max) {
            min = getRaster().scaleInverse(min);
            max = getRaster().scaleInverse(max);
            return getRaster().scale(min + (max - min) * 0.5);
        }

        private String getDataAsText() {
            if (_histogram == null) {
                return null;
            }

            final int[] binVals = _histogram.getBinCounts();
            final int numBins = _histogram.getNumBins();
            final double min = getRaster().scale(_histogram.getMin());
            final double max = getRaster().scale(_histogram.getMax());

            final StringBuffer sb = new StringBuffer(16000);

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
}


