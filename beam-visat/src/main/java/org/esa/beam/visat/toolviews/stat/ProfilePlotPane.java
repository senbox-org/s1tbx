package org.esa.beam.visat.toolviews.stat;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TransectProfileData;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.param.validators.NumberValidator;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.layer.FigureLayer;
import org.esa.beam.util.math.MathUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;

/**
 * The profile plot pane within the statistcs window.
 *
 * @author Marco Peters
 */
class ProfilePlotPane extends PagePane {

    private static final String TITLE_PREFIX = "Profile Plot"; /*I18N*/
    private static final String DEFAULT_PROFILEPLOT_TEXT = "No profile plot computed yet. " +
            "It will be computed if a shape is added to the image view.";    /*I18N*/

    private static final int VAR1 = 0;
    private static final int VAR2 = 1;

    private final static LayerObserver figureLayerObserver = LayerObserver.getInstance(FigureLayer.class);
    private final static Parameter[] autoMinMaxParams = new Parameter[2];
    private final static Parameter[] minParams = new Parameter[2];
    private final static Parameter[] maxParams = new Parameter[2];
    private static Parameter markVerticesParam = new Parameter("markVertices");
    private static boolean isInitialized = false;

    private ParamGroup paramGroup;
    private ChartPanel profilePlotDisplay;
    private JFreeChart chart;
    private XYSeriesCollection dataset;
    private TransectProfileData profileData;

    public ProfilePlotPane(final ToolView parentDialog) {
        super(parentDialog);
        figureLayerObserver.addLayerObserverListener(new LayerObserver.LayerObserverListener() {
            public void layerChanged() {
                updateContent();
            }
        });
        figureLayerObserver.setRaster(getRaster());
    }

    @Override
    protected String getTitlePrefix() {
        return TITLE_PREFIX;
    }

    @Override
    protected void setRaster(final RasterDataNode raster) {
        final RasterDataNode oldRaster = super.getRaster();
        if (oldRaster != raster) {
            figureLayerObserver.setRaster(raster);
        }
        super.setRaster(raster);
    }

    @Override
    protected void initContent() {
        initParameters();
        createUI();
        isInitialized = true;
        updateContent();
    }

    @Override
    protected void updateContent() {
        if (!isInitialized) {
            return;
        }
        try {
            profileData = StatisticsUtils.TransectProfile.getTransectProfileData(getRaster());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(getParent(),
                                          "Failed to compute profile plot.\n" +
                                                  "An I/O error occured:" + e.getMessage(),
                                          "I/O error",
                                          JOptionPane.ERROR_MESSAGE);   /*I18N*/
            return;
        }
        dataset.removeAllSeries();
        if (profileData != null) {
            XYSeries series = new XYSeries("Sample Values");
            final float[] sampleValues = profileData.getSampleValues();
            for (int i = 0; i < sampleValues.length; i++) {
                series.add(i, sampleValues[i]);
            }
            dataset.addSeries(series);

            final Number minX = 0;
            final Number maxX = profileData.getNumPixels() - 1;
            final Number minY = StatisticsUtils.round(profileData.getSampleMin());
            final Number maxY = StatisticsUtils.round(profileData.getSampleMax());

            minParams[VAR1].getProperties().setMinValue(minX);
            minParams[VAR1].getProperties().setMaxValue(maxX);
            minParams[VAR1].setValue(minX, null);
            maxParams[VAR1].getProperties().setMinValue(minX);
            maxParams[VAR1].getProperties().setMaxValue(maxX);
            maxParams[VAR1].setValue(maxX, null);

            minParams[VAR2].setValue(minY, null);
            //            minParams[VAR2].getProperties().setMinValue(minY);
            //            minParams[VAR2].getProperties().setMaxValue(maxY);
            maxParams[VAR2].setValue(maxY, null);
            //            maxParams[VAR2].getProperties().setMinValue(minY);
            //            maxParams[VAR2].getProperties().setMaxValue(maxY);

            markVerticesParam.setUIEnabled(profileData.getShapeVertices().length > 2);
        }

        updateUIState();

        setDiagramProperties();
    }


    private void initParameters() {
        paramGroup = new ParamGroup();
        initParameters(VAR1);
        initParameters(VAR2);
        paramGroup.addParamChangeListener(new ParamChangeListener() {

            public void parameterValueChanged(final ParamChangeEvent event) {
                updateUIState();
            }
        });
    }

