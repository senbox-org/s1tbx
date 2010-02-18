/**
 * Created by IntelliJ IDEA.
 * User: administrator
 * Date: Nov 13, 2002
 * Time: 2:59:44 PM
 * To change this template use Options | File Templates.
 */
package org.esa.beam.visat.dialogs;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @deprecated since BEAM 4.7
 */
@Deprecated
public class OutputParamsDialog extends ModalDialog {

    private static final int PIXEL_REF_ULEFT = 1;
    private static final int PIXEL_REF_CENTER = 2;
    private static final int PIXEL_REF_OTHER = 3;

    private final Product _product;
    private MapInfo _mapInfo;
    private boolean _editable;

    private Parameter _paramPixelX;
    private Parameter _paramPixelY;
    private Parameter _paramNorthing;
    private Parameter _paramEasting;
    private Parameter _paramPixelSizeX;
    private Parameter _paramPixelSizeY;
    private Parameter _paramFitProductSize;
    private Parameter _paramOrientation;
    private Parameter _paramProductWidth;
    private Parameter _paramProductHeight;
    private Parameter _paramNoDataValue;
    private JRadioButton _pixelRefULeftButton;
    private JRadioButton _pixelRefCenterButton;
    private JRadioButton _pixelRefOtherButton;

    public OutputParamsDialog(Window parent, MapInfo mapInfo, Product product, boolean editable) {
        super(parent, "Output Parameters", ID_OK_CANCEL | ModalDialog.ID_HELP, "mapProjection"); /* I18N */
        Guardian.assertNotNull("mapInfo", mapInfo);
        Guardian.assertNotNull("product", product);
        _mapInfo = mapInfo;
        _product = product;
        _editable = editable;
        createParameters();
        createUI();
        updateUIState();
    }

    @Override
    protected void onOK() {
        super.onOK();
        if (_editable) {
            _mapInfo.setPixelX(((Float) _paramPixelX.getValue()).floatValue());
            _mapInfo.setPixelY(((Float) _paramPixelY.getValue()).floatValue());
            _mapInfo.setNorthing(((Float) _paramNorthing.getValue()).floatValue());
            _mapInfo.setEasting(((Float) _paramEasting.getValue()).floatValue());
            _mapInfo.setPixelSizeX(((Float) _paramPixelSizeX.getValue()).floatValue());
            _mapInfo.setPixelSizeY(((Float) _paramPixelSizeY.getValue()).floatValue());
            _mapInfo.setOrientation(((Float) _paramOrientation.getValue()).floatValue());
            _mapInfo.setSceneWidth(((Integer) _paramProductWidth.getValue()).intValue());
            _mapInfo.setSceneHeight(((Integer) _paramProductHeight.getValue()).intValue());
            _mapInfo.setSceneSizeFitted(((Boolean) _paramFitProductSize.getValue()).booleanValue());
        }
        _mapInfo.setNoDataValue(((Double) _paramNoDataValue.getValue()).doubleValue());
    }

    @Override
    protected void onCancel() {
        super.onCancel();
        _mapInfo = null;
    }

    public MapInfo getMapInfo() {
        return _mapInfo;
    }

