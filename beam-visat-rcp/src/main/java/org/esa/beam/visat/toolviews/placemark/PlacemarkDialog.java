/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.visat.toolviews.placemark;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PlacemarkDescriptor;
import org.esa.beam.framework.datamodel.PlacemarkSymbol;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.util.Guardian;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Paint;
import java.awt.Window;
import java.awt.geom.Rectangle2D;

/**
 * A dialog used to create new placemarks or edit existing placemarks.
 */
public class PlacemarkDialog extends ModalDialog {

    private final Product product;
    private Parameter paramName;
    private Parameter paramLabel;
    private Parameter paramUsePixelPos;
    private Parameter paramLat;
    private Parameter paramLon;
    private Parameter paramPixelX;
    private Parameter paramPixelY;
    private Parameter paramDescription;
    private PlacemarkSymbol symbol;
    private JLabel symbolLabel;
    private Parameter paramColorOutline;
    private Parameter paramColorFill;
    private boolean canGetPixelPos;
    private boolean canGetGeoPos;
    private boolean adjusting;
    private boolean symbolChanged;
    private PlacemarkDescriptor placemarkDescriptor;
    private boolean simultaneousEditingAllowed;


    public PlacemarkDialog(final Window parent, final Product product, final PlacemarkDescriptor placemarkDescriptor,
                     boolean simultaneousEditingAllowed) {
        super(parent, "New " + placemarkDescriptor.getRoleLabel(), ModalDialog.ID_OK_CANCEL, null); /*I18N*/
        Guardian.assertNotNull("product", product);
        this.product = product;
        this.placemarkDescriptor = placemarkDescriptor;
        this.simultaneousEditingAllowed = simultaneousEditingAllowed;
        initParameter();
        creatUI();
    }

    public boolean isSimultaneousEditingAllowed() {
        return simultaneousEditingAllowed;
    }

    public Product getProduct() {
        return product;
    }

    @Override
    protected void onOK() {
        if (ProductNode.isValidNodeName(getName())) {
            if (symbolChanged) {
                // Create new symbol instance so an event is fired by placemark when new symbol is set.
                updateSymbolInstance();
            }
            super.onOK();
        } else {
            showInformationDialog("'" + getName() + "' is not a valid " + placemarkDescriptor.getRoleLabel() + " name."); /*I18N*/
        }
    }

    private void updateSymbolInstance() {
        PlacemarkSymbol symbol = PlacemarkSymbol.createDefaultPinSymbol();
        symbol.setOutlineColor(this.symbol.getOutlineColor());
        symbol.setOutlineStroke(this.symbol.getOutlineStroke());
        symbol.setFillPaint(this.symbol.getFillPaint());
        symbol.setFilled(this.symbol.isFilled());
        this.symbol = symbol;
    }

    public String getName() {
        return paramName.getValueAsText();
    }

    public void setName(String name) {
        paramName.setValueAsText(name, null);
    }

    public String getLabel() {
        return paramLabel.getValueAsText();
    }

    public void setLabel(String label) {
        paramLabel.setValueAsText(label, null);
    }

    public boolean isUsePixelPos() {
        return (Boolean) paramUsePixelPos.getValue();
    }

    /**
     * Sets whether or not to use the pixel co-ordinates instead of geographic co-ordinates. Has no effect if the
     * current product is null.
     *
     * @param usePixelPos whether or not to use the pixel co-ordinates instead of geographic co-ordinates
     */
    public void setUsePixelPos(boolean usePixelPos) {
        paramUsePixelPos.setValue(usePixelPos, null);
    }

    public float getLat() {
        return (Float) paramLat.getValue();
    }

    public void setLat(float lat) {
        paramLat.setValue(lat, null);
    }

    public float getLon() {
        return (Float) paramLon.getValue();
    }

    public void setLon(float lon) {
        paramLon.setValue(lon, null);
    }

    public GeoPos getGeoPos() {
        return new GeoPos(getLat(), getLon());
    }

    public void setGeoPos(GeoPos geoPos) {
        if (geoPos != null) {
            setLat(geoPos.lat);
            setLon(geoPos.lon);
        } else {
            setLat(0.0f);
            setLon(0.0f);
        }
    }

    public PixelPos getPixelPos() {
        return new PixelPos(getPixelX(), getPixelY());
    }

    public void setPixelPos(PixelPos pixelPos) {
        if (pixelPos != null) {
            setPixelX(pixelPos.x);
            setPixelY(pixelPos.y);
        } else {
            setPixelX(0.0F);
            setPixelY(0.0F);
        }
    }

    public float getPixelX() {
        return (Float) paramPixelX.getValue();
    }

