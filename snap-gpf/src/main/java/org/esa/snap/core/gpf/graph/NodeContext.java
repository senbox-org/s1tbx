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

package org.esa.snap.core.gpf.graph;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.core.gpf.internal.OperatorConfiguration;
import org.esa.snap.core.gpf.internal.OperatorContext;
import org.esa.snap.runtime.Config;

import javax.media.jai.PlanarImage;
import java.io.File;
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
            throw new GraphException("[NodeId: " + node.getId() + "] " + e.getMessage(), e);
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
        return operatorContext.getOperator().canComputeTileStack();
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
            String msg = Config.instance().preferences().get("snap.gpf.unsupported." + node.getOperatorName(), null);
            if (msg == null) {
                msg = "SPI not found for operator '" + node.getOperatorName() + "'";
            }
            throw new GraphException(msg);
        }

        try {
            this.operator = operatorSpi.createOperator();
            this.operator.setLogger(graphContext.getLogger());
            // this.operator.setConfiguration(node.getConfiguration());
        } catch (OperatorException e) {
            throw new GraphException("Failed to create instance of operator '" + node.getOperatorName() + "'", e);
        }
    }

    private void initOperatorContext() {
        try {
            Field field = Operator.class.getDeclaredField("context");
            field.setAccessible(true);
            operatorContext = (OperatorContext) field.get(operator);
            operatorContext.setId(node.getId());
            field.setAccessible(false);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean isProductOpened(ProductManager productManager, Product targetProduct) {
        if (productManager.contains(targetProduct)) {
            return true;
        }
        final File file = targetProduct.getFileLocation();
        if (file == null) {
            return false;
        }

        final Product[] openedProducts = productManager.getProducts();
        for (Product openedProduct : openedProducts) {
            if (file.equals(openedProduct.getFileLocation())) {
                return true;
            }
        }
        return false;
    }

    public synchronized void dispose() {
        if (targetProduct != null) {
            if (!(operator != null && isProductOpened(operator.getProductManager(), targetProduct))) {
                targetProduct.dispose();
                targetProduct = null;
            }
        }
        if (operatorContext != null && !operatorContext.isDisposed()) {
            operatorContext.dispose(); // disposes operator as well
            operatorContext = null;
            operator = null;
        }
    }
}
