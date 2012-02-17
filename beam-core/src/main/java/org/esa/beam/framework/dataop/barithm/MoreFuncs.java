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

package org.esa.beam.framework.dataop.barithm;

import com.bc.jexp.*;
import com.bc.jexp.impl.AbstractFunction;
import com.bc.jexp.impl.AbstractSymbol;
import com.bc.jexp.impl.SymbolFactory;
import org.esa.beam.framework.datamodel.*;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Random;

/**
 * This class adds the following functions to VISAT's expression editor:
 * <pre>
 *     random_gaussian
 *     random_uniform
 *     sinh
 *     cosh
 *     tanh
 *     sech
 *     cosech
 *     log10
 * <pre>
 *
 * @author Norman Fomferra
 */
class MoreFuncs {

    private static final Random RANDOM = new Random();
    public static final GeoPos INVALID_GEO_POS = new GeoPos(Float.NaN, Float.NaN);

    private MoreFuncs() {
    }

    public static void registerExtraFunctions() {
        BandArithmetic.registerFunction(new AbstractFunction.D("random_gaussian", 0) {
            @Override
            public double evalD(EvalEnv env, Term[] args) throws EvalException {
                return MoreFuncs.RANDOM.nextGaussian();
            }
        });

        BandArithmetic.registerFunction(new AbstractFunction.D("random_uniform", 0) {
            @Override
            public double evalD(EvalEnv env, Term[] args) throws EvalException {
                return MoreFuncs.RANDOM.nextDouble();
            }
        });

        BandArithmetic.registerFunction(new AbstractFunction.D("sinh", 1) {
            @Override
            public double evalD(EvalEnv env, Term[] args) throws EvalException {
                return Math.sinh(args[0].evalD(env));
            }
        });

        BandArithmetic.registerFunction(new AbstractFunction.D("cosh", 1) {
            @Override
            public double evalD(EvalEnv env, Term[] args) throws EvalException {
                return Math.cosh(args[0].evalD(env));
            }
        });

        BandArithmetic.registerFunction(new AbstractFunction.D("tanh", 1) {
            @Override
            public double evalD(EvalEnv env, Term[] args) throws EvalException {
                return Math.tanh(args[0].evalD(env));
            }
        });

        BandArithmetic.registerFunction(new AbstractFunction.D("sech", 1) {
            @Override
            public double evalD(EvalEnv env, Term[] args) throws EvalException {
                return MoreFuncs.sech(args[0].evalD(env));
            }
        });

        BandArithmetic.registerFunction(new AbstractFunction.D("cosech", 1) {
            @Override
            public double evalD(EvalEnv env, Term[] args) throws EvalException {
                return MoreFuncs.cosech(args[0].evalD(env));
            }
        });
    }

    public static void registerExtraSymbols() {
        BandArithmetic.addNamespaceExtender(new BandArithmetic.NamespaceExtender() {
            @Override
            public void extendNamespace(WritableNamespace namespace, Product product, String namePrefix) {
                final int numBands = product.getNumBands();
                for (int i = 0; i < numBands; i++) {
                    final Band band = product.getBandAt(i);
                    MoreFuncs.registerBandProperties(namespace, band);
                }
            }
        });

        BandArithmetic.addNamespaceExtender(new BandArithmetic.NamespaceExtender() {
            @Override
            public void extendNamespace(final WritableNamespace namespace, final Product product, final String namePrefix) {
                final int width = product.getSceneRasterWidth();
                final int height = product.getSceneRasterHeight();
                final WeakReference<GeoCoding> geocodingRef = new WeakReference<GeoCoding>(product.getGeoCoding());
                final Symbol lat = new AbstractSymbol.D("LAT") {
                    @Override
                    public double evalD(EvalEnv env) throws EvalException {
                        double latitude = Double.NaN;
                        GeoCoding geoCoding = geocodingRef.get();
                        if (geoCoding != null && geoCoding.canGetGeoPos()) {
                            GeoPos geoPos = getGeoPos(geoCoding, env, width, height);
                            if (geoPos.isValid()) {
                                latitude = geoPos.getLat();
                            }
                        }
                        return latitude;
                    }
                };
                final Symbol lon = new AbstractSymbol.D("LON") {
                    @Override
                    public double evalD(EvalEnv env) throws EvalException {
                        double longitude = Double.NaN;
                        GeoCoding geoCoding = geocodingRef.get();
                        if (geoCoding != null && geoCoding.canGetGeoPos()) {
                            GeoPos geoPos = getGeoPos(geoCoding, env, width, height);
                            if (geoPos.isValid()) {
                                longitude = geoPos.getLon();
                            }
                        }
                        return longitude;
                    }
                };
                BandArithmetic.registerSymbol(lat);
                BandArithmetic.registerSymbol(lon);
            }
        });
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

    private static void registerBandProperties(WritableNamespace namespace, final Band band) {
        final Class bandClass = band.getClass();
        final Method[] declaredMethods = bandClass.getDeclaredMethods();
        for (Method method : declaredMethods) {
            final String methodName = method.getName();
            final Class methodType = method.getReturnType();
            if (methodType.isPrimitive() &&
                    MoreFuncs.hasGetterPrefix(methodName) &&
                    method.getParameterTypes().length == 0) {
                Object propertyValue = null;
                try {
                    propertyValue = method.invoke(band, (Object[]) null);
                } catch (Throwable ignored) {
                    // todo - handle exception!
                }
                if (propertyValue != null) {
                    final String propertyName = MoreFuncs.convertMethodNameToPropertyName(methodName);
                    final String symbolName = band.getName() + "." + propertyName;
                    MoreFuncs.registerConstant(namespace, symbolName, propertyValue);
                }
            }
        }
    }

    private static void registerConstant(WritableNamespace namespace, final String symbolName, Object propertyValue) {
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
        return (MoreFuncs.hasPrefix(methodName, "is") || MoreFuncs.hasPrefix(methodName, "get"));
    }

    private static boolean hasPrefix(final String methodName, final String prefix) {
        return methodName.startsWith(prefix) && methodName.length() > prefix.length();
    }

    private static double sech(final double x) {
        return 2.0 / (Math.exp(x) + Math.exp(-x));
    }

    private static double cosech(final double x) {
        return 2.0 / (Math.exp(x) - Math.exp(-x));
    }
}
