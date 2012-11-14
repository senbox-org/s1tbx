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

package org.esa.beam.framework.gpf.graph;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.esa.beam.framework.gpf.internal.OperatorConfiguration;
import org.esa.beam.framework.gpf.internal.OperatorContext;

import javax.media.jai.PlanarImage;
import java.lang.reflect.Field;

/**
 * @since Public since BEAM 4.10.3.
 */
public class NodeContext {

    private final GraphContext graphContext;
    private final Node node;
    private Operator operator;
    private OperatorContext operatorContext;
    private int referenceCount;
    private Product targetProduct;

    NodeContext(GraphContext graphContext, Node node) throws GraphException {
        this.graphContext = graphContext;
        this.node = node;
        initOperator();
        initOperatorContext();
    }

    public GraphContext getGraphContext() {
        return graphContext;
    }

    public Node getNode() {
        return node;
    }

    public Operator getOperator() {
        return operator;
    }

    public boolean isOutput() {
        return referenceCount == 0;
    }

    void incrementReferenceCount() {
        this.referenceCount++;
    }

    public void initTargetProduct() throws GraphException {
        try {
            targetProduct = operator.getTargetProduct();
        } catch (OperatorException e) {
            throw new GraphException(e.getMessage(), e);
        }
    }

    public Product getTargetProduct() {
        Assert.notNull(targetProduct, "targetProduct");
        return targetProduct;
    }

    PlanarImage getTargetImage(Band band) {
        return operatorContext.getTargetImage(band);
    }

    public boolean canComputeTileStack() {
        return operatorContext.isComputeTileStackMethodUsable();
    }

    public boolean isInitialized() {
        return operatorContext.isInitialized();
    }

    public void addSourceProduct(String id, Product sourceProduct) {
        operator.setSourceProduct(id, sourceProduct);
    }

    public void setOperatorConfiguration(OperatorConfiguration opConfiguration) {
        operatorContext.setConfiguration(opConfiguration);
    }

    public Product getSourceProduct(String id) {
        return operator.getSourceProduct(id);
    }

    public Product[] getSourceProducts() {
        return operator.getSourceProducts();
    }

    private void initOperator() throws GraphException {
        final OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        OperatorSpi operatorSpi = spiRegistry.getOperatorSpi(node.getOperatorName());
        if (operatorSpi == null) {
            throw new GraphException("SPI not found for operator '" + node.getOperatorName() + "'");
        }

        try {
            this.operator = operatorSpi.createOperator();
            this.operator.setLogger(graphContext.getLogger());
            // this.operator.setConfiguration(node.getConfiguration());
        } catch (OperatorException e) {
            throw new GraphException("Failed to create instance of operator '" + node.getOperatorName() + "'");
        }
    }

    private void initOperatorContext() {
        try {
            Field field = Operator.class.getDeclaredField("context");
            field.setAccessible(true);
            operatorContext = (OperatorContext) field.get(operator);
            operatorContext.setId(node.getId());
            field.setAccessible(false);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    public synchronized void dispose() {
        if (operatorContext != null && !operatorContext.isDisposed()) {
            operatorContext.dispose(); // disposes operator as well
            operatorContext = null;
            operator = null;
        }
        if (targetProduct != null) {
            targetProduct.dispose();
            targetProduct = null;
        }
    }
}
