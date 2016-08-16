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

package org.esa.snap.core.dataop.barithm;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.jexp.EvalEnv;
import org.esa.snap.core.jexp.EvalException;
import org.esa.snap.core.jexp.Symbol;
import org.esa.snap.core.jexp.WritableNamespace;
import org.esa.snap.core.jexp.impl.AbstractSymbol;
import org.esa.snap.core.jexp.impl.SymbolFactory;
import org.esa.snap.core.util.ProductUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ServiceLoader;

/**
 * SNAP's standard product {@link WritableNamespace namespace} extender.
 * Populates a given namespace for band arithmetic expression with symbols derived from product components.
 *
 * @author Norman Fomferra
 */
class ProductNamespaceExtenderImpl implements ProductNamespaceExtender {

    public static final String PIXEL_X_SYMBOL = "X";
    public static final String PIXEL_Y_SYMBOL = "Y";
    public static final String PIXEL_LAT_SYMBOL = "LAT";
    public static final String PIXEL_LON_SYMBOL = "LON";
    public static final String PIXEL_TIME_SYMBOL = "TIME";

    /**
     * Names of product-level symbols available in all product types.
     */
    public static final String[] COMMON_PRODUCT_SYMBOLS = {
            PIXEL_X_SYMBOL,
            PIXEL_Y_SYMBOL,
            PIXEL_LAT_SYMBOL,
            PIXEL_LON_SYMBOL,
            PIXEL_TIME_SYMBOL,
    };

    public static final GeoPos INVALID_GEO_POS = new GeoPos(Float.NaN, Float.NaN);

    @Override
    public void extendNamespace(Product product, String namePrefix, WritableNamespace namespace) {
        registerPixelSymbols(product, namePrefix, namespace);
        registerTiePointGridSymbols(product, namePrefix, namespace);
        registerBandSymbols(product, namePrefix, namespace);
        registerMaskSymbols(product, namePrefix, namespace);
        registerSingleFlagSymbols(product, namePrefix, namespace);
        registerBandProperties(product, namePrefix, namespace);
        callServiceProviders(product, namePrefix, namespace);
    }

    private static void registerTiePointGridSymbols(Product product, String namePrefix, WritableNamespace namespace) {
        for (int i = 0; i < product.getNumTiePointGrids(); i++) {
            final TiePointGrid grid = product.getTiePointGridAt(i);
            final String symbolName = namePrefix + grid.getName();
            namespace.registerSymbol(new RasterDataSymbol(symbolName, grid, RasterDataSymbol.GEOPHYSICAL));
        }
    }

    private static void registerBandSymbols(Product product, String namePrefix, WritableNamespace namespace) {
        for (int i = 0; i < product.getNumBands(); i++) {
            final Band band = product.getBandAt(i);
            final String symbolName = namePrefix + band.getName();
            namespace.registerSymbol(new RasterDataSymbol(symbolName, band, RasterDataSymbol.GEOPHYSICAL));
            namespace.registerSymbol(new RasterDataSymbol(symbolName + ".raw", band, RasterDataSymbol.RAW));
        }
    }

    private static void registerMaskSymbols(Product product, String namePrefix, WritableNamespace namespace) {
        for (int i = 0; i < product.getMaskGroup().getNodeCount(); i++) {
            final Mask mask = product.getMaskGroup().get(i);
            final String symbolName = namePrefix + mask.getName();
            namespace.registerSymbol(new RasterDataSymbol(symbolName, mask));
        }
    }

    private static void registerSingleFlagSymbols(Product product, String namePrefix, WritableNamespace namespace) {
        for (int i = 0; i < product.getNumBands(); i++) {
            final Band band = product.getBandAt(i);
            if (band.getFlagCoding() != null) {
                for (int j = 0; j < band.getFlagCoding().getNumAttributes(); j++) {
                    final MetadataAttribute attribute = band.getFlagCoding().getAttributeAt(j);
                    final ProductData flagData = attribute.getData();
                    final int flagMask;
                    final int flagValue;
                    if (flagData.getNumElems() == 2) {
                        flagMask = flagData.getElemIntAt(0);
                        flagValue = flagData.getElemIntAt(1);
                    } else {
                        flagMask = flagValue = flagData.getElemInt();
                    }
                    final String symbolName = namePrefix + band.getName() + "." + attribute.getName();
                    final Symbol symbol = new SingleFlagSymbol(symbolName, band, flagMask, flagValue);
                    namespace.registerSymbol(symbol);
                }
            }
        }
    }

