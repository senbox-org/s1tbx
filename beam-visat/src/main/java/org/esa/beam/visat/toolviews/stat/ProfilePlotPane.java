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

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.io.IOException;

/**
 * The profile plot pane within the statistcs window.
 *
 * @author Marco Peters
 */
class ProfilePlotPane extends PagePane {

    private static final String _TITLE_PREFIX = "Profile Plot"; /*I18N*/
    private static final String _DEFAULT_PROFILEPLOT_TEXT = "No profile plot computed yet. " +
            "It will be computed if a shape is added to the image view.";    /*I18N*/

    private static final int VAR1 = 0;
    private static final int VAR2 = 1;


    private final static LayerObserver _figureLayerObserver = LayerObserver.getInstance(FigureLayer.class);
    private final static Parameter[] _autoMinMaxParams = new Parameter[2];
    private final static Parameter[] _minParams = new Parameter[2];
    private final static Parameter[] _maxParams = new Parameter[2];
    private static Parameter _markVerticesParam = new Parameter("markVertices");
    private ParamGroup _paramGroup;

    private ProfilePlotDisplay _profilePlotDisplay;
    private static boolean _isInitialized = false;

    public ProfilePlotPane(final ToolView parentDialog) {
        super(parentDialog);
        _figureLayerObserver.addLayerObserverListener(new LayerObserver.LayerObserverListener() {
            public void layerChanged() {
                updateContent();
            }
        });
        _figureLayerObserver.setRaster(getRaster());
    }

    @Override
    protected String getTitlePrefix() {
        return _TITLE_PREFIX;
    }

    @Override
    protected void setRaster(final RasterDataNode raster) {
        final RasterDataNode oldRaster = super.getRaster();
        if (oldRaster != raster) {
            _figureLayerObserver.setRaster(raster);
        }
        super.setRaster(raster);
    }

    @Override
    protected void initContent() {
        initParameters();
        createUI();
        _isInitialized = true;
        updateContent();
    }

    @Override
    protected void updateContent() {
        if (!_isInitialized) {
            return;
        }
        final TransectProfileData data;
        try {
            data = StatisticsUtils.TransectProfile.getTransectProfileData(getRaster());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(getParent(),
                                          "Failed to compute profile plot.\n" +
                                                  "An I/O error occured:" + e.getMessage(),
                                          "I/O error",
                                          JOptionPane.ERROR_MESSAGE);   /*I18N*/
            return;
        }
        if (data != null) {

            final Number minX = 0;
            final Number maxX = data.getNumPixels() - 1;
            final Number minY = StatisticsUtils.round(data.getSampleMin());
            final Number maxY = StatisticsUtils.round(data.getSampleMax());

            _minParams[VAR1].getProperties().setMinValue(minX);
            _minParams[VAR1].getProperties().setMaxValue(maxX);
            _minParams[VAR1].setValue(minX, null);
            _maxParams[VAR1].getProperties().setMinValue(minX);
            _maxParams[VAR1].getProperties().setMaxValue(maxX);
            _maxParams[VAR1].setValue(maxX, null);

            _minParams[VAR2].setValue(minY, null);
            //            _minParams[VAR2].getProperties().setMinValue(minY);
            //            _minParams[VAR2].getProperties().setMaxValue(maxY);
            _maxParams[VAR2].setValue(maxY, null);
            //            _maxParams[VAR2].getProperties().setMinValue(minY);
            //            _maxParams[VAR2].getProperties().setMaxValue(maxY);

            _markVerticesParam.setUIEnabled(data.getShapeVertices().length > 2);
        }

        updateUIState();

        setDiagramProperties();
    }


    private void initParameters() {
        _paramGroup = new ParamGroup();
        initParameters(VAR1);
        initParameters(VAR2);
        _paramGroup.addParamChangeListener(new ParamChangeListener() {

            public void parameterValueChanged(final ParamChangeEvent event) {
                updateUIState();
            }
        });
    }

