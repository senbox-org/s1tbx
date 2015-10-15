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

package org.esa.snap.core.gpf.common;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.internal.JaiHelper;

import java.awt.RenderingHints;
import java.text.MessageFormat;
import java.util.HashMap;


@OperatorMetadata(alias = "JAI",
                  description = "Performs a JAI (Java Advanced Imaging) operation on bands of a data product.",
                  internal = true)
public class JaiOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @Parameter
    private String[] bandNames;

    @Parameter
    private String operationName;

    //@Parameter
    private HashMap<String, Object> operationParameters;

    //@Parameter
    private RenderingHints renderingHints;


    public JaiOp() {
    }

    public JaiOp(Product sourceProduct,
                 String operationName,
                 HashMap<String, Object> operationParameters,
                 RenderingHints renderingHints) {
        this.sourceProduct = sourceProduct;
        this.operationName = operationName;
        this.operationParameters = operationParameters;
        this.renderingHints = renderingHints;
    }

    @Override
    public void initialize() throws OperatorException {
        Product targetProduct = JaiHelper.createTargetProduct(sourceProduct,
                                                              bandNames,
                                                              operationName,
                                                              operationParameters,
                                                              renderingHints);
        setTargetProduct(targetProduct);
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public HashMap<String, Object> getOperationParameters() {
        return operationParameters;
    }

    public void setOperationParameters(HashMap<String, Object> operationParameters) {
        this.operationParameters = operationParameters;
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        throw new IllegalStateException(MessageFormat.format("Operator ''{0}'' cannot compute tiles on its own.", getClass().getName()));
    }
}
