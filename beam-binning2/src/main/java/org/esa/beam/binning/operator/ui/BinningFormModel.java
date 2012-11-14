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

    static String PROPERTY_KEY_SOURCE_PRODUCTS = "sourceProducts";
    static String PROPERTY_KEY_VARIABLE_CONFIGS = "variableConfigs";
    static String PROPERTY_KEY_REGION = "region";
    static String PROPERTY_KEY_COMPUTE_REGION = "compute";
    static String PROPERTY_KEY_GLOBAL = "global";
    static String PROPERTY_KEY_EXPRESSION = "expression";
    static String PROPERTY_KEY_TEMPORAL_FILTER = "temporalFilter";
    static String PROPERTY_KEY_START_DATE = "startDate";
    static String PROPERTY_KEY_END_DATE = "endDate";
    static String PROPERTY_KEY_OUTPUT_BINNED_DATA = "outputBinnedData";
    static String PROPERTY_KEY_TARGET_HEIGHT = "targetHeight";
    static String PROPERTY_KEY_SUPERSAMPLING = "supersampling";
    static String PROPERTY_KEY_MANUAL_WKT = "manualWktKey";

    static int DEFAULT_NUM_ROWS = 2160;

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
