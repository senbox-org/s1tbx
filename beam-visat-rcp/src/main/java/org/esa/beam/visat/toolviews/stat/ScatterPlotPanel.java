package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.param.editors.ComboBoxEditor;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.util.Debug;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.Range;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.ui.RectangleInsets;

import javax.media.jai.ROI;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;


/**
 * The scatter plot pane within the statistcs window.
 *
 * @author Marco Peters
 */
class ScatterPlotPanel extends PagePanel {

    private static final String NO_DATA_MESSAGE = "No scatter plot computed yet.";  /*I18N*/
    private static final String TITLE_PREFIX = "Scatter Plot";

    private static final int X_VAR = 0;
    private static final int Y_VAR = 1;

    private ParamGroup paramGroup;

    private static Parameter[] rasterNameParams = new Parameter[2];
    private static Parameter[] autoMinMaxParams = new Parameter[2];
    private static Parameter[] minParams = new Parameter[2];
    private static Parameter[] maxParams = new Parameter[2];

    private ComputePanel computePanel;
    private ChartPanel scatterPlotDisplay;
    private boolean adjustingAutoMinMax;
    private XYImagePlot plot;

    public ScatterPlotPanel(final ToolView parentDialog) {
        super(parentDialog);
    }

    @Override
    public String getTitle() {
        return getTitlePrefix();
    }

    @Override
    protected String getTitlePrefix() {
        return TITLE_PREFIX;
    }

    @Override
    protected void initContent() {
        initParameters();
        createUI();
    }

    @Override
    protected void updateContent() {
        if (scatterPlotDisplay != null) {
            final String[] availableBands = createAvailableBandList();
            updateParameters(X_VAR, availableBands);
            updateParameters(Y_VAR, availableBands);
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
            rasterNameParams[var].getProperties().setValueSet(availableBands);
            rasterNameParams[var].setValue(rasterName, null);
        } else {
            rasterNameParams[var].getProperties().setValueSet(new String[0]);
        }

        if ((Boolean) autoMinMaxParams[var].getValue()) {
            minParams[var].setDefaultValue();
            maxParams[var].setDefaultValue();
        }
    }

