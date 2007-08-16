package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.DialogProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.util.Debug;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.util.math.Range;

import javax.media.jai.ROI;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * The scatter plot pane within the statistcs window.
 *
 * @author Marco Peters
 */
class ScatterPlotPane extends PagePane {

    private static final String _DEFAULT_SCATTERPLOT_TEXT = "No scatter plot computed yet.";  /*I18N*/
    private static final String _TITLE_PREFIX = "Scatter Plot";

    private static final int VAR1 = 0;
    private static final int VAR2 = 1;

    private ParamGroup _paramGroup;

    private static Parameter[] _rasterNameParams = new Parameter[2];
    private static Parameter[] _autoMinMaxParams = new Parameter[2];
    private static Parameter[] _minParams = new Parameter[2];
    private static Parameter[] _maxParams = new Parameter[2];

    private ComputePane _computePane;
    private ScatterPlotDisplay _scatterPlotDisplay;
    private boolean _autoMinMaxComputing;

    public ScatterPlotPane(final ToolView parentDialog) {
        super(parentDialog);
    }

    @Override
    public String getTitle() {
        return getTitlePrefix();
    }

    @Override
    protected String getTitlePrefix() {
        return _TITLE_PREFIX;
    }

    @Override
    protected void initContent() {
        initParameters();
        createUI();
    }

    @Override
    protected void updateContent() {
        if (_scatterPlotDisplay != null) {
            final String[] availableBands = createAvailableBandList();
            updateParameters(VAR1, availableBands);
            updateParameters(VAR2, availableBands);
            _scatterPlotDisplay.clearRasters();
        }
    }

    private void updateParameters(final int var, final String[] availableBands) {

        final RasterDataNode raster = getRaster();
        String rasterName = null;
        if (raster != null) {
            rasterName = raster.getName();
        } else if (availableBands.length > 0) {
            rasterName = availableBands[0];
        }
        if (rasterName != null) {
            _rasterNameParams[var].getProperties().setValueSet(availableBands);
            _rasterNameParams[var].setValue(rasterName, null);
        }

        if ((Boolean) _autoMinMaxParams[var].getValue()) {
            _minParams[var].setDefaultValue();
            _maxParams[var].setDefaultValue();
        }
    }

    private void initParameters() {
        _paramGroup = new ParamGroup();

        final String[] availableBands = createAvailableBandList();
        initParameters(VAR1, availableBands);
        initParameters(VAR2, availableBands);

        _paramGroup.addParamChangeListener(new ParamChangeListener() {

            public void parameterValueChanged(final ParamChangeEvent event) {
                updateUIState();
            }
        });
    }

    private void initParameters(final int var, final String[] availableBands) {

        final String paramPrefix = "var" + var + ".";

        final RasterDataNode raster = getRaster();
        final String rasterName;
        if (raster != null) {
            rasterName = raster.getName();
        } else if (availableBands.length > 0) {
            rasterName = availableBands[0];
        } else {
            rasterName = "";
        }
//        final Parameter[] rasterNameParams = getRasterNameParams();
        _rasterNameParams[var] = new Parameter(paramPrefix + "rasterName", rasterName);
        _rasterNameParams[var].getProperties().setValueSet(availableBands);
        _rasterNameParams[var].getProperties().setValueSetBound(true);
        _rasterNameParams[var].getProperties().setDescription("Band name"); /*I18N*/
        _paramGroup.addParameter(_rasterNameParams[var]);

//        final Parameter[] autoMinMaxParams = getAutoMinMaxParams();
        _autoMinMaxParams[var] = new Parameter(paramPrefix + "autoMinMax", Boolean.TRUE);
        _autoMinMaxParams[var].getProperties().setLabel("Auto min/max");
        _autoMinMaxParams[var].getProperties().setDescription("Automatically detect min/max");  /*I18N*/
        _paramGroup.addParameter(_autoMinMaxParams[var]);

//        getMinParams();
        _minParams[var] = new Parameter(paramPrefix + "min", 0.0);
        _minParams[var].getProperties().setLabel("Min:");
        _minParams[var].getProperties().setDescription("Minimum display value");    /*I18N*/
        _minParams[var].getProperties().setNumCols(7);
        _paramGroup.addParameter(_minParams[var]);

//        getMaxParams();
        _maxParams[var] = new Parameter(paramPrefix + "max", 100.0);
        _maxParams[var].getProperties().setLabel("Max:");
        _maxParams[var].getProperties().setDescription("Maximum display value");    /*I18N*/
        _maxParams[var].getProperties().setNumCols(7);
        _paramGroup.addParameter(_maxParams[var]);
    }