    private void initParameters(final int var) {

        final String paramPrefix = "var" + var + ".";
        final String axis = (var == VAR1) ? "X" : "Y";
        Object paramValue;

        _autoMinMaxParams[var] = new Parameter(paramPrefix + "autoMinMax", Boolean.TRUE);
        _autoMinMaxParams[var].getProperties().setLabel("Auto min/max");    /*I18N*/
        _autoMinMaxParams[var].getProperties().setDescription("Automatically detect min/max for " + axis);  /*I18N*/
        _paramGroup.addParameter(_autoMinMaxParams[var]);

        paramValue = !(var == VAR1) ? (Object) new Float(0.0f) : (Object) new Integer(0);
        _minParams[var] = new Parameter(paramPrefix + "min", paramValue);
        _minParams[var].getProperties().setLabel("Min:");
        _minParams[var].getProperties().setDescription("Minimum display value for " + axis);    /*I18N*/
        _minParams[var].getProperties().setNumCols(7);
        if (var == VAR1) {
            _minParams[var].getProperties().setValidatorClass(NumberValidator.class);
        }
        _paramGroup.addParameter(_minParams[var]);

        paramValue = !(var == VAR1) ? (Object) new Float(100.0f) : (Object) new Integer(100);
        _maxParams[var] = new Parameter(paramPrefix + "max", paramValue);
        _maxParams[var].getProperties().setLabel("Max:");
        _maxParams[var].getProperties().setDescription("Maximum display value for " + axis);    /*I18N*/
        _maxParams[var].getProperties().setNumCols(7);
        if (var == VAR1) {
            _maxParams[var].getProperties().setValidatorClass(NumberValidator.class);
        }
        _paramGroup.addParameter(_maxParams[var]);

        if (var == VAR1) {
            _markVerticesParam = new Parameter(paramPrefix + "markVertices", Boolean.TRUE);
            _markVerticesParam.getProperties().setLabel("Mark vertices");
            _markVerticesParam.getProperties().setDescription("Toggle whether or not to mark vertices");    /*I18N*/
            _paramGroup.addParameter(_markVerticesParam);
        }
    }

    private void createUI() {

        _profilePlotDisplay = new ProfilePlotDisplay();
        _profilePlotDisplay.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        _profilePlotDisplay.addMouseListener(new PopupHandler());

        this.add(_profilePlotDisplay, BorderLayout.CENTER);
        this.add(createOptionsPane(), BorderLayout.EAST);
    }

    private void updateUIState() {
        if (!_isInitialized) {
            return;
        }
        updateUIState(VAR1);
        updateUIState(VAR2);
        setDiagramProperties();
    }

    private void setDiagramProperties() {
        if (!_isInitialized) {
            return;
        }
        _profilePlotDisplay.setDiagramProperties(((Number) _minParams[VAR1].getValue()).intValue(),
                                                 ((Number) _maxParams[VAR1].getValue()).intValue(),
                                                 ((Number) _minParams[VAR2].getValue()).floatValue(),
                                                 ((Number) _maxParams[VAR2].getValue()).floatValue(),
                                                 (Boolean) _markVerticesParam.getValue());
    }


    private void updateUIState(final int var) {
        if (!_isInitialized) {
            return;
        }

        TransectProfileData data;
        try {
            data = StatisticsUtils.TransectProfile.getTransectProfileData(getRaster());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(getParent(),
                                          "Failed to compute profile plot.\n" +
                                                  "An I/O error occured:" + e.getMessage(),
                                          "I/O error",
                                          JOptionPane.ERROR_MESSAGE);       /*I18N*/
            data = null;
        }

        if (data == null) {
            _minParams[var].setUIEnabled(false);
            _maxParams[var].setUIEnabled(false);
            return;
        }

        final boolean autoMinMaxEnabled = (Boolean) _autoMinMaxParams[var].getValue();
        _minParams[var].setUIEnabled(!autoMinMaxEnabled);
        _maxParams[var].setUIEnabled(!autoMinMaxEnabled);

        if (autoMinMaxEnabled) {
            if (var == VAR1) {
                _minParams[var].setValue(0, null);
                _maxParams[var].setValue(data.getNumPixels() - 1, null);
            } else {
                final float v = MathUtils.computeRoundFactor(data.getSampleMin(), data.getSampleMax(), 4);
                _minParams[var].setValue(StatisticsUtils.round(data.getSampleMin(), v), null);
                _maxParams[var].setValue(StatisticsUtils.round(data.getSampleMax(), v), null);
            }
        } else {
            final float min = ((Number) _minParams[var].getValue()).floatValue();
            final float max = ((Number) _maxParams[var].getValue()).floatValue();
            if (min > max) {
                _minParams[var].setValue(max, null);
                _maxParams[var].setValue(min, null);
            }
        }
    }