    private void initParameters() {
        paramGroup = new ParamGroup();

        final String[] availableBands = createAvailableBandList();
        initParameters(X_VAR, availableBands);
        initParameters(Y_VAR, availableBands);

        paramGroup.addParamChangeListener(new ParamChangeListener() {

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
        rasterNameParams[var] = new Parameter(paramPrefix + "rasterName", rasterName);
        rasterNameParams[var].getProperties().setValueSet(availableBands);
        rasterNameParams[var].getProperties().setValueSetBound(true);
        rasterNameParams[var].getProperties().setDescription("Band name"); /*I18N*/
        rasterNameParams[var].getProperties().setEditorClass(ComboBoxEditor.class);
        paramGroup.addParameter(rasterNameParams[var]);

//        final Parameter[] autoMinMaxParams = getAutoMinMaxParams();
        autoMinMaxParams[var] = new Parameter(paramPrefix + "autoMinMax", Boolean.TRUE);
        autoMinMaxParams[var].getProperties().setLabel("Auto min/max");
        autoMinMaxParams[var].getProperties().setDescription("Automatically detect min/max");  /*I18N*/
        paramGroup.addParameter(autoMinMaxParams[var]);

//        getMinParams();
        minParams[var] = new Parameter(paramPrefix + "min", 0.0);
        minParams[var].getProperties().setLabel("Min:");
        minParams[var].getProperties().setDescription("Minimum display value");    /*I18N*/
        minParams[var].getProperties().setNumCols(7);
        paramGroup.addParameter(minParams[var]);

//        getMaxParams();
        maxParams[var] = new Parameter(paramPrefix + "max", 100.0);
        maxParams[var].getProperties().setLabel("Max:");
        maxParams[var].getProperties().setDescription("Maximum display value");    /*I18N*/
        maxParams[var].getProperties().setNumCols(7);
        paramGroup.addParameter(maxParams[var]);
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
        computePanel = ComputePanel.createComputePane(actionAll, actionROI, getRaster());

        plot = new XYImagePlot();

        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
        plot.setNoDataMessage(NO_DATA_MESSAGE);

//        NumberAxis scaleAxis = new NumberAxis("Scale");
//        scaleAxis.setTickLabelFont(new Font("Dialog", Font.PLAIN, 7));
//        PaintScaleLegend legend = new PaintScaleLegend(new GrayPaintScale(), scaleAxis);
//        legend.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
//        legend.setAxisOffset(5.0);
//        legend.setMargin(new RectangleInsets(5, 5, 5, 5));
//        legend.setPadding(new RectangleInsets(10, 10, 10, 10));
//        legend.setStripWidth(10);
//        legend.setPosition(RectangleEdge.RIGHT);

        JFreeChart chart = new JFreeChart("Scatter Plot", plot);
        chart.removeLegend();
//         chart.addSubtitle(legend);

        scatterPlotDisplay = new ChartPanel(chart);
        scatterPlotDisplay.getPopupMenu().add(createCopyDataToClipboardMenuItem());

        final JPanel controlPanel = new JPanel(new BorderLayout(2, 2));
        controlPanel.add(createOptionsPane(), BorderLayout.NORTH);
        controlPanel.add(computePanel, BorderLayout.SOUTH);

        this.add(scatterPlotDisplay, BorderLayout.CENTER);
        this.add(controlPanel, BorderLayout.EAST);

        updateUIState();
    }

    private RasterDataNode getRaster(final int var) {
        final Product product = getProduct();
        if (product == null) {
            return null;
        }
        final String rasterName = rasterNameParams[var].getValue().toString();
        RasterDataNode raster = product.getRasterDataNode(rasterName);
        if (raster == null) {
            // todo - "if the product doesn't have the raster, take some other?" - this is stupid (nf - 18.12.2007)
            if (getRaster() != null && rasterName.equalsIgnoreCase(getRaster().getName())) {
                raster = getRaster();
            }
        }
        Debug.assertTrue(raster != null);
        return raster;
    }

    private void updateUIState() {
        updateComputePane();
        updateUIState(X_VAR);
        updateUIState(Y_VAR);
    }

    private void updateComputePane() {
        computePanel.setRaster(getRaster(X_VAR));
    }

    private void updateUIState(final int var) {
        final double min = ((Number) minParams[var].getValue()).doubleValue();
        final double max = ((Number) maxParams[var].getValue()).doubleValue();
        if (!adjustingAutoMinMax && min > max) {
            minParams[var].setValue(max, null);
            maxParams[var].setValue(min, null);
        }
        final boolean autoMinMaxEnabled = (Boolean) autoMinMaxParams[var].getValue();
        minParams[var].setUIEnabled(!autoMinMaxEnabled);
        maxParams[var].setUIEnabled(!autoMinMaxEnabled);
    }


    private JPanel createOptionsPane() {
        final JPanel optionsPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("anchor=NORTHWEST,fill=BOTH");

        GridBagUtils.setAttributes(gbc, "gridy=1,weightx=1");
        GridBagUtils.addToPanel(optionsPane, createOptionsPane(X_VAR), gbc, "gridy=0,insets.top=0");
        GridBagUtils.addToPanel(optionsPane, createOptionsPane(Y_VAR), gbc, "gridy=1,insets.top=7");
        GridBagUtils.addVerticalFiller(optionsPane, gbc);

        return optionsPane;
    }

    private JPanel createOptionsPane(final int var) {

        final JPanel optionsPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("anchor=WEST,fill=HORIZONTAL");

        GridBagUtils.setAttributes(gbc, "gridwidth=2,gridy=0,insets.top=0");
        GridBagUtils.addToPanel(optionsPane, rasterNameParams[var].getEditor().getComponent(), gbc,
                                "gridx=0,weightx=1");

        GridBagUtils.setAttributes(gbc, "gridwidth=2,gridy=1,insets.top=4");
        GridBagUtils.addToPanel(optionsPane, autoMinMaxParams[var].getEditor().getComponent(), gbc,
                                "gridx=0,weightx=1");

        GridBagUtils.setAttributes(gbc, "gridwidth=1,gridy=2,insets.top=2");
        GridBagUtils.addToPanel(optionsPane, minParams[var].getEditor().getLabelComponent(), gbc,
                                "gridx=0,weightx=0");
        GridBagUtils.addToPanel(optionsPane, minParams[var].getEditor().getComponent(), gbc, "gridx=1,weightx=1");

        GridBagUtils.setAttributes(gbc, "gridwidth=1,gridy=3,insets.top=2");
        GridBagUtils.addToPanel(optionsPane, maxParams[var].getEditor().getLabelComponent(), gbc,
                                "gridx=0,weightx=0");
        GridBagUtils.addToPanel(optionsPane, maxParams[var].getEditor().getComponent(), gbc, "gridx=1,weightx=1");

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

    private static class ScatterPlot {
        private final BufferedImage image;
        private final Range rangeX;
        private final Range rangeY;

        private ScatterPlot(BufferedImage image, Range rangeX, Range rangeY) {
            this.image = image;
            this.rangeX = rangeX;
            this.rangeY = rangeY;
        }

        public BufferedImage getImage() {
            return image;
        }

        public Range getRangeX() {
            return rangeX;
        }

        public Range getRangeY() {
            return rangeY;
        }
    }

    private void computeScatterPlotImpl(final boolean useROI) throws IOException {

        final RasterDataNode rasterX = getRaster(X_VAR);
        final RasterDataNode rasterY = getRaster(Y_VAR);

        if (rasterX == null || rasterY == null) {
            return;
        }

        final ROI roi = useROI ? rasterX.createROI(ProgressMonitor.NULL) : null;
        final SwingWorker<ScatterPlot, Object> swingWorker = new ProgressMonitorSwingWorker<ScatterPlot, Object>(getParentDialogContentPane(), "Compute Statistic") {

            @Override
            protected ScatterPlot doInBackground(ProgressMonitor pm) throws Exception {
                pm.beginTask("Computing scatter plot...", 100);
                try {
                    final Range rangeX = getRange(X_VAR, rasterX, roi, SubProgressMonitor.create(pm, 15));
                    final Range rangeY = getRange(Y_VAR, rasterY, roi, SubProgressMonitor.create(pm, 15));
                    final BufferedImage image = ProductUtils.createScatterPlotImage(rasterX,
                                                                                    (float) rangeX.getMin(),
                                                                                    (float) rangeX.getMax(),
                                                                                    rasterY,
                                                                                    (float) rangeY.getMin(),
                                                                                    (float) rangeY.getMax(),
                                                                                    roi,
                                                                                    512,
                                                                                    512,
                                                                                    new Color(255, 255, 255, 0),
                                                                                    null,
                                                                                    SubProgressMonitor.create(pm, 70));
                    return new ScatterPlot(image, rangeX, rangeY);
                } finally {
                    pm.done();
                }
            }

            @Override
            public void done() {
                try {
                    final ScatterPlot scatterPlot = get();
                    final double minX = scatterPlot.getRangeX().getMin();
                    final double maxX = scatterPlot.getRangeX().getMax();
                    final double minY = scatterPlot.getRangeY().getMin();
                    final double maxY = scatterPlot.getRangeY().getMax();
                    plot.setImage(scatterPlot.getImage());
                    plot.setImageDataBounds(new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY));
                    setAutoRange(X_VAR, scatterPlot.getRangeX());
                    setAutoRange(Y_VAR, scatterPlot.getRangeY());
                    plot.getDomainAxis().setLabel(getAxisLabel(getRaster(X_VAR)));
                    plot.getRangeAxis().setLabel(getAxisLabel(getRaster(Y_VAR)));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(getParentDialogContentPane(),
                                                  "Failed to compute scatter plot.\n" +
                                                          "Calculation canceled.",
                                                  /*I18N*/
                                                  "Scatter Plot", /*I18N*/
                                                  JOptionPane.ERROR_MESSAGE);
                } catch (CancellationException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(getParentDialogContentPane(),
                                                  "Failed to compute scatter plot.\n" +
                                                          "Calculation canceled.",
                                                  /*I18N*/
                                                  "Scatter Plot", /*I18N*/
                                                  JOptionPane.ERROR_MESSAGE);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(getParentDialogContentPane(),
                                                  "Failed to compute scatter plot.\n" +
                                                          "An error occured:\n" +
                                                          e.getCause().getMessage(),
                                                  "Scatter Plot", /*I18N*/
                                                  JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        swingWorker.execute();
    }

    private String getAxisLabel(RasterDataNode raster) {
        final String unit = raster.getUnit();
        if (unit != null) {
            return raster.getName() + " [" + unit + "]";
        }
        return raster.getName();
    }

    private Range getRange(final int var, final RasterDataNode raster, final ROI roi, ProgressMonitor pm) throws
            IOException {
        final boolean autoMinMax = (Boolean) autoMinMaxParams[var].getValue();
        if (autoMinMax) {
            final Range range = raster.computeRasterDataRange(roi, pm);
            final double min = raster.scale(range.getMin());
            final double max = raster.scale(range.getMax());
            return new Range(min, max);
        } else {
            return new Range((Double) minParams[var].getValue(), (Double) maxParams[var].getValue());
        }
    }

    private void setAutoRange(final int var, Range range) {
        final boolean autoMinMax = (Boolean) autoMinMaxParams[var].getValue();
        if (autoMinMax) {
            adjustingAutoMinMax = true;
            minParams[var].setValue(range.getMin(), null);
            maxParams[var].setValue(range.getMax(), null);
            adjustingAutoMinMax = false;
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
        final int numNonEmptyBins = getNumNonEmptyBins();
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

    private byte[] getValidData(final BufferedImage image) {
        if (image != null &&
                image.getColorModel() instanceof IndexColorModel &&
                image.getData().getDataBuffer() instanceof DataBufferByte) {
            return ((DataBufferByte) image.getData().getDataBuffer()).getData();
        }
        return null;
    }

    protected int getNumNonEmptyBins() {
        final byte[] data = getValidData(plot.getImage());
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

    @Override
    protected String getDataAsText() {
        final BufferedImage image = plot.getImage();
        final Rectangle2D bounds = plot.getImageDataBounds();

        final byte[] data = getValidData(image);
        if (data == null) {
            return null;
        }

        final StringBuffer sb = new StringBuffer(64000);
        final int w = image.getWidth();
        final int h = image.getHeight();

        final RasterDataNode rasterX = getRaster(X_VAR);
        final String nameX = rasterX.getName();
        final double sampleMinX = bounds.getMinX();
        final double sampleMaxX = bounds.getMaxX();

        final RasterDataNode rasterY = getRaster(Y_VAR);
        final String nameY = rasterY.getName();
        final double sampleMinY = bounds.getMinY();
        final double sampleMaxY = bounds.getMaxY();

        sb.append("Product name:\t").append(rasterX.getProduct().getName()).append("\n");
        sb.append("Dataset X name:\t").append(nameX).append("\n");
        sb.append("Dataset Y name:\t").append(nameY).append("\n");
        sb.append('\n');
        sb.append(nameX).append(" minimum:\t").append(sampleMinX).append("\t").append(rasterX.getUnit()).append(
                "\n");
        sb.append(nameX).append(" maximum:\t").append(sampleMaxX).append("\t").append(rasterX.getUnit()).append(
                "\n");
        sb.append(nameX).append(" bin size:\t").append((sampleMaxX - sampleMinX) / w).append("\t").append(
                rasterX.getUnit()).append("\n");
        sb.append(nameX).append(" #bins:\t").append(w).append("\n");
        sb.append('\n');
        sb.append(nameY).append(" minimum:\t").append(sampleMinY).append("\t").append(rasterY.getUnit()).append(
                "\n");
        sb.append(nameY).append(" maximum:\t").append(sampleMaxY).append("\t").append(rasterY.getUnit()).append(
                "\n");
        sb.append(nameY).append(" bin size:\t").append((sampleMaxY - sampleMinY) / h).append("\t").append(
                rasterY.getUnit()).append("\n");
        sb.append(nameY).append(" #bins:\t").append(h).append("\n");
        sb.append('\n');

        sb.append(nameX);
        sb.append('\t');
        sb.append(nameY);
        sb.append('\t');
        sb.append("Bin counts\t(cropped at 255)");
        sb.append('\n');

        int x, y, z;
        double v1, v2;
        for (int i = 0; i < data.length; i++) {
            z = data[i] & 0xff;
            if (z != 0) {

                x = i % w;
                y = h - i / w - 1;

                v1 = sampleMinX + ((x + 0.5) * (sampleMaxX - sampleMinX)) / w;
                v2 = sampleMinY + ((y + 0.5) * (sampleMaxY - sampleMinY)) / h;

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

