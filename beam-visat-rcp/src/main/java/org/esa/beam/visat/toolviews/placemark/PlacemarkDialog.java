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
package org.esa.beam.visat.toolviews.placemark;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyPane;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.util.FeatureUtils;
import org.esa.beam.util.Guardian;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * A dialog used to create new placemarks or edit existing placemarks.
 */
public class PlacemarkDialog extends ModalDialog {

    private final static String PROPERTY_NAME_NAME = "name";
    private final static String PROPERTY_NAME_LABEL = "label";
    private final static String PROPERTY_NAME_DESCRIPTION = "description";
    private final static String PROPERTY_NAME_STYLE_CSS = "styleCss";
    private final static String PROPERTY_NAME_LAT = "lat";
    private final static String PROPERTY_NAME_LON = "lon";
    private final static String PROPERTY_NAME_PIXEL_X = "pixelX";
    private final static String PROPERTY_NAME_PIXEL_Y = "pixelY";
    private final static String PROPERTY_NAME_USE_PIXEL_POS = "usePixelPos";

    private final Product product;
    private final boolean canGetPixelPos;
    private final boolean canGetGeoPos;
    private final PlacemarkDescriptor placemarkDescriptor;

    private final BindingContext bindingContext;
    private boolean adjusting;

    public PlacemarkDialog(final Window parent, final Product product, final PlacemarkDescriptor placemarkDescriptor,
                           boolean switchGeoAndPixelPositionsEditable) {
        super(parent, "New " + placemarkDescriptor.getRoleLabel(), ModalDialog.ID_OK_CANCEL, null); /*I18N*/
        Guardian.assertNotNull("product", product);
        this.product = product;
        this.placemarkDescriptor = placemarkDescriptor;
        bindingContext = new BindingContext();
        final GeoCoding geoCoding = this.product.getGeoCoding();
        final boolean hasGeoCoding = geoCoding != null;
        canGetPixelPos = hasGeoCoding && geoCoding.canGetPixelPos();
        canGetGeoPos = hasGeoCoding && geoCoding.canGetGeoPos();

        boolean usePixelPos = !hasGeoCoding && switchGeoAndPixelPositionsEditable;

        PropertySet propertySet = bindingContext.getPropertySet();
        propertySet.addProperties(Property.create(PROPERTY_NAME_NAME, ""),
                                  Property.create(PROPERTY_NAME_LABEL, ""),
                                  Property.create(PROPERTY_NAME_DESCRIPTION, ""),
                                  Property.create(PROPERTY_NAME_STYLE_CSS, ""),
                                  Property.create(PROPERTY_NAME_LAT, 0.0F),
                                  Property.create(PROPERTY_NAME_LON, 0.0F),
                                  Property.create(PROPERTY_NAME_PIXEL_X, 0.0F),
                                  Property.create(PROPERTY_NAME_PIXEL_Y, 0.0F),
                                  Property.create(PROPERTY_NAME_USE_PIXEL_POS, usePixelPos)
        );
        propertySet.getProperty(PROPERTY_NAME_USE_PIXEL_POS).getDescriptor().setAttribute("enabled", hasGeoCoding && switchGeoAndPixelPositionsEditable);
        propertySet.getProperty(PROPERTY_NAME_LAT).getDescriptor().setDisplayName("Latitude");
        propertySet.getProperty(PROPERTY_NAME_LAT).getDescriptor().setUnit("deg");
        propertySet.getProperty(PROPERTY_NAME_LON).getDescriptor().setDisplayName("Longitude");
        propertySet.getProperty(PROPERTY_NAME_LON).getDescriptor().setUnit("deg");
        propertySet.getProperty(PROPERTY_NAME_PIXEL_X).getDescriptor().setDisplayName("Pixel X");
        propertySet.getProperty(PROPERTY_NAME_PIXEL_X).getDescriptor().setUnit("pixels");
        propertySet.getProperty(PROPERTY_NAME_PIXEL_Y).getDescriptor().setDisplayName("Pixel Y");
        propertySet.getProperty(PROPERTY_NAME_PIXEL_Y).getDescriptor().setUnit("pixels");


        PropertyChangeListener geoChangeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updatePixelPos();
            }
        };

        propertySet.getProperty(PROPERTY_NAME_LAT).addPropertyChangeListener(geoChangeListener);
        propertySet.getProperty(PROPERTY_NAME_LON).addPropertyChangeListener(geoChangeListener);

        PropertyChangeListener pixelChangeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateGeoPos();
            }
        };

        propertySet.getProperty(PROPERTY_NAME_PIXEL_X).addPropertyChangeListener(pixelChangeListener);
        propertySet.getProperty(PROPERTY_NAME_PIXEL_Y).addPropertyChangeListener(pixelChangeListener);

        /*
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
        */

        setContent(new PropertyPane(bindingContext).createPanel());
        if (switchGeoAndPixelPositionsEditable) {
            bindingContext.bindEnabledState(PROPERTY_NAME_LAT, false, PROPERTY_NAME_USE_PIXEL_POS, true);
            bindingContext.bindEnabledState(PROPERTY_NAME_LON, false, PROPERTY_NAME_USE_PIXEL_POS, true);
            bindingContext.bindEnabledState(PROPERTY_NAME_PIXEL_X, true, PROPERTY_NAME_USE_PIXEL_POS, true);
            bindingContext.bindEnabledState(PROPERTY_NAME_PIXEL_Y, true, PROPERTY_NAME_USE_PIXEL_POS, true);
        }
    }

    public Product getProduct() {
        return product;
    }

    @Override
    protected void onOK() {
        if (ProductNode.isValidNodeName(getName())) {
            super.onOK();
        } else {
            showInformationDialog("'" + getName() + "' is not a valid " + placemarkDescriptor.getRoleLabel() + " name."); /*I18N*/
        }
    }

    public String getName() {
        return bindingContext.getPropertySet().getValue(PROPERTY_NAME_NAME);
    }

    public void setName(String name) {
        bindingContext.getPropertySet().setValue(PROPERTY_NAME_NAME, name);
    }

    public String getLabel() {
        return bindingContext.getPropertySet().getValue(PROPERTY_NAME_LABEL);
    }

    public void setLabel(String label) {
        bindingContext.getPropertySet().setValue(PROPERTY_NAME_LABEL, label);
    }

    public String getDescription() {
        return bindingContext.getPropertySet().getValue(PROPERTY_NAME_DESCRIPTION);
    }

    public void setDescription(String description) {
        bindingContext.getPropertySet().setValue(PROPERTY_NAME_DESCRIPTION, description);
    }

    public String getStyleCss() {
        return bindingContext.getPropertySet().getValue(PROPERTY_NAME_STYLE_CSS);
    }

    private void setStyleCss(String styleCss) {
        bindingContext.getPropertySet().setValue(PROPERTY_NAME_STYLE_CSS, styleCss);
    }

    public float getPixelX() {
        return (Float) bindingContext.getPropertySet().getValue(PROPERTY_NAME_PIXEL_X);
    }

    public void setPixelX(float pixelX) {
        bindingContext.getPropertySet().setValue(PROPERTY_NAME_PIXEL_X, pixelX);
    }

    public float getPixelY() {
        return (Float) bindingContext.getPropertySet().getValue(PROPERTY_NAME_PIXEL_Y);
    }

    public void setPixelY(float pixelY) {
        bindingContext.getPropertySet().setValue(PROPERTY_NAME_PIXEL_Y, pixelY);
    }

    public float getLat() {
        return (Float) bindingContext.getPropertySet().getValue(PROPERTY_NAME_LAT);
    }

    public void setLat(float lat) {
        bindingContext.getPropertySet().setValue(PROPERTY_NAME_LAT, lat);
    }

    public float getLon() {
        return (Float) bindingContext.getPropertySet().getValue(PROPERTY_NAME_LON);
    }

    public void setLon(float lon) {
        bindingContext.getPropertySet().setValue(PROPERTY_NAME_LON, lon);
    }

    public GeoPos getGeoPos() {
        return new GeoPos(getLat(), getLon());
    }

    public void setGeoPos(GeoPos geoPos) {
        if (geoPos != null) {
            setLat(geoPos.lat);
            setLon(geoPos.lon);
        } else {
            setLat(0.0F);
            setLon(0.0F);
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
     * Shows a dialog to edit the properties of an placemark.
     * If the placemark does not belong to a product it will be added after editing.
     *
     * @param parent              the parent window fo the dialog
     * @param product             the product where the placemark is already contained or where it will be added
     * @param placemark           the placemark to edit
     * @param placemarkDescriptor the descriptor of the placemark
     * @return <code>true</code> if editing was successful, otherwise <code>false</code>.
     */
    public static boolean showEditPlacemarkDialog(Window parent, Product product, Placemark placemark,
                                                  PlacemarkDescriptor placemarkDescriptor) {
        final PlacemarkDialog dialog = new PlacemarkDialog(parent, product, placemarkDescriptor,
                                                           placemarkDescriptor instanceof PinDescriptor);
        boolean belongsToProduct = placemark.getProduct() != null;
        String titlePrefix = belongsToProduct ? "Edit" : "New";
        String roleLabel = FeatureUtils.firstLetterUp(placemarkDescriptor.getRoleLabel());

        dialog.getJDialog().setTitle(titlePrefix + " " + roleLabel);
        dialog.getJDialog().setName(titlePrefix + "_" + roleLabel);
        dialog.setName(placemark.getName());
        dialog.setLabel(placemark.getLabel());
        dialog.setDescription(placemark.getDescription() != null ? placemark.getDescription() : "");
        // prevent that geoPos change updates pixelPos and vice versa during dialog creation
        dialog.adjusting = true;
        dialog.setPixelPos(placemark.getPixelPos());
        GeoPos geoPos = placemark.getGeoPos();
        dialog.setGeoPos(geoPos != null ? geoPos : new GeoPos(Float.NaN, Float.NaN));
        dialog.adjusting = false;
        dialog.setStyleCss(placemark.getStyleCss());
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
            placemark.setStyleCss(dialog.getStyleCss());
        }
        return ok;
    }


    public static void main(String[] args) throws TransformException, FactoryException {
        Product product1 = new Product("A", "B", 360, 180);
        product1.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 360, 180, -180.0, 90.0, 1.0, 1.0, 0.0, 0.0));
        PinDescriptor descriptor = PinDescriptor.getInstance();
        Placemark pin1 = Placemark.createPointPlacemark(descriptor, "pin_1", "Pin 1", "Schnatter!", new PixelPos(0, 0), new GeoPos(), product1.getGeoCoding());
        product1.getPinGroup().add(pin1);
        showEditPlacemarkDialog(null, product1, pin1, descriptor);
    }
}