    public void setPixelX(float pixelX) {
        paramPixelX.setValue(pixelX, null);
    }

    public float getPixelY() {
        return (Float) paramPixelY.getValue();
    }

    public void setPixelY(float pixelY) {
        paramPixelY.setValue(pixelY, null);
    }

    public String getDescription() {
        return paramDescription.getValueAsText();
    }

    public void setDescription(String description) {
        paramDescription.setValueAsText(description, null);
    }

    public PlacemarkSymbol getPlacemarkSymbol() {
        return symbol;
    }

    public void setPlacemarkSymbol(PlacemarkSymbol symbol) {
        Color fillColor = (Color) symbol.getFillPaint();
        Color outlineColor = symbol.getOutlineColor();
        paramColorFill.setValue(fillColor, null);
        paramColorOutline.setValue(outlineColor, null);
        this.symbol = symbol;
    }

    private void initParameter() {
        GeoCoding geoCoding = product.getGeoCoding();
        boolean hasGeoCoding = geoCoding != null;
        canGetPixelPos = hasGeoCoding && geoCoding.canGetPixelPos();
        canGetGeoPos = hasGeoCoding && geoCoding.canGetGeoPos();

        paramName = new Parameter("paramName", "");
        paramName.getProperties().setLabel("Name");/*I18N*/

        paramLabel = new Parameter("paramLabel", "");
        paramLabel.getProperties().setLabel("Label");/*I18N*/

        boolean usePixelPos = !hasGeoCoding;
        paramUsePixelPos = new Parameter("paramUsePixelPos", usePixelPos);
        paramUsePixelPos.getProperties().setLabel("Use pixel position");/*I18N*/
        paramUsePixelPos.setUIEnabled(canGetPixelPos || canGetGeoPos);
        paramUsePixelPos.addParamChangeListener(new ParamChangeListener() {
            @Override
            public void parameterValueChanged(ParamChangeEvent event) {
                if (isSimultaneousEditingAllowed()) {
                    boolean value = isUsePixelPos();
                    paramLat.setUIEnabled(!value);
                    paramLon.setUIEnabled(!value);
                    paramPixelX.setUIEnabled(value);
                    paramPixelY.setUIEnabled(value);
                }
            }
        });

        ParamChangeListener geoChangeListener = new ParamChangeListener() {
            @Override
            public void parameterValueChanged(ParamChangeEvent event) {
                updatePixelPos();
            }
        };

        paramLat = new Parameter("paramLat", 0.0f);
        paramLat.getProperties().setLabel("Lat");/*I18N*/
        paramLat.getProperties().setPhysicalUnit("deg"); /*I18N*/
        paramLat.setUIEnabled(!usePixelPos || !isSimultaneousEditingAllowed());
        paramLat.addParamChangeListener(geoChangeListener);

        paramLon = new Parameter("paramLon", 0.0f);
        paramLon.getProperties().setLabel("Lon");/*I18N*/
        paramLon.getProperties().setPhysicalUnit("deg");/*I18N*/
        paramLon.setUIEnabled(!usePixelPos || !isSimultaneousEditingAllowed());
        paramLon.addParamChangeListener(geoChangeListener);

        ParamChangeListener pixelChangeListener = new ParamChangeListener() {
            @Override
            public void parameterValueChanged(ParamChangeEvent event) {
                updateGeoPos();
            }
        };

        paramPixelX = new Parameter("paramPixelX", 0.0F);
        paramPixelX.getProperties().setLabel("Pixel X");
        paramPixelX.setUIEnabled(usePixelPos || !isSimultaneousEditingAllowed());
        paramPixelX.addParamChangeListener(pixelChangeListener);

        paramPixelY = new Parameter("paramPixelY", 0.0F);
        paramPixelY.getProperties().setLabel("Pixel Y");
        paramPixelY.setUIEnabled(usePixelPos || !isSimultaneousEditingAllowed());
        paramPixelY.addParamChangeListener(pixelChangeListener);

        paramDescription = new Parameter("paramDesc", "");
        paramDescription.getProperties().setLabel("Description"); /*I18N*/
        paramDescription.getProperties().setNumRows(3);

        if (symbol == null) {
            symbol = placemarkDescriptor.createDefaultSymbol();
        }

        ParamChangeListener colorChangelistener = new ParamChangeListener() {
            @Override
            public void parameterValueChanged(ParamChangeEvent event) {
                symbol.setFillPaint((Paint) paramColorFill.getValue());
                symbol.setOutlineColor((Color) paramColorOutline.getValue());
                symbolLabel.repaint();
                symbolChanged = true;
            }
        };

        paramColorOutline = new Parameter("outlineColor", symbol.getOutlineColor());
        paramColorOutline.getProperties().setLabel("Outline colour");
        paramColorOutline.getProperties().setNullValueAllowed(true);
        paramColorOutline.addParamChangeListener(colorChangelistener);
        paramColorOutline.setUIEnabled(symbol.getIcon() == null);

        paramColorFill = new Parameter("fillColor", symbol.getFillPaint());
        paramColorFill.getProperties().setLabel("Fill colour");
        paramColorFill.getProperties().setNullValueAllowed(true);
        paramColorFill.addParamChangeListener(colorChangelistener);
        paramColorFill.setUIEnabled(symbol.getIcon() == null);
    }

