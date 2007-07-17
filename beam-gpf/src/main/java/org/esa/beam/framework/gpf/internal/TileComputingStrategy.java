package org.esa.beam.framework.gpf.internal;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.AbstractOperator;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Raster;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.TileCache;

import java.awt.Rectangle;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

public class TileComputingStrategy {

    private static HashMap<Operator, ImplementationInfo> implementationMap;

    private TileComputingStrategy() {
    }

    public static void computeBand(OperatorComputationContext operatorContext, RasterDataNode targetRasterDataNode,
                                   Rectangle targetRectangle, ProductData targetBuffer, ProgressMonitor pm) throws
                                                                                                            OperatorException {
        Operator operator = operatorContext.getOperator();
        ImplementationInfo implementationInfo = getImplementationInfo(operator);
        TileCache tileCache = GPF.getDefaultInstance().getTileCache();
        Tile tile = tileCache.getTile(targetRasterDataNode, targetRectangle);
        if (tile != null && tile.getState() == Tile.State.COMPUTED) {
            return; // tile is already computed, we can return immediately
        }

        pm.beginTask("Computing raster...", 20);
        try {
            Raster targetRaster = operatorContext.getRaster(targetRasterDataNode, targetRectangle, targetBuffer,
                                                            new SubProgressMonitor(pm, 10));

            if (implementationInfo.isBandMethodImplemented()) {
                // Prefer a provided implementation of Operator.computeBand()
                if (mustCompute(targetRectangle, targetRasterDataNode)) {
                    operator.computeBand(targetRaster, new SubProgressMonitor(pm, 10));
                }
            } else {
                // No other choice, we have to use Operator.computeAllBands()
                if (mustCompute(targetRectangle, operatorContext.getTargetProduct().getBands())) {
                    operator.computeAllBands(targetRectangle, new SubProgressMonitor(pm, 10));
                }
            }
        } finally {
            pm.done();
        }
        setTargetTilesToComputedState(operatorContext);
    }

    public static void computeAllBands(OperatorComputationContext operatorContext, Rectangle targetRectangle,
                                       ProgressMonitor pm) throws OperatorException {
        Operator operator = operatorContext.getOperator();
        ImplementationInfo implementationInfo = getImplementationInfo(operator);
        TileCache tileCache = GPF.getDefaultInstance().getTileCache();
        Product targetProduct = operatorContext.getTargetProduct();
        if (implementationInfo.isAllBandsMethodImplemented()) {
            // Prefer a provided implementation of Operator.computeAllBands()
            if (mustCompute(targetRectangle, targetProduct.getBands())) {
                operator.computeAllBands(targetRectangle, pm);
            }
        } else {
            // No other choice, we have to use Operator.computeBand() separately for each band
            pm.beginTask("Computing bands...", targetProduct.getBands().length);
            try {
                for (Band band : targetProduct.getBands()) {
                    if (mustCompute(targetRectangle, band)) {
                        Tile tile = tileCache.createTile(band, targetRectangle, null);
                        operator.computeBand(tile.getRaster(), new SubProgressMonitor(pm, 1));
                    } else {
                        // todo - find a clean solution for operators where target and source product are the same
                        for (Product sourceProduct : operatorContext.getSourceProducts()) {
                            if (sourceProduct == targetProduct) {
                                Tile tile = tileCache.getTile(band, targetRectangle);
                                assert(tile != null);
                                operator.computeBand(tile.getRaster(), new SubProgressMonitor(pm, 1));
                                break;
                            }
                        }
                    }
                }
            } finally {
                pm.done();
            }
        }

        setTargetTilesToComputedState(operatorContext);
    }

    private static boolean mustCompute(Rectangle targetRectangle, RasterDataNode ... rasterDataNodes) {
        TileCache tileCache = GPF.getDefaultInstance().getTileCache();
        for (RasterDataNode band : rasterDataNodes) {
            Tile tile = tileCache.getTile(band, targetRectangle);
            if (tile == null || tile.getState() != Tile.State.COMPUTED) {
                return true;
            }
        }
        return false;
    }

    private static void setTargetTilesToComputedState(OperatorComputationContext operatorContext) {
        Product product = operatorContext.getTargetProduct();
        TileCache tileCache = GPF.getDefaultInstance().getTileCache();
        for (Band band : product.getBands()) {
            Tile[] tiles = tileCache.getTiles(band);
            for (Tile tile : tiles) {
                if (tile.getState() == Tile.State.COMPUTING) {
                    tile.setState(Tile.State.COMPUTED);
                }
            }
        }
    }

    private static ImplementationInfo getImplementationInfo(Operator operator) {
        if (implementationMap == null) {
            implementationMap = new HashMap<Operator, ImplementationInfo>(10);
        }
        if (!implementationMap.containsKey(operator)) {
            implementationMap.put(operator, new ImplementationInfo(operator.getClass()));
        }
        return implementationMap.get(operator);
    }


    static class ImplementationInfo {

        private final boolean bandMethodImplemented;
        private final boolean tilesMethodImplemented;

        public ImplementationInfo(Class operatorClass) {
            bandMethodImplemented = implementsComputeBandMethod(operatorClass);
            tilesMethodImplemented = implementsComputeAllBandsMethod(operatorClass);
        }

        public boolean isBandMethodImplemented() {
            return bandMethodImplemented;
        }

        public boolean isAllBandsMethodImplemented() {
            return tilesMethodImplemented;
        }

        public static boolean implementsComputeBandMethod(Class<?> aClass) {
            return implementsMethod(aClass, "computeBand",
                                    new Class[]{
                                            Raster.class,
                                            ProgressMonitor.class});
        }

        public static boolean implementsComputeAllBandsMethod(Class<?> aClass) {
            return implementsMethod(aClass, "computeAllBands",
                                    new Class[]{
                                            Rectangle.class,
                                            ProgressMonitor.class});
        }

        private static boolean implementsMethod(Class<?> aClass, String methodName, Class[] methodParameterTypes) {
            if (Operator.class.equals(aClass)
                || AbstractOperator.class.equals(aClass)
                || !Operator.class.isAssignableFrom(aClass)) {
                return false;
            }
            try {
                Method declaredMethod = aClass.getDeclaredMethod(methodName, methodParameterTypes);
                return declaredMethod.getModifiers() != Modifier.ABSTRACT;
            } catch (NoSuchMethodException e) {
                Class<?> superclass = aClass.getSuperclass();
                return implementsMethod(superclass, methodName, methodParameterTypes);
            }
        }

    }
}
