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

package org.esa.beam.pet.visat;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;

import javax.swing.AbstractListModel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Thomas Storm
 */
class InputListModel extends AbstractListModel {

    private List<Object> list = new ArrayList<Object>();
    private List<Product> sourceProducts = new ArrayList<Product>();
    private Property inputPaths;

    InputListModel(Property inputPaths) {
        this.inputPaths = inputPaths;
    }

    @Override
    public Object getElementAt(int index) {
        return list.get(index);
    }

    @Override
    public int getSize() {
        return list.size();
    }

    Product[] getSourceProducts() {
        return sourceProducts.toArray(new Product[sourceProducts.size()]);
    }

    void addElements(Object... elements) throws ValidationException {
        for (Object element : elements) {
            if (!(element instanceof File || element instanceof Product)) {
                throw new IllegalStateException(
                        "Only java.io.File or org.esa.beam.framework.datamodel.Product allowed.");
            }
            if (mayAdd(element)) {
                list.add(element);
            }
        }
        updateProperty();
        fireIntervalAdded(this, 0, list.size());
    }

    void clear() {
        list.clear();
        try {
            updateProperty();
        } catch (ValidationException ignored) {
        }
    }

    void removeElementsAt(int[] selectedIndices) {
        List<Object> toRemove = new ArrayList<Object>();
        for (int selectedIndex : selectedIndices) {
            toRemove.add(list.get(selectedIndex));
        }
        list.removeAll(toRemove);

        try {
            updateProperty();
        } catch (ValidationException ignored) {
        }
        fireIntervalRemoved(this, 0, list.size());
    }

    private void updateProperty() throws ValidationException {
        List<File> files = new ArrayList<File>();
        List<Product> products = new ArrayList<Product>();
        for (Object element : list) {
            if (element instanceof File) {
                files.add((File) element);
            } else if (element instanceof Product) {
                products.add((Product) element);

            }
        }
        inputPaths.setValue(files.toArray(new File[files.size()]));
        sourceProducts = products;
    }

    private boolean mayAdd(Object element) {
        if (list.contains(element)) {
            return false;
        }

        if (element instanceof Product) {
            return true;
        }

        File file = (File) element;
        if (file.isDirectory()) {
            return true;
        }
        Product product;
        try {
            product = ProductIO.readProduct(file);
        } catch (IOException e) {
            return false;
        }
        return product != null && !alreadyContained(product);
    }

    private boolean alreadyContained(Product product) {
        if (sourceProducts.contains(product)) {
            return true;
        }

        boolean isContained = false;
        for (Product sourceProduct : sourceProducts) {
            isContained |= areFairlyEqual(sourceProduct, product);
        }
        return isContained;
    }

    private boolean areFairlyEqual(Product product, Product sourceProduct) {
        if (!sourceProduct.getProductType().equals(product.getProductType())) {
            return false;
        }
        if (!sourceProduct.getName().equals(product.getName())) {
            return false;
        }
        if (sourceProduct.getSceneRasterHeight() != product.getSceneRasterHeight()) {
            return false;
        }
        if (sourceProduct.getSceneRasterWidth() != product.getSceneRasterWidth()) {
            return false;
        }
        String[] sourceBandNames = sourceProduct.getBandNames();
        String[] newBandNames = product.getBandNames();
        for (int i = 0; i < sourceBandNames.length; i++) {
            if (!sourceBandNames[i].equals(newBandNames[i])) {
                return false;
            }
        }

        return true;
    }
}