    private static void registerBandProperties(Product product, String namePrefix, WritableNamespace namespace) {
        int numBands = product.getNumBands();
        for (int i = 0; i < numBands; i++) {
            final Band band = product.getBandAt(i);
            registerBandProperties(namespace, band, namePrefix);
        }
    }

    private static void registerPixelSymbols(Product product, String namePrefix, WritableNamespace namespace) {
        int width = product.getSceneRasterWidth();
        int height = product.getSceneRasterHeight();

        namespace.registerSymbol(new PixelXSymbol(namePrefix + PIXEL_X_SYMBOL));
        namespace.registerSymbol(new PixelYSymbol(namePrefix + PIXEL_Y_SYMBOL));

        namespace.registerSymbol(new PixelLatSymbol(namePrefix + PIXEL_LAT_SYMBOL, product.getSceneGeoCoding(), width, height));
        namespace.registerSymbol(new PixelLonSymbol(namePrefix + PIXEL_LON_SYMBOL, product.getSceneGeoCoding(), width, height));

        namespace.registerSymbol(new PixelTimeSymbol(namePrefix + PIXEL_TIME_SYMBOL, product));
        namespace.registerSymbol(new PixelTimeSymbol(namePrefix + "MJD", product)); // For compatibility with SNAP 1.0 only
    }

    private static void registerBandProperties(WritableNamespace namespace, final Band band, String namePrefix) {
        final Class bandClass = band.getClass();
        final Method[] declaredMethods = bandClass.getDeclaredMethods();
        for (Method method : declaredMethods) {
            final String methodName = method.getName();
            final Class methodType = method.getReturnType();
            if (methodType.isPrimitive() &&
                    ProductNamespaceExtenderImpl.hasGetterPrefix(methodName) &&
                    method.getParameterTypes().length == 0) {
                Object propertyValue = null;
                try {
                    propertyValue = method.invoke(band, (Object[]) null);
                } catch (Throwable ignored) {
                }
                if (propertyValue != null) {
                    final String propertyName = ProductNamespaceExtenderImpl.convertMethodNameToPropertyName(methodName);
                    final String symbolName = band.getName() + "." + propertyName;
                    ProductNamespaceExtenderImpl.registerPropertyConstant(namespace, namePrefix + symbolName, propertyValue);
                }
            }
        }
    }

    private static void registerPropertyConstant(WritableNamespace namespace, final String symbolName, Object propertyValue) {
        final Class getterType = propertyValue.getClass();
        if (getterType.equals(Double.class) ||
                getterType.equals(Float.class)) {
            namespace.registerSymbol(SymbolFactory.createConstant(symbolName, ((Number) propertyValue).doubleValue()));
        } else if (getterType.equals(Byte.class) ||
                getterType.equals(Short.class) ||
                getterType.equals(Integer.class) ||
                getterType.equals(Long.class)) {
            namespace.registerSymbol(SymbolFactory.createConstant(symbolName, ((Number) propertyValue).intValue()));
        } else if (getterType.equals(Boolean.class)) {
            namespace.registerSymbol(
                    SymbolFactory.createConstant(symbolName, (Boolean) propertyValue));
        }
    }

    private static void callServiceProviders(Product product, String namePrefix, WritableNamespace namespace) {
        ServiceLoader<ProductNamespaceExtender> namespaceExtenders = ServiceLoader.load(ProductNamespaceExtender.class);
        for (ProductNamespaceExtender namespaceExtender : namespaceExtenders) {
            namespaceExtender.extendNamespace(product, namePrefix, namespace);
        }
    }

    private static GeoPos getGeoPos(final GeoCoding geoCoding, EvalEnv env, int width, int height) {
        RasterDataEvalEnv rasterEnv = (RasterDataEvalEnv) env;
        int pixelX = rasterEnv.getPixelX();
        int pixelY = rasterEnv.getPixelY();
        if (pixelX >= 0 && pixelX < width && pixelY >= 0 && pixelY < height) {
            return geoCoding.getGeoPos(new PixelPos(pixelX + 0.5f, pixelY + 0.5f), null);
        }
        return INVALID_GEO_POS;
    }