    private void createUI() {
        final ActionListener actionAll = new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                computeScatterPlot(false);
            }
        };
        final ActionListener actionROI = new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                computeScatterPlot(true);
            }
        };
        _computePane = ComputePane.createComputePane(actionAll, actionROI, getRaster());

        _scatterPlotDisplay = new ScatterPlotDisplay();
        _scatterPlotDisplay.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        _scatterPlotDisplay.addMouseListener(new PopupHandler());

        this.add(_scatterPlotDisplay, BorderLayout.CENTER);
        this.add(_computePane, BorderLayout.SOUTH);
        this.add(createOptionsPane(), BorderLayout.EAST);

        updateUIState();
    }

    private RasterDataNode getRaster(final int var) {
        final Product product = getProduct();
        if (product == null) {
            return null;
        }
        final String rasterName = _rasterNameParams[var].getValue().toString();
        RasterDataNode raster = product.getRasterDataNode(rasterName);
        if (raster == null) {
            if (getRaster() != null && rasterName.equalsIgnoreCase(getRaster().getName())) {
                raster = getRaster();
            }
        }
        Debug.assertTrue(raster != null);
        return raster;
    }

    /**
     * Called by the AWT.
     *
     * @param x      the new x-coordinate of this component
     * @param y      the new y-coordinate of this component
     * @param width  the new width of this component
     * @param height the new height of this component
     */
    @Override
    public void setBounds(final int x, final int y, final int width, final int height) {
        if (this.getWidth() != width || this.getHeight() != height) {
            // if size changes, give user the chance to recompute at a higher/lower
            // resolution...
            updateComputePane();
        }
        super.setBounds(x, y, width, height);
    }

    private void updateUIState() {
        updateComputePane();
        updateUIState(VAR1);
        updateUIState(VAR2);
    }

    private void updateComputePane() {
        _computePane.setRaster(getRaster(VAR1));
    }

    private void updateUIState(final int var) {
        final double min = ((Number) _minParams[var].getValue()).doubleValue();
        final double max = ((Number) _maxParams[var].getValue()).doubleValue();
        if (!_autoMinMaxComputing && min > max) {
            _minParams[var].setValue(max, null);
            _maxParams[var].setValue(min, null);
        }
        final boolean autoMinMaxEnabled = (Boolean) _autoMinMaxParams[var].getValue();
        _minParams[var].setUIEnabled(!autoMinMaxEnabled);
        _maxParams[var].setUIEnabled(!autoMinMaxEnabled);
    }


    private static JPanel createOptionsPane() {
        final JPanel optionsPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("anchor=NORTHWEST,fill=BOTH");

        GridBagUtils.setAttributes(gbc, "gridy=1,weightx=1");
        GridBagUtils.addToPanel(optionsPane, createOptionsPane(VAR1), gbc, "gridy=0,insets.top=0");
        GridBagUtils.addToPanel(optionsPane, createOptionsPane(VAR2), gbc, "gridy=1,insets.top=7");
        GridBagUtils.addVerticalFiller(optionsPane, gbc);

        return optionsPane;
    }

    private static JPanel createOptionsPane(final int var) {

        final JPanel optionsPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("anchor=WEST,fill=HORIZONTAL");

        GridBagUtils.setAttributes(gbc, "gridwidth=2,gridy=0,insets.top=0");
        GridBagUtils.addToPanel(optionsPane, _rasterNameParams[var].getEditor().getComponent(), gbc,
                                "gridx=0,weightx=1");

        GridBagUtils.setAttributes(gbc, "gridwidth=2,gridy=1,insets.top=4");
        GridBagUtils.addToPanel(optionsPane, _autoMinMaxParams[var].getEditor().getComponent(), gbc,
                                "gridx=0,weightx=1");

        GridBagUtils.setAttributes(gbc, "gridwidth=1,gridy=2,insets.top=2");
        GridBagUtils.addToPanel(optionsPane, _minParams[var].getEditor().getLabelComponent(), gbc,
                                "gridx=0,weightx=1");
        GridBagUtils.addToPanel(optionsPane, _minParams[var].getEditor().getComponent(), gbc, "gridx=1,weightx=0");

        GridBagUtils.setAttributes(gbc, "gridwidth=1,gridy=3,insets.top=2");
        GridBagUtils.addToPanel(optionsPane, _maxParams[var].getEditor().getLabelComponent(), gbc,
                                "gridx=0,weightx=1");
        GridBagUtils.addToPanel(optionsPane, _maxParams[var].getEditor().getComponent(), gbc, "gridx=1,weightx=0");

        optionsPane.setBorder(BorderFactory.createTitledBorder((var == 0 ? "X" : "Y") + "-Band"));

        return optionsPane;
    }

    private void computeScatterPlot(final boolean useROI) {
        try {
            computeScatterPlotImpl(useROI);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                                          "An I/O error occured:\n" + e.getMessage(), /*I18N*/
                                          "Scatter Plot", /*I18N*/
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    private void computeScatterPlotImpl(final boolean useROI) throws IOException {

        final RasterDataNode raster1 = getRaster(VAR1);
        final RasterDataNode raster2 = getRaster(VAR2);

        if (raster1 == null || raster2 == null) {
            return;
        }

        final ROI roi = useROI ? raster1.createROI(ProgressMonitor.NULL) : null;
        final SwingWorker swingWorker = new SwingWorker<IOException, Object>() {
            final ProgressMonitor pm = new DialogProgressMonitor(getParentDialogContentPane(), "Compute Statistic",
                                                                 Dialog.ModalityType.APPLICATION_MODAL);    /*I18N*/

            @Override
            protected IOException doInBackground() throws Exception {
                pm.beginTask("Computing scatter plot...", 2);
                try {
                    computeAutoMinMax(VAR1, raster1, roi, SubProgressMonitor.create(pm, 1));
                    if (pm.isCanceled()) {
                        return null;
                    }
                    computeAutoMinMax(VAR2, raster2, roi, SubProgressMonitor.create(pm, 1));
                    if (pm.isCanceled()) {
                        return null;
                    }
                } catch (IOException e) {
                    return e;
                } finally {
                    pm.done();
                }
                return null;
            }

            @Override
            public void done() {
                if (pm.isCanceled()) {
                    JOptionPane.showMessageDialog(getParentDialogContentPane(),
                                                  "Failed to compute scatter plot.\nThe user has cancelled the calculation.",
                                                  /*I18N*/
                                                  "Statistics", /*I18N*/
                                                  JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                Exception exception;
                try {
                    exception = get();
                } catch (Exception e) {
                    exception = e;
                }
                if (exception instanceof IOException) {
                    JOptionPane.showMessageDialog(getParentDialogContentPane(),
                                                  "Failed to compute scatter plot.\nAn internal error occured:\n" +
                                                  exception.getMessage(),
                                                  "Statistics", /*I18N*/
                                                  JOptionPane.ERROR_MESSAGE);
                    return;
                }
                _scatterPlotDisplay.setRasters(raster1,
                                               ((Number) _minParams[VAR1].getValue()).floatValue(),
                                               ((Number) _maxParams[VAR1].getValue()).floatValue(),
                                               raster2,
                                               ((Number) _minParams[VAR2].getValue()).floatValue(),
                                               ((Number) _maxParams[VAR2].getValue()).floatValue(),
                                               roi);
            }
        };
        swingWorker.execute();
    }

    private void computeAutoMinMax(final int var, final RasterDataNode raster, final ROI roi, ProgressMonitor pm) throws
                                                                                                                  IOException {
        final boolean autoMinMax = (Boolean) _autoMinMaxParams[var].getValue();
        if (autoMinMax) {
            final Range range = raster.computeRasterDataRange(roi, pm);
            final double min = raster.scale(range.getMin());
            final double max = raster.scale(range.getMax());
            final double v = MathUtils.computeRoundFactor(min, max, 4);
            _autoMinMaxComputing = true;
            _minParams[var].setValue(StatisticsUtils.round(min, v), null);
            _maxParams[var].setValue(StatisticsUtils.round(max, v), null);
            _autoMinMaxComputing = false;
        }
    }

    private String[] createAvailableBandList() {
        final List<String> availableBandList = new ArrayList<String>();
        final Product product = getProduct();
        if (product != null) {
            for (int i = 0; i < product.getNumBands(); i++) {
                final Band band = product.getBandAt(i);
                availableBandList.add(band.getName());
            }
            for (int i = 0; i < product.getNumTiePointGrids(); i++) {
                final TiePointGrid grid = product.getTiePointGridAt(i);
                availableBandList.add(grid.getName());
            }
        }
        // if raster is only binded to the product and does not belong to it
        final RasterDataNode raster = getRaster();
        if (raster != null && raster.getProduct() == product) {
            final String rasterName = raster.getName();
            if (!availableBandList.contains(rasterName)) {
                availableBandList.add(rasterName);
            }
        }

        return availableBandList.toArray(new String[availableBandList.size()]);
    }

    @Override
    protected boolean checkDataToClipboardCopy() {
        final int warnLimit = 2000;
        final int excelLimit = 65536;
        final int numNonEmptyBins = _scatterPlotDisplay.getNumNonEmptyBins();
        if (numNonEmptyBins > warnLimit) {
            String excelNote = "";
            if (numNonEmptyBins > excelLimit - 100) {
                excelNote = "Note that e.g., Microsoft® Excel 2002 only supports a total of "
                            + excelLimit + " rows in a sheet.\n";   /*I18N*/
            }
            final int status = JOptionPane.showConfirmDialog(this,
                                                             "This scatter plot diagram contains " + numNonEmptyBins + " non-empty bins.\n" +
                                                             "For each bin, a text data row containing an x, y and z value will be created.\n" +
                                                             excelNote + "\n" +
                                                             "Press 'Yes' if you really want to copy this amount of data to the system clipboard.\n" +
                                                             "", /*I18N*/
                                                                 "Copy Data to Clipboard", /*I18N*/
                                                                 JOptionPane.YES_NO_OPTION,
                                                                 JOptionPane.WARNING_MESSAGE);
            if (status != JOptionPane.YES_OPTION) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected String getDataAsText() {
        return _scatterPlotDisplay.getDataAsText();
    }

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Scatter Plot Display
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static class ScatterPlotDisplay extends JPanel {

        private RasterDataNode[] _rasters;
        private float[] _mins;
        private float[] _maxs;
        private ROI _roi;

        private BufferedImage _offscreenImage;

        public ScatterPlotDisplay() {
            super(null);
            setBackground(Color.white);
            setForeground(Color.black);
        }

        public void clearRasters() {
            _rasters = null;
            _mins = null;
            _maxs = null;
            _offscreenImage = null;
            repaint();
        }

        public BufferedImage getOffscreenImage() {
            return _offscreenImage;
        }

        public void setRasters(final RasterDataNode raster1, final float min1, final float max1,
                               final RasterDataNode raster2, final float min2, final float max2,
                               final ROI roi) {
            _rasters = new RasterDataNode[]{raster1, raster2};
            _mins = new float[]{min1, min2};
            _maxs = new float[]{max1, max2};
            _offscreenImage = null;
            _roi = roi;
            repaint();
        }

        private BufferedImage createOffscreenImage(final int width, final int height) throws IOException {
            return ProductUtils.createScatterPlotImage(_rasters[0],
                                                       _mins[0],
                                                       _maxs[0],
                                                       _rasters[1],
                                                       _mins[1],
                                                       _maxs[1],
                                                       _roi,
                                                       width,
                                                       height,
                                                       StatisticsToolView.DIAGRAM_BG_COLOR,
                                                       _offscreenImage,
                                                       ProgressMonitor.NULL);
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
            final int fh = fm.getHeight();

            if (_rasters == null) {
                g2d.setColor(StatisticsToolView.DIAGRAM_TEXT_COLOR);
                g2d.drawString(_DEFAULT_SCATTERPLOT_TEXT, insets.left + 1, insets.top + fh);
            } else {
                final int diagX0 = StatisticsToolView.DIAGRAM_MIN_INSETS + insets.left + 2 * fh;
                final int diagY0 = StatisticsToolView.DIAGRAM_MIN_INSETS + insets.top;
                final int diagW = getWidth() - 2 * StatisticsToolView.DIAGRAM_MIN_INSETS - (insets.left + insets.right) - 2 * fh - 1;
                final int diagH = getHeight() - 2 * StatisticsToolView.DIAGRAM_MIN_INSETS - (insets.top + insets.bottom) - 2 * fh - 1;
                drawScatterPlot(g2d, diagX0, diagY0, diagW, diagH);
                drawDiagramText(g2d, diagX0, diagY0, diagW, diagH, fm);
            }
        }


        private void drawScatterPlot(final Graphics2D g2d, final int diagX0, final int diagY0, final int diagW,
                                     final int diagH) {

            if (_offscreenImage == null) {
                final Cursor oldCursor = UIUtils.setRootFrameWaitCursor(this);
                try {
                    _offscreenImage = createOffscreenImage(diagW <= 0 ? 1 : diagW, diagH <= 0 ? 1 : diagH);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this,
                                                  "Failed to compute scatter plot.\nAn I/O error occured:" + e.getMessage(),
                                                  "I/O error",
                                                  JOptionPane.ERROR_MESSAGE);   /*I18N*/
                    return;

                } finally {
                    UIUtils.setRootFrameCursor(this, oldCursor);
                }
            }

            g2d.translate(diagX0, diagY0);

            if (_offscreenImage.getWidth() != diagW
                || _offscreenImage.getHeight() != diagH) {
                final AffineTransform t = AffineTransform.getScaleInstance((double) diagW / _offscreenImage.getWidth(),
                                                                           (double) diagH / _offscreenImage.getHeight());
                g2d.drawImage(_offscreenImage, t, this);
            } else {
                g2d.drawImage(_offscreenImage, 0, 0, this);
            }

            g2d.setColor(getForeground());
            g2d.drawRect(-1, -1, diagW + 1, diagH + 1);

            g2d.translate(-diagX0, -diagY0);
        }

        private void drawDiagramText(final Graphics2D g2d, final int diagX0, final int diagY0, final int diagW,
                                     final int diagH, final FontMetrics fm) {

            final RasterDataNode raster1 = _rasters[0];
            final float sampleMin1 = _mins[0];
            final float sampleMax1 = _maxs[0];

            final RasterDataNode raster2 = _rasters[1];
            final float sampleMin2 = _mins[1];
            final float sampleMax2 = _maxs[1];

            final int fontY = fm.getLeading() + fm.getDescent();
            final int fontH = fm.getHeight();

            String text;
            int textW;

            g2d.setColor(StatisticsToolView.DIAGRAM_TEXT_COLOR);

            text = String.valueOf(sampleMin1);
//            text = roundString(sampleMin1);
            g2d.drawString(text, diagX0, diagY0 + diagH + fontH);

            text = String.valueOf(sampleMax1);
//            text = roundString(sampleMax1);
            textW = fm.stringWidth(text);
            g2d.drawString(text, diagX0 + diagW - textW, diagY0 + diagH + fontH);

            text = String.valueOf(0.5F * (sampleMin1 + sampleMax1));
//            text = roundString(0.5F * (sampleMin1 + sampleMax1));
            textW = fm.stringWidth(text);
            g2d.drawString(text, diagX0 + (diagW - textW) / 2, diagY0 + diagH + fontH);

            text = StatisticsUtils.getDiagramLabel(raster1);
            textW = fm.stringWidth(text);
            g2d.drawString(text, diagX0 + (diagW - textW) / 2, diagY0 + diagH + 2 * fontH);

            final int translX = diagX0 - fontY;
            final int translY = diagY0 + diagH;
            final double rotA = -0.5 * Math.PI;

            g2d.translate(translX, translY);
            g2d.rotate(rotA);

            text = String.valueOf(sampleMin2);
//            text = roundString(sampleMin2);
            g2d.drawString(text, 0, 0);

            text = String.valueOf(sampleMax2);
//            text = roundString(sampleMax2);
            textW = fm.stringWidth(text);
            g2d.drawString(text, diagH - textW, -fontY);

            text = String.valueOf(0.5F * (sampleMin2 + sampleMax2));
//            text = roundString(0.5F * (sampleMin2 + sampleMax2));
            textW = fm.stringWidth(text);
            g2d.drawString(text, (diagH - textW) / 2, -fontY);

            text = StatisticsUtils.getDiagramLabel(raster2);
            textW = fm.stringWidth(text);
            g2d.drawString(text, (diagH - textW) / 2, -fontY - fontH);

            g2d.rotate(-rotA);
            g2d.translate(-translX, -translY);
        }

        private byte[] getValidData(final BufferedImage image) {
            if (image != null &&
                image.getColorModel() instanceof IndexColorModel &&
                image.getData().getDataBuffer() instanceof DataBufferByte) {
                return ((DataBufferByte) _offscreenImage.getData().getDataBuffer()).getData();
            }
            return null;
        }

        protected int getNumNonEmptyBins() {
            final byte[] data = getValidData(_offscreenImage);
            int n = 0;
            if (data != null) {
                int b;
                for (byte aData : data) {
                    b = aData & 0xff;
                    if (b != 0) {
                        n++;
                    }
                }
            }
            return n;
        }

        protected String getDataAsText() {
            final byte[] data = getValidData(_offscreenImage);
            if (data == null) {
                return null;
            }

            final StringBuffer sb = new StringBuffer(64000);
            final int w = _offscreenImage.getWidth();
            final int h = _offscreenImage.getHeight();

            final RasterDataNode raster1 = _rasters[0];
            final String name1 = raster1.getName();
            final float sampleMin1 = _mins[0];
            final float sampleMax1 = _maxs[0];

            final RasterDataNode raster2 = _rasters[1];
            final String name2 = raster2.getName();
            final float sampleMin2 = _mins[1];
            final float sampleMax2 = _maxs[1];

            sb.append("Product name:\t").append(raster1.getProduct().getName()).append("\n");
            sb.append("Dataset 1 name:\t").append(name1).append("\n");
            sb.append("Dataset 2 name:\t").append(name2).append("\n");
            sb.append('\n');
            sb.append(name1).append(" minimum:\t").append(sampleMin1).append("\t").append(raster1.getUnit()).append(
                    "\n");
            sb.append(name1).append(" maximum:\t").append(sampleMax1).append("\t").append(raster1.getUnit()).append(
                    "\n");
            sb.append(name1).append(" bin size:\t").append((sampleMax1 - sampleMin1) / w).append("\t").append(
                    raster1.getUnit()).append("\n");
            sb.append(name1).append(" #bins:\t").append(w).append("\n");
            sb.append('\n');
            sb.append(name2).append(" minimum:\t").append(sampleMin2).append("\t").append(raster2.getUnit()).append(
                    "\n");
            sb.append(name2).append(" maximum:\t").append(sampleMax2).append("\t").append(raster2.getUnit()).append(
                    "\n");
            sb.append(name2).append(" bin size:\t").append((sampleMax2 - sampleMin2) / h).append("\t").append(
                    raster2.getUnit()).append("\n");
            sb.append(name2).append(" #bins:\t").append(h).append("\n");
            sb.append('\n');

            sb.append(name1);
            sb.append('\t');
            sb.append(name2);
            sb.append('\t');
            sb.append("Bin counts\t(cropped at 255)");
            sb.append('\n');

            int x, y, z;
            float v1, v2;
            for (int i = 0; i < data.length; i++) {
                z = data[i] & 0xff;
                if (z != 0) {

                    x = i % w;
                    y = h - i / w - 1;

                    v1 = sampleMin1 + ((x + 0.5f) * (sampleMax1 - sampleMin1)) / w;
                    v2 = sampleMin2 + ((y + 0.5f) * (sampleMax2 - sampleMin2)) / h;

                    sb.append(v1);
                    sb.append('\t');
                    sb.append(v2);
                    sb.append('\t');
                    sb.append(z);
                    sb.append('\n');
                }
            }

            return sb.toString();
        }
    }
}