    private void createUI() {
        int line = 0;
        JPanel dialogPane = GridBagUtils.createPanel();
        dialogPane.setBorder(new EmptyBorder(7, 7, 7, 7));
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        GridBagUtils.setAttributes(gbc, "insets.top=0,gridwidth=3");

        final ActionListener actionHandler = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateUIState();
            }
        };

        _pixelRefULeftButton = new JRadioButton("Reference pixel is at scene upper left", false);
        _pixelRefULeftButton.addActionListener(actionHandler);
        _pixelRefCenterButton = new JRadioButton("Reference pixel is at scene center", false);
        _pixelRefCenterButton.addActionListener(actionHandler);
        _pixelRefOtherButton = new JRadioButton("Other reference pixel position", false);
        _pixelRefOtherButton.addActionListener(actionHandler);
        ButtonGroup g = new ButtonGroup();
        g.add(_pixelRefULeftButton);
        g.add(_pixelRefCenterButton);
        g.add(_pixelRefOtherButton);
        if (_mapInfo.getPixelX() == 0.5f &&
            _mapInfo.getPixelY() == 0.5f) {
            _pixelRefULeftButton.setSelected(true);
        } else if (_mapInfo.getPixelX() == 0.5f * _mapInfo.getSceneWidth() &&
                   _mapInfo.getPixelY() == 0.5f * _mapInfo.getSceneHeight()) {
            _pixelRefCenterButton.setSelected(true);
        } else {
            _pixelRefOtherButton.setSelected(true);
        }

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, _pixelRefULeftButton, gbc, "fill=HORIZONTAL,weightx=1");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, _pixelRefCenterButton, gbc);
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, _pixelRefOtherButton, gbc);

        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, _paramPixelX.getEditor().getLabelComponent(), gbc,
                                "insets.top=1,gridwidth=1,fill=NONE,weightx=0");
        GridBagUtils.addToPanel(dialogPane, _paramPixelX.getEditor().getComponent(), gbc, "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, _paramPixelX.getEditor().getPhysUnitLabelComponent(), gbc,
                                "fill=NONE,weightx=0");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, _paramPixelY.getEditor().getLabelComponent(), gbc, "insets.top=3");
        GridBagUtils.addToPanel(dialogPane, _paramPixelY.getEditor().getComponent(), gbc, "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, _paramPixelY.getEditor().getPhysUnitLabelComponent(), gbc,
                                "fill=NONE,weightx=0");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, _paramNorthing.getEditor().getLabelComponent(), gbc, "insets.top=12");
        GridBagUtils.addToPanel(dialogPane, _paramNorthing.getEditor().getComponent(), gbc,
                                "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, _paramNorthing.getEditor().getPhysUnitLabelComponent(), gbc,
                                "fill=NONE,weightx=0");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, _paramEasting.getEditor().getLabelComponent(), gbc, "insets.top=3");
        GridBagUtils.addToPanel(dialogPane, _paramEasting.getEditor().getComponent(), gbc, "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, _paramEasting.getEditor().getPhysUnitLabelComponent(), gbc,
                                "fill=NONE,weightx=0");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, _paramOrientation.getEditor().getLabelComponent(), gbc, "insets.top=3");
        GridBagUtils.addToPanel(dialogPane, _paramOrientation.getEditor().getComponent(), gbc,
                                "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, _paramOrientation.getEditor().getPhysUnitLabelComponent(), gbc,
                                "fill=NONE,weightx=0");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, _paramPixelSizeX.getEditor().getLabelComponent(), gbc, "insets.top=12");
        GridBagUtils.addToPanel(dialogPane, _paramPixelSizeX.getEditor().getComponent(), gbc,
                                "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, _paramPixelSizeX.getEditor().getPhysUnitLabelComponent(), gbc,
                                "fill=NONE,weightx=0");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, _paramPixelSizeY.getEditor().getLabelComponent(), gbc, "insets.top=3");
        GridBagUtils.addToPanel(dialogPane, _paramPixelSizeY.getEditor().getComponent(), gbc,
                                "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, _paramPixelSizeY.getEditor().getPhysUnitLabelComponent(), gbc,
                                "fill=NONE,weightx=0");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, _paramFitProductSize.getEditor().getComponent(), gbc,
                                "insets.top=12, gridwidth=3,fill=HORIZONTAL,weightx=1");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, _paramProductWidth.getEditor().getLabelComponent(), gbc,
                                "insets.top=3, gridwidth=1,fill=NONE,weightx=0");
        GridBagUtils.addToPanel(dialogPane, _paramProductWidth.getEditor().getComponent(), gbc,
                                "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, _paramProductWidth.getEditor().getPhysUnitLabelComponent(), gbc,
                                "fill=NONE,weightx=0");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, _paramProductHeight.getEditor().getLabelComponent(), gbc);
        GridBagUtils.addToPanel(dialogPane, _paramProductHeight.getEditor().getComponent(), gbc,
                                "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, _paramProductHeight.getEditor().getPhysUnitLabelComponent(), gbc,
                                "fill=NONE,weightx=0");
        gbc.gridy = ++line;
        GridBagUtils.addToPanel(dialogPane, _paramNoDataValue.getEditor().getLabelComponent(), gbc,
                                "insets.top=12, gridwidth=1");
        GridBagUtils.addToPanel(dialogPane, _paramNoDataValue.getEditor().getComponent(), gbc,
                                "fill=HORIZONTAL,weightx=1");
        GridBagUtils.addToPanel(dialogPane, _paramNoDataValue.getEditor().getPhysUnitLabelComponent(), gbc,
                                "fill=NONE,weightx=0");

        setContent(dialogPane);
    }

    private void createParameters() {

        final ParamChangeListener paramChangeListener = new ParamChangeListener() {
            public void parameterValueChanged(ParamChangeEvent event) {
                updateUIState();
            }
        };
        final String mapUnit = _mapInfo.getMapProjection().getMapUnit();

        _paramPixelX = new Parameter("pixelX", new Float(_mapInfo.getPixelX()));
        _paramPixelX.getProperties().setLabel("Reference pixel X");
        _paramPixelX.getProperties().setPhysicalUnit("pixel");

        _paramPixelY = new Parameter("pixelY", new Float(_mapInfo.getPixelY()));
        _paramPixelY.getProperties().setLabel("Reference pixel Y");
        _paramPixelY.getProperties().setPhysicalUnit("pixel");

        _paramNorthing = new Parameter("northing", new Float(_mapInfo.getNorthing()));
        _paramNorthing.getProperties().setLabel("Northing at reference pixel");
        _paramNorthing.getProperties().setPhysicalUnit(mapUnit);

        _paramEasting = new Parameter("easting", new Float(_mapInfo.getEasting()));
        _paramEasting.getProperties().setLabel("Easting at reference pixel");
        _paramEasting.getProperties().setPhysicalUnit(mapUnit);

        _paramPixelSizeX = new Parameter("pixelSizeX", new Float(_mapInfo.getPixelSizeX()));
        _paramPixelSizeX.getProperties().setLabel("Pixel size X");
        _paramPixelSizeX.getProperties().setPhysicalUnit(mapUnit);
        _paramPixelSizeX.addParamChangeListener(paramChangeListener);

        _paramPixelSizeY = new Parameter("pixelSizeY", new Float(_mapInfo.getPixelSizeY()));
        _paramPixelSizeY.getProperties().setLabel("Pixel size Y");
        _paramPixelSizeY.getProperties().setPhysicalUnit(mapUnit);
        _paramPixelSizeY.addParamChangeListener(paramChangeListener);

        _paramOrientation = new Parameter("orientation", new Float(_mapInfo.getOrientation()));
        _paramOrientation.getProperties().setLabel("Orientation angle");
        _paramOrientation.getProperties().setPhysicalUnit("degree");
        _paramOrientation.addParamChangeListener(paramChangeListener);

        final Integer valueX = new Integer(_mapInfo.getSceneWidth());
        _paramProductWidth = new Parameter("producSceneWidth", valueX);
        _paramProductWidth.getProperties().setLabel("Product scene width");/*I18N*/
        _paramProductWidth.getProperties().setPhysicalUnit("pixel");

        final Integer valueY = new Integer(_mapInfo.getSceneHeight());
        _paramProductHeight = new Parameter("producSceneHeight", valueY);
        _paramProductHeight.getProperties().setLabel("Product scene height"); /*I18N*/
        _paramProductHeight.getProperties().setPhysicalUnit("pixel");

        _paramFitProductSize = new Parameter("fitProductSize",
                                             _mapInfo.isSceneSizeFitted() ? Boolean.TRUE : Boolean.FALSE);
        _paramFitProductSize.getProperties().setLabel("Adjust size (to include entire source region)"); /*I18N*/
        _paramFitProductSize.addParamChangeListener(paramChangeListener);

        _paramNoDataValue = new Parameter("noDataValue", new Double(_mapInfo.getNoDataValue()));
        _paramNoDataValue.getProperties().setLabel("Default no-data value"); /*I18N*/
        _paramNoDataValue.getProperties().setPhysicalUnit(" ");
    }

    private void updateUIState() {

        boolean editable = _editable;
        boolean fitProductSize = ((Boolean) _paramFitProductSize.getValue()).booleanValue();
        if (editable && fitProductSize) {
            final Dimension outputRasterSize = ProductUtils.getOutputRasterSize(_product,
                                                                                null,
                                                                                _mapInfo.getMapProjection().getMapTransform(),
                                                                                ((Float) _paramPixelSizeX.getValue()).floatValue(),
                                                                                ((Float) _paramPixelSizeY.getValue()).floatValue());
            _paramProductWidth.setValue(new Integer(outputRasterSize.width), null);
            _paramProductHeight.setValue(new Integer(outputRasterSize.height), null);
        }

        if (_pixelRefULeftButton.isSelected()) {
            final float pixelX = 0.5f;
            final float pixelY = 0.5f;
            _paramPixelX.setValue(new Float(pixelX), null);
            _paramPixelY.setValue(new Float(pixelY), null);
        } else if (_pixelRefCenterButton.isSelected()) {
            final float pixelX = 0.5f * ((Number) _paramProductWidth.getValue()).floatValue();
            final float pixelY = 0.5f * ((Number) _paramProductHeight.getValue()).floatValue();
            _paramPixelX.setValue(new Float(pixelX), null);
            _paramPixelY.setValue(new Float(pixelY), null);
        }

        _pixelRefULeftButton.setEnabled(editable);
        _pixelRefCenterButton.setEnabled(editable);
        _pixelRefOtherButton.setEnabled(editable);
        _paramFitProductSize.setUIEnabled(editable);
        _paramPixelX.setUIEnabled(editable && _pixelRefOtherButton.isSelected());
        _paramPixelY.setUIEnabled(editable && _pixelRefOtherButton.isSelected());
        _paramNorthing.setUIEnabled(editable);
        _paramEasting.setUIEnabled(editable);
        _paramPixelSizeX.setUIEnabled(editable);
        _paramPixelSizeY.setUIEnabled(editable);
        _paramOrientation.setUIEnabled(editable);
        _paramProductHeight.setUIEnabled(editable && !fitProductSize);
        _paramProductWidth.setUIEnabled(editable && !fitProductSize);
    }
}