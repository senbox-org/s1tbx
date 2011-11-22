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

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyPane;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.ui.ModalDialog;
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

    private final Product product;
    private final boolean canGetPixelPos;
    private final boolean canGetGeoPos;
    private final PlacemarkDescriptor placemarkDescriptor;
    private final BindingContext bindingContext;

    private boolean adjusting;

    public PlacemarkDialog(final Window parent, final Product product, final PlacemarkDescriptor placemarkDescriptor,
                           boolean geoAndPixelPositionsEditable) {
        super(parent, "New " + placemarkDescriptor.getRoleLabel(), ModalDialog.ID_OK_CANCEL, null); /*I18N*/
        Guardian.assertNotNull("product", product);
        this.product = product;
        this.placemarkDescriptor = placemarkDescriptor;
        bindingContext = new BindingContext();
        GeoCoding geoCoding = this.product.getGeoCoding();
        boolean hasGeoCoding = geoCoding != null;
        canGetPixelPos = hasGeoCoding && geoCoding.canGetPixelPos();
        canGetGeoPos = hasGeoCoding && geoCoding.canGetGeoPos();
        boolean usePixelPos = false;

        PropertySet propertySet = bindingContext.getPropertySet();
        propertySet.addProperties(Property.create("name", ""),
                                  Property.create("label", ""),
                                  Property.create("description", ""),
                                  Property.create("styleCss", ""),
                                  Property.create("lat", 0.0F),
                                  Property.create("lon", 0.0F),
                                  Property.create("pixelX", 0.0F),
                                  Property.create("pixelY", 0.0F),
                                  Property.create("usePixelPos", usePixelPos)
        );
        propertySet.getProperty("usePixelPos").getDescriptor().setAttribute("enabled", hasGeoCoding && geoAndPixelPositionsEditable);
        propertySet.getProperty("lat").getDescriptor().setDisplayName("Latitude");
        propertySet.getProperty("lat").getDescriptor().setUnit("deg");
        propertySet.getProperty("lon").getDescriptor().setDisplayName("Longitude");
        propertySet.getProperty("lon").getDescriptor().setUnit("deg");
        propertySet.getProperty("pixelX").getDescriptor().setDisplayName("Pixel X");
        propertySet.getProperty("pixelX").getDescriptor().setUnit("pixels");
        propertySet.getProperty("pixelY").getDescriptor().setDisplayName("Pixel Y");
        propertySet.getProperty("pixelY").getDescriptor().setUnit("pixels");


        PropertyChangeListener geoChangeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updatePixelPos();
            }
        };

        propertySet.getProperty("lat").addPropertyChangeListener(geoChangeListener);
        propertySet.getProperty("lon").addPropertyChangeListener(geoChangeListener);

        PropertyChangeListener pixelChangeListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateGeoPos();
            }
        };

        propertySet.getProperty("pixelX").addPropertyChangeListener(pixelChangeListener);
        propertySet.getProperty("pixelY").addPropertyChangeListener(pixelChangeListener);

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
        bindingContext.bindEnabledState("lat", false, "usePixelPos", true);
        bindingContext.bindEnabledState("lon", false, "usePixelPos", true);
        bindingContext.bindEnabledState("pixelX", true, "usePixelPos", true);
        bindingContext.bindEnabledState("pixelY", true, "usePixelPos", true);
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
        return bindingContext.getPropertySet().getValue("name");
    }

    public void setName(String name) {
        bindingContext.getPropertySet().setValue("name", name);
    }

    public String getLabel() {
        return bindingContext.getPropertySet().getValue("label");
    }

    public void setLabel(String label) {
        bindingContext.getPropertySet().setValue("label", label);
    }

    public String getDescription() {
        return bindingContext.getPropertySet().getValue("description");
    }

    public void setDescription(String description) {
        bindingContext.getPropertySet().setValue("description", description);
    }

    public String getStyleCss() {
        return bindingContext.getPropertySet().getValue("styleCss");
    }

    private void setStyleCss(String styleCss) {
        bindingContext.getPropertySet().setValue("styleCss", styleCss);
    }

    public float getPixelX() {
        return (Float) bindingContext.getPropertySet().getValue("pixelX");
    }

    public void setPixelX(float pixelX) {
        bindingContext.getPropertySet().setValue("pixelX", pixelX);
    }

    public float getPixelY() {
        return (Float) bindingContext.getPropertySet().getValue("pixelY");
    }

    public void setPixelY(float pixelY) {
        bindingContext.getPropertySet().setValue("pixelY", pixelY);
    }

    public float getLat() {
        return (Float) bindingContext.getPropertySet().getValue("lat");
    }

    public void setLat(float lat) {
        bindingContext.getPropertySet().setValue("lat", lat);
    }

    public float getLon() {
        return (Float) bindingContext.getPropertySet().getValue("lon");
    }

    public void setLon(float lon) {
        bindingContext.getPropertySet().setValue("lon", lon);
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