    private void initParameters(final int var) {

        final String paramPrefix = "var" + var + ".";
        final String axis = (var == VAR1) ? "X" : "Y";
        Object paramValue;

        autoMinMaxParams[var] = new Parameter(paramPrefix + "autoMinMax", Boolean.TRUE);
        autoMinMaxParams[var].getProperties().setLabel("Auto min/max");    /*I18N*/
        autoMinMaxParams[var].getProperties().setDescription("Automatically detect min/max for " + axis);  /*I18N*/
        paramGroup.addParameter(autoMinMaxParams[var]);

        paramValue = !(var == VAR1) ? new Float(0.0f) : new Integer(0);
        minParams[var] = new Parameter(paramPrefix + "min", paramValue);
        minParams[var].getProperties().setLabel("Min:");
        minParams[var].getProperties().setDescription("Minimum display value for " + axis);    /*I18N*/
        minParams[var].getProperties().setNumCols(7);
        if (var == VAR1) {
            minParams[var].getProperties().setValidatorClass(NumberValidator.class);
        }
        paramGroup.addParameter(minParams[var]);

        paramValue = !(var == VAR1) ? new Float(100.0f) : new Integer(100);
        maxParams[var] = new Parameter(paramPrefix + "max", paramValue);
        maxParams[var].getProperties().setLabel("Max:");
        maxParams[var].getProperties().setDescription("Maximum display value for " + axis);    /*I18N*/
        maxParams[var].getProperties().setNumCols(7);
        if (var == VAR1) {
            maxParams[var].getProperties().setValidatorClass(NumberValidator.class);
        }
        paramGroup.addParameter(maxParams[var]);

        if (var == VAR1) {
            markVerticesParam = new Parameter(paramPrefix + "markVertices", Boolean.TRUE);
            markVerticesParam.getProperties().setLabel("Mark vertices");
            markVerticesParam.getProperties().setDescription("Toggle whether or not to mark vertices");    /*I18N*/
            paramGroup.addParameter(markVerticesParam);
        }
    }

    private void createUI() {

        dataset = new XYSeriesCollection();
        chart = ChartFactory.createXYLineChart(
                "Profile Plot",
                "Way [pixel]",
                "Sample value",
                dataset,
                PlotOrientation.VERTICAL,
                false, // Legend?
                true,
                false
        );

        profilePlotDisplay = new ChartPanel(chart);
        profilePlotDisplay.getPopupMenu().add(createCopyDataToClipboardMenuItem());
        this.add(profilePlotDisplay, BorderLayout.CENTER);
        this.add(createOptionsPane(), BorderLayout.EAST);
    }

    private void updateUIState() {
        if (!isInitialized) {
            return;
        }
        updateUIState(VAR1);
        updateUIState(VAR2);
        setDiagramProperties();
    }

    private void setDiagramProperties() {
        if (!isInitialized) {
            return;
        }
 // todo
        /*
        profilePlotDisplay.setDiagramProperties(((Number) minParams[VAR1].getValue()).intValue(),
                                                 ((Number) maxParams[VAR1].getValue()).intValue(),
                                                 ((Number) minParams[VAR2].getValue()).floatValue(),
                                                 ((Number) maxParams[VAR2].getValue()).floatValue(),
                                                 (Boolean) markVerticesParam.getValue());
*/
    }


    private void updateUIState(final int var) {
        if (!isInitialized) {
            return;
        }


        if (profileData == null) {
            minParams[var].setUIEnabled(false);
            maxParams[var].setUIEnabled(false);
            return;
        }

        final boolean autoMinMaxEnabled = (Boolean) autoMinMaxParams[var].getValue();
        minParams[var].setUIEnabled(!autoMinMaxEnabled);
        maxParams[var].setUIEnabled(!autoMinMaxEnabled);

        if (autoMinMaxEnabled) {
            if (var == VAR1) {
                minParams[var].setValue(0, null);
                maxParams[var].setValue(profileData.getNumPixels() - 1, null);
            } else {
                final float v = MathUtils.computeRoundFactor(profileData.getSampleMin(), profileData.getSampleMax(), 4);
                minParams[var].setValue(StatisticsUtils.round(profileData.getSampleMin(), v), null);
                maxParams[var].setValue(StatisticsUtils.round(profileData.getSampleMax(), v), null);
            }
        } else {
            final float min = ((Number) minParams[var].getValue()).floatValue();
            final float max = ((Number) maxParams[var].getValue()).floatValue();
            if (min > max) {
                minParams[var].setValue(max, null);
                maxParams[var].setValue(min, null);
            }
        }
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

        GridBagUtils.setAttributes(gbc, "gridwidth=2,gridy=0,insets.top=4");
        GridBagUtils.addToPanel(optionsPane, autoMinMaxParams[var].getEditor().getComponent(), gbc,
                                "gridx=0,weightx=1");

        GridBagUtils.setAttributes(gbc, "gridwidth=1,gridy=1,insets.top=2");
        GridBagUtils.addToPanel(optionsPane, minParams[var].getEditor().getLabelComponent(), gbc,
                                "gridx=0,weightx=1");
        GridBagUtils.addToPanel(optionsPane, minParams[var].getEditor().getComponent(), gbc, "gridx=1,weightx=0");

        GridBagUtils.setAttributes(gbc, "gridwidth=1,gridy=2,insets.top=2");
        GridBagUtils.addToPanel(optionsPane, maxParams[var].getEditor().getLabelComponent(), gbc,
                                "gridx=0,weightx=1");
        GridBagUtils.addToPanel(optionsPane, maxParams[var].getEditor().getComponent(), gbc, "gridx=1,weightx=0");

        if (var == VAR1) {
            GridBagUtils.setAttributes(gbc, "gridwidth=2,gridy=3,insets.top=4");
            GridBagUtils.addToPanel(optionsPane, markVerticesParam.getEditor().getComponent(), gbc,
                                    "gridx=0,weightx=0");
        }

        optionsPane.setBorder(BorderFactory.createTitledBorder(var == 0 ? "X" : "Y"));

        return optionsPane;
    }

    @Override
    protected String getDataAsText() {
        try {
            return StatisticsUtils.TransectProfile.createTransectProfileText(getRaster());
        } catch (IOException e) {
            return "";
        }
    }

}