    private static String convertMethodNameToPropertyName(String s) {
        int skipCount = 0;
        if (s.startsWith("is")) {
            skipCount = 2;
        } else if (s.startsWith("get")) {
            skipCount = 3;
        }
        StringBuilder sb = new StringBuilder();
        final int n = s.length();
        for (int i = 0; i < n; i++) {
            if (i >= skipCount) {
                if (i < n - 1) {
                    final char c1 = s.charAt(i);
                    final char c2 = s.charAt(i + 1);
                    sb.append(Character.toLowerCase(c1));
                    if (Character.isLowerCase(c1) && Character.isUpperCase(c2)) {
                        sb.append('_');
                    }
                } else {
                    sb.append(Character.toLowerCase(s.charAt(i)));
                }
            }
        }
        return sb.toString();
    }

    private static boolean hasGetterPrefix(final String methodName) {
        return (ProductNamespaceExtenderImpl.hasPrefix(methodName, "is") || ProductNamespaceExtenderImpl.hasPrefix(methodName, "get"));
    }

    private static boolean hasPrefix(final String methodName, final String prefix) {
        return methodName.startsWith(prefix) && methodName.length() > prefix.length();
    }

    static final class PixelXSymbol extends AbstractSymbol.D {
        public PixelXSymbol(String name) {
            super(name);
        }

        @Override
        public double evalD(EvalEnv env) throws EvalException {
            return ((RasterDataEvalEnv) env).getPixelX() + 0.5;
        }
    }

    static final class PixelYSymbol extends AbstractSymbol.D {
        public PixelYSymbol(String name) {
            super(name);
        }

        @Override
        public double evalD(EvalEnv env) throws EvalException {
            return ((RasterDataEvalEnv) env).getPixelY() + 0.5;
        }
    }

    ;

    static final class PixelTimeSymbol extends AbstractSymbol.D {
        private final WeakReference<Product> productRef;

        PixelTimeSymbol(String name, Product product) {
            super(name);
            this.productRef = new WeakReference<>(product);
        }

        @Override
        public double evalD(EvalEnv env) throws EvalException {
            Product product = productRef.get();
            if (product != null) {
                int pixelY = ((RasterDataEvalEnv) env).getPixelY();
                ProductData.UTC scanLineTime = ProductUtils.getScanLineTime(product, pixelY);
                if (scanLineTime != null) {
                    return scanLineTime.getMJD();
                }
            }
            return Double.NaN;
        }
    }

    static abstract class PixelGeoPosSymbol extends AbstractSymbol.D {
        private final WeakReference<GeoCoding> geocodingRef;
        private final int width;
        private final int height;

        protected PixelGeoPosSymbol(String name, GeoCoding geocoding, int width, int height) {
            super(name);
            this.geocodingRef = new WeakReference<>(geocoding);
            this.width = width;
            this.height = height;
        }

        @Override
        public double evalD(EvalEnv env) throws EvalException {
            double longitude = Double.NaN;
            GeoCoding geoCoding = geocodingRef.get();
            if (geoCoding != null && geoCoding.canGetGeoPos()) {
                GeoPos geoPos = getGeoPos(geoCoding, env, width, height);
                if (geoPos.isValid()) {
                    longitude = getCoord(geoPos);
                }
            }
            return longitude;
        }

        protected abstract double getCoord(GeoPos geoPos);
    }

    static final class PixelLonSymbol extends PixelGeoPosSymbol {

        PixelLonSymbol(String name, GeoCoding geocoding, int width, int height) {
            super(name, geocoding, width, height);
        }

        @Override
        protected double getCoord(GeoPos geoPos) {
            return geoPos.lon;
        }
    }

    static final class PixelLatSymbol extends PixelGeoPosSymbol {

        PixelLatSymbol(String name, GeoCoding geocoding, int width, int height) {
            super(name, geocoding, width, height);
        }

        @Override
        protected double getCoord(GeoPos geoPos) {
            return geoPos.lat;
        }
    }
}
