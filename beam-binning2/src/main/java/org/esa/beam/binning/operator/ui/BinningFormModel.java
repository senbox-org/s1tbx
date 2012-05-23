/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.operator.ui;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.beam.framework.datamodel.Product;

import java.beans.PropertyChangeListener;

/**
 * The model responsible for managing the binning parameters.
 *
 * @author Thomas Storm
 */
interface BinningFormModel {

    String PROPERTY_KEY_SOURCE_PRODUCTS = "sourceProducts";
    String PROPERTY_KEY_VARIABLE_CONFIGS = "variableConfigs";
    String PROPERTY_KEY_REGION = "region";
    String PROPERTY_KEY_COMPUTE_REGION = "compute";
    String PROPERTY_KEY_GLOBAL = "global";
    String PROPERTY_KEY_EXPRESSION = "expression";
    String PROPERTY_KEY_TEMPORAL_FILTER = "temporalFilter";
    String PROPERTY_KEY_START_DATE = "startDate";
    String PROPERTY_KEY_END_DATE = "endDate";
    String PROPERTY_KEY_OUTPUT_BINNED_DATA = "outputBinnedData";
    String PROPERTY_KEY_TARGET_HEIGHT = "targetHeight";
    String PROPERTY_KEY_SUPERSAMPLING = "supersampling";
    String PROPERTY_KEY_MANUAL_WKT = "manualWktKey";

    void setProperty(String key, Object value) throws ValidationException;

    void addPropertyChangeListener(PropertyChangeListener propertyChangeListener);

    BindingContext getBindingContext();

    /**
     * @return The source products of this binning operation, never <code>null</code>.
     */
    Product[] getSourceProducts();

    /**
     * @return The variable configurations.
     */
    TableRow[] getTableRows();

    /**
     * @return The selected target region.
     */
    String getRegion();

    /**
     * @return the expression good pixels in the target product need to comply with.
     */
    String getValidExpression();

    /**
     * @return the user-chosen start date; <code>null</code> if no start date has been chosen
     */
    String getStartDate();

    /**
     * @return the user-chosen end date; <code>null</code> if no end date has been chosen
     */
    String getEndDate();

    boolean shallOutputBinnedData();

    int getSuperSampling();

    int getNumRows();
}