    private JPanel createOptionsPane() {
        final JPanel optionsPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("anchor=NORTHWEST,fill=BOTH");

        GridBagUtils.setAttributes(gbc, "gridy=1,weightx=1");
        GridBagUtils.addToPanel(optionsPane, createOptionsPane(VAR1), gbc, "gridy=0,insets.top=0");
        GridBagUtils.addToPanel(optionsPane, createOptionsPane(VAR2), gbc, "gridy=1,insets.top=7");
        GridBagUtils.addVerticalFiller(optionsPane, gbc);

        return optionsPane;
    }

    private JPanel createOptionsPane(final int var) {

        final JPanel optionsPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints("anchor=WEST,fill=HORIZONTAL");

        GridBagUtils.setAttributes(gbc, "gridwidth=2,gridy=0,insets.top=4");
        GridBagUtils.addToPanel(optionsPane, _autoMinMaxParams[var].getEditor().getComponent(), gbc,
                                "gridx=0,weightx=1");

        GridBagUtils.setAttributes(gbc, "gridwidth=1,gridy=1,insets.top=2");
        GridBagUtils.addToPanel(optionsPane, _minParams[var].getEditor().getLabelComponent(), gbc,
                                "gridx=0,weightx=1");
        GridBagUtils.addToPanel(optionsPane, _minParams[var].getEditor().getComponent(), gbc, "gridx=1,weightx=0");

        GridBagUtils.setAttributes(gbc, "gridwidth=1,gridy=2,insets.top=2");
        GridBagUtils.addToPanel(optionsPane, _maxParams[var].getEditor().getLabelComponent(), gbc,
                                "gridx=0,weightx=1");
        GridBagUtils.addToPanel(optionsPane, _maxParams[var].getEditor().getComponent(), gbc, "gridx=1,weightx=0");

        if (var == VAR1) {
            GridBagUtils.setAttributes(gbc, "gridwidth=2,gridy=3,insets.top=4");
            GridBagUtils.addToPanel(optionsPane, _markVerticesParam.getEditor().getComponent(), gbc,
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

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Profile Plot Display
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private class ProfilePlotDisplay extends JPanel {

        private int _minX;
        private int _maxX;
        private float _minY;
        private float _maxY;
        private boolean _markVertices;

        public ProfilePlotDisplay() {
            this.setBackground(Color.white);
            this.setForeground(Color.black);
        }

        public void setDiagramProperties(final int minX, final int maxX, final float minY, final float maxY,
                                         final boolean markVertices) {
            _minX = minX;
            _maxX = maxX;
            _minY = minY;
            _maxY = maxY;
            _markVertices = markVertices;
            this.repaint();
        }


        /**
         * If the UI delegate is non-null, calls its paint method.  We pass the delegate a copy of the Graphics object
         * to protect the rest of the paint code from irrevocable changes (for example, Graphics.translate()).
         *
         * @param g the Graphics object to protect
         * @see #paint
         */
        @Override
        protected void paintComponent(final Graphics g) {
            super.paintComponent(g);
            draw((Graphics2D) g);
        }


        private void draw(final Graphics2D g2d) {

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            final Insets insets = this.getInsets();

            g2d.setColor(this.getBackground());
            g2d.fillRect(insets.left,
                         insets.top,
                         this.getWidth() - insets.left - insets.right,
                         this.getHeight() - insets.top - insets.bottom);

            final FontMetrics fm = g2d.getFontMetrics();
            final int fh = fm.getHeight();

            TransectProfileData data;
            try {
                data = StatisticsUtils.TransectProfile.getTransectProfileData(getRaster());
            } catch (IOException e) {
                data = null;
            }
            if (data == null || getRaster() == null) {
                g2d.setColor(StatisticsToolView.DIAGRAM_TEXT_COLOR);
                g2d.drawString(_DEFAULT_PROFILEPLOT_TEXT, insets.left + 1, insets.top + fh);
            } else {
                final int diagX0 = StatisticsToolView.DIAGRAM_MIN_INSETS + insets.left + 2 * fh;
                final int diagY0 = StatisticsToolView.DIAGRAM_MIN_INSETS + insets.top;
                final int diagW = this.getWidth() - 2 * StatisticsToolView.DIAGRAM_MIN_INSETS - (insets.left + insets.right) - 2 * fh - 1;
                final int diagH = this.getHeight() - 2 * StatisticsToolView.DIAGRAM_MIN_INSETS - (insets.top + insets.bottom) - 2 * fh - 1;
                drawProfilePlot(g2d, diagX0, diagY0, diagW, diagH);
                drawDiagramText(g2d, diagX0, diagY0, diagW, diagH, fm);
            }

        }


        private void drawProfilePlot(final Graphics2D g2d, final int diagX0, final int diagY0, final int diagW,
                                     final int diagH) {

            final TransectProfileData data;
            try {
                data = StatisticsUtils.TransectProfile.getTransectProfileData(getRaster());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(getParent(),
                                              "Failed to compute profile plot.\n" +
                                                      "An I/O error occured:" + e.getMessage(),
                                              "I/O error",
                                              JOptionPane.ERROR_MESSAGE);   /*I18N*/
                return;
            }
            if (data == null) {
                return;
            }

            final int[] vertexIndices = data.getShapeVertexIndexes();
            final float[] sampleValues = data.getSampleValues();

            g2d.translate(diagX0, diagY0);

            g2d.setColor(StatisticsToolView.DIAGRAM_BG_COLOR);
            g2d.fillRect(-1, -1, diagW + 1, diagH + 1);

//            Rectangle newClip = new Rectangle(0, 0, diagW, diagH);
//            Shape oldClip = g2d.getClip();
//            g2d.setClip(newClip);

            g2d.setColor(Color.white);
            final int diagY = (int) ((0.0F - _minY) / (_maxY - _minY) * diagH); //todo 1 he/nf - risky int cast ?
            if (diagY > 0 && diagY < diagH) {
                g2d.drawLine(0, diagY, diagW, diagY);
            }
            if (_markVertices) {
                for (int vertexIndice : vertexIndices) {
                    final int diagX = (int) ((float) (vertexIndice - _minX) / (_maxX - _minX) * diagW);
                    if (diagX > 0 && diagX < diagW) {
                        g2d.drawLine(diagX, 0, diagX, diagH);
                    }
                }
            }

            g2d.setColor(StatisticsToolView.DIAGRAM_FG_COLOR);
            int diagX2 = 0, diagY2 = 0;
            for (int x = _minX; x <= _maxX; x++) {
                final int diagX1 = (int) ((float) (x - _minX) / (_maxX - _minX) * diagW); //todo 1 he/nf -  risky int cast ?
                final int diagY1 = (int) (diagH - (sampleValues[x] - _minY) / (_maxY - _minY) * diagH); //todo 1 he/nf - risky int cast ?
                if (x > _minX) {
                    g2d.drawLine(diagX1, diagY1, diagX2, diagY2);
                }
                diagX2 = diagX1;
                diagY2 = diagY1;
            }

//            g2d.setClip(oldClip);

            g2d.setColor(this.getForeground());
            g2d.drawRect(-1, -1, diagW + 1, diagH + 1);

            g2d.translate(-diagX0, -diagY0);
        }

        private void drawDiagramText(final Graphics2D g2d, final int diagX0, final int diagY0, final int diagW,
                                     final int diagH, final FontMetrics fm) {

            final int fontY = fm.getLeading() + fm.getDescent();
            final int fontH = fm.getHeight();

            String text;
            int textW;

            g2d.setColor(StatisticsToolView.DIAGRAM_TEXT_COLOR);

            text = String.valueOf(_minX);
            g2d.drawString(text, diagX0, diagY0 + diagH + fontH);

            text = String.valueOf(_maxX);
            textW = fm.stringWidth(text);
            g2d.drawString(text, diagX0 + diagW - textW, diagY0 + diagH + fontH);

            text = String.valueOf((_minX + _maxX) / 2);
            textW = fm.stringWidth(text);
            g2d.drawString(text, diagX0 + (diagW - textW) / 2, diagY0 + diagH + fontH);

            text = "Way (Pixel)";
            textW = fm.stringWidth(text);
            g2d.drawString(text, diagX0 + (diagW - textW) / 2, diagY0 + diagH + 2 * fontH);

            final int translX = diagX0 - fontY;
            final int translY = diagY0 + diagH;
            final double rotA = -0.5 * Math.PI;

            g2d.translate(translX, translY);
            g2d.rotate(rotA);

            text = String.valueOf(_minY);
            g2d.drawString(text, 0, 0);

            text = String.valueOf(_maxY);
            textW = fm.stringWidth(text);
            g2d.drawString(text, diagH - textW, -fontY);

            text = String.valueOf(0.5F * (_minY + _maxY));
            textW = fm.stringWidth(text);
            g2d.drawString(text, (diagH - textW) / 2, -fontY);

            text = StatisticsUtils.getDiagramLabel(getRaster());
            textW = fm.stringWidth(text);
            g2d.drawString(text, (diagH - textW) / 2, -fontY - fontH);

            g2d.rotate(-rotA);
            g2d.translate(-translX, -translY);
        }
    }

}