    private void creatUI() {

        symbolLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (g instanceof Graphics2D) {
                    Graphics2D g2d = (Graphics2D) g;
                    final PixelPos refPoint = symbol.getRefPoint();
                    Rectangle2D bounds = symbol.getBounds();
                    double tx = refPoint.getX() - bounds.getX() / 2;
                    double ty = refPoint.getY() - bounds.getY() / 2;
                    g2d.translate(tx, ty);
                    symbol.draw(g2d);
                    g2d.translate(-tx, -ty);
                }
            }
        };
        symbolLabel.setPreferredSize(new Dimension(40, 40));

        JPanel dialogPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        gbc.insets.top = 3;

        gbc.gridy++;
        gbc.gridwidth = 1;
        GridBagUtils.addToPanel(dialogPane, paramName.getEditor().getLabelComponent(), gbc);
        gbc.gridwidth = 4;
        GridBagUtils.addToPanel(dialogPane, paramName.getEditor().getComponent(), gbc,
                                "weightx=1, fill=HORIZONTAL");
        gbc.gridy++;
        gbc.gridwidth = 1;
        GridBagUtils.addToPanel(dialogPane, paramLabel.getEditor().getLabelComponent(), gbc);
        gbc.gridwidth = 4;
        GridBagUtils.addToPanel(dialogPane, paramLabel.getEditor().getComponent(), gbc,
                                "weightx=1, fill=HORIZONTAL");
        if (isSimultaneousEditingAllowed()) {
            gbc.gridwidth = 5;
            gbc.gridy++;
            GridBagUtils.addToPanel(dialogPane, paramUsePixelPos.getEditor().getComponent(), gbc);
        }

        gbc.gridy++;
        GridBagUtils.addToPanel(dialogPane, paramPixelX.getEditor().getLabelComponent(), gbc,
                                "weightx=0, gridwidth=1");
        final int space = 30;
        gbc.insets.right -= 2;
        GridBagUtils.addToPanel(dialogPane, paramPixelX.getEditor().getComponent(), gbc, "weightx=1");
        gbc.insets.right += 2;
        gbc.insets.left -= 2;
        GridBagUtils.addToPanel(dialogPane, new JLabel(""), gbc, "weightx=0");
        gbc.insets.left += 2;
        gbc.insets.left += space;
        GridBagUtils.addToPanel(dialogPane, paramLon.getEditor().getLabelComponent(), gbc, "weightx=0");
        gbc.insets.left -= space;
        GridBagUtils.addToPanel(dialogPane, paramLon.getEditor().getComponent(), gbc, "weightx=1");
        GridBagUtils.addToPanel(dialogPane, paramLon.getEditor().getPhysUnitLabelComponent(), gbc, "weightx=0");

        gbc.gridy++;
        GridBagUtils.addToPanel(dialogPane, paramPixelY.getEditor().getLabelComponent(), gbc);
        gbc.insets.right -= 2;
        GridBagUtils.addToPanel(dialogPane, paramPixelY.getEditor().getComponent(), gbc, "weightx=1");
        gbc.insets.right += 2;
        gbc.insets.left -= 2;
        GridBagUtils.addToPanel(dialogPane, new JLabel(""), gbc, "weightx=0");
        gbc.insets.left += 2;
        gbc.insets.left += space;
        GridBagUtils.addToPanel(dialogPane, paramLat.getEditor().getLabelComponent(), gbc, "weightx=0");
        gbc.insets.left -= space;
        GridBagUtils.addToPanel(dialogPane, paramLat.getEditor().getComponent(), gbc, "weightx=1");
        GridBagUtils.addToPanel(dialogPane, paramLat.getEditor().getPhysUnitLabelComponent(), gbc, "weightx=0");


        final int symbolSpace = 10;

        gbc.gridy++;
        gbc.insets.top += symbolSpace;
        GridBagUtils.addToPanel(dialogPane, createSymbolPane(), gbc, "fill=NONE, gridwidth=5, weightx=0");

        gbc.gridy++;
        GridBagUtils.addToPanel(dialogPane, paramDescription.getEditor().getLabelComponent(), gbc, "fill=BOTH");
        gbc.insets.top -= symbolSpace;
        gbc.gridy++;
        GridBagUtils.addToPanel(dialogPane, paramDescription.getEditor().getComponent(), gbc, "weighty=1");

        setContent(dialogPane);

        final JComponent editorComponent = paramName.getEditor().getEditorComponent();
        if (editorComponent instanceof JTextComponent) {
            JTextComponent tc = (JTextComponent) editorComponent;
            tc.selectAll();
        }
    }

    private JPanel createSymbolPane() {
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        gbc.insets.top = 3;
        final JPanel symbolPanel = GridBagUtils.createPanel();

        gbc.gridheight = 1;

        gbc.gridy = 0;
        gbc.gridx = 0;
        symbolPanel.add(paramColorFill.getEditor().getLabelComponent(), gbc);
        gbc.gridx = 1;
        symbolPanel.add(paramColorFill.getEditor().getComponent(), gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        symbolPanel.add(paramColorOutline.getEditor().getLabelComponent(), gbc);
        gbc.gridx = 1;
        symbolPanel.add(paramColorOutline.getEditor().getComponent(), gbc);

        gbc.gridy = 0;
        gbc.gridx = 2;
        gbc.gridheight = 2;
        gbc.insets.left = 10;
        symbolPanel.add(symbolLabel, gbc);
        gbc.insets.left = 0;

        return symbolPanel;
    }

    private void updatePixelPos() {
        if (canGetPixelPos && !adjusting) {
            adjusting = true;
            PixelPos pixelPos = placemarkDescriptor.updatePixelPos(product.getGeoCoding(), getGeoPos(), getPixelPos());
            setPixelPos(pixelPos);
            adjusting = false;
        }
    }

    private void updateGeoPos() {
        if (canGetGeoPos && !adjusting) {
            adjusting = true;
            GeoPos geoPos = placemarkDescriptor.updateGeoPos(product.getGeoCoding(), getPixelPos(), getGeoPos());
            setGeoPos(geoPos);
            adjusting = false;
        }
    }

    /**
     * Turns the first letter of the given string to upper case.
     *
     * @param string the string to change
     * @return a changed string
     */
    private static String firstLetterUp(String string) {
        String firstChar = string.substring(0, 1).toUpperCase();
        return firstChar + string.substring(1);
    }

    /**
     * Shows a dialog to edit the properties of an placemark.
     * If the placemark does not belong to a product it will be added after editing.
     *
     * @param parent the parent window fo the dialog
     * @param product the product where the placemark is already contained or where it will be added
     * @param placemark the placemark to edit
     * @param placemarkDescriptor the descriptor of the placemark
     * @return <code>true</code> if editing was successful, otherwise <code>false</code>.
     */
    public static boolean showEditPlacemarkDialog(Window parent, Product product, Placemark placemark,
                                            PlacemarkDescriptor placemarkDescriptor) {
        final PlacemarkDialog dialog = new PlacemarkDialog(parent, product, placemarkDescriptor,
                                                  placemarkDescriptor instanceof PinDescriptor);
        boolean belongsToProduct = placemark.getProduct() != null;
        String titlePrefix = belongsToProduct ? "Edit" : "New"; 
        String roleLabel = firstLetterUp(placemarkDescriptor.getRoleLabel());

        dialog.getJDialog().setTitle(titlePrefix + " " + roleLabel);
        dialog.getJDialog().setName(titlePrefix + "_" + roleLabel);
        dialog.setName(placemark.getName());
        dialog.setLabel(placemark.getLabel());
        dialog.setDescription(placemark.getDescription() != null ? placemark.getDescription() : "");
        // prevent that geoPos change updates pixelPos and vice versa during dialog creation
        dialog.adjusting = true;
        dialog.setPixelPos(placemark.getPixelPos());
        dialog.setGeoPos(placemark.getGeoPos());
        dialog.adjusting = false;
        dialog.setPlacemarkSymbol(placemark.getSymbol());
        boolean ok = (dialog.show() == ID_OK);
        if (ok) {
            if (!belongsToProduct) {
                // must add to product, otherwise setting the pixel position wil fail
                placemarkDescriptor.getPlacemarkGroup(product).add(placemark);
            }
            placemark.setName(dialog.getName());
            placemark.setLabel(dialog.getLabel());
            placemark.setDescription(dialog.getDescription());
            placemark.setGeoPos(dialog.getGeoPos());
            placemark.setPixelPos(dialog.getPixelPos());
            placemark.setSymbol(dialog.getPlacemarkSymbol());
        }
        return ok;
    }
}
