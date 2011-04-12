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

package org.esa.beam.gpf.operators.reproject;

import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductFilter;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.crs.CrsForm;
import org.esa.beam.util.ProductUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class CollocationCrsForm extends CrsForm {

    private SourceProductSelector collocateProductSelector;


    public CollocationCrsForm(AppContext appContext) {
        super(appContext);
    }

    @Override
    protected String getLabelText() {
        return "Use CRS of";
    }

    @Override
    public CoordinateReferenceSystem getCRS(GeoPos referencePos) {
        Product collocationProduct = collocateProductSelector.getSelectedProduct();
        if (collocationProduct != null) {
            return collocationProduct.getGeoCoding().getMapCRS();
        }
        return null;
    }

    @Override
    protected JRadioButton createRadioButton() {
        final JRadioButton button = super.createRadioButton();
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final boolean collocate = button.isSelected();
                getCrsUI().firePropertyChange("collocate", !collocate, collocate);
            }
        });
        return button;

    }

    @Override
    public void prepareShow() {
        collocateProductSelector.initProducts();
    }

    @Override
    public void prepareHide() {
        collocateProductSelector.releaseProducts();
    }

    @Override
    protected JComponent createCrsComponent() {
        collocateProductSelector = new SourceProductSelector(getAppContext(), "Product:");
        collocateProductSelector.setProductFilter(new CollocateProductFilter());
        collocateProductSelector.addSelectionChangeListener(new AbstractSelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                fireCrsChanged();
            }
        });

        final JPanel panel = new JPanel(new BorderLayout(2, 2));
        panel.add(collocateProductSelector.getProductNameComboBox(), BorderLayout.CENTER);
        panel.add(collocateProductSelector.getProductFileChooserButton(), BorderLayout.EAST);
        panel.addPropertyChangeListener("enabled", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                collocateProductSelector.getProductNameComboBox().setEnabled(panel.isEnabled());
                collocateProductSelector.getProductFileChooserButton().setEnabled(panel.isEnabled());
                final boolean collocate = getRadioButton().isSelected();
                getCrsUI().firePropertyChange("collocate", !collocate, collocate);
            }
        });
        return panel;
    }


    public Product getCollocationProduct() {
        return collocateProductSelector.getSelectedProduct();
    }

    private class CollocateProductFilter implements ProductFilter {

        @Override
        public boolean accept(Product collocationProduct) {
            final Product referenceProduct = getReferenceProduct();
            if (referenceProduct == collocationProduct ||
                collocationProduct.getGeoCoding() == null) {
                return false;
            }
            if (referenceProduct == null) {
                return true;
            }
            final GeoCoding geoCoding = collocationProduct.getGeoCoding();
            if (geoCoding.canGetGeoPos() && geoCoding.canGetPixelPos() && (geoCoding instanceof CrsGeoCoding)) {
                final GeneralPath[] sourcePath = ProductUtils.createGeoBoundaryPaths(referenceProduct);
                final GeneralPath[] collocationPath = ProductUtils.createGeoBoundaryPaths(collocationProduct);
                for (GeneralPath path : sourcePath) {
                    Rectangle bounds = path.getBounds();
                    for (GeneralPath colPath : collocationPath) {
                        if (colPath.getBounds().intersects(bounds)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }


}
