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

package org.esa.beam.framework.dataop.barithm;

import com.bc.jexp.impl.AbstractFunction;
import com.bc.jexp.impl.AbstractSymbol;
import com.bc.jexp.impl.SymbolFactory;
import com.bc.jexp.EvalEnv;
import com.bc.jexp.Symbol;
import com.bc.jexp.Term;
import com.bc.jexp.EvalException;
import com.bc.jexp.WritableNamespace;

import java.util.Random;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.Band;

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
 *<pre>
 * @author Norman Fomferra
 */
class MoreFuncs {

    private final static Random RANDOM = new Random();

    public static void registerExtraFunctions() {
        BandArithmetic.registerFunction(new AbstractFunction.D("random_gaussian", 0) {
            public double evalD(EvalEnv env, Term[] args) throws EvalException {
                return MoreFuncs.RANDOM.nextGaussian();
            }
        });

        BandArithmetic.registerFunction(new AbstractFunction.D("random_uniform", 0) {
            public double evalD(EvalEnv env, Term[] args) throws EvalException {
                return MoreFuncs.RANDOM.nextDouble();
            }
        });

        BandArithmetic.registerFunction(new AbstractFunction.D("sinh", 1) {
            public double evalD(EvalEnv env, Term[] args) throws EvalException {
                return MoreFuncs.sinh(args[0].evalD(env));
            }
        });

        BandArithmetic.registerFunction(new AbstractFunction.D("cosh", 1) {
            public double evalD(EvalEnv env, Term[] args) throws EvalException {
                return MoreFuncs.cosh(args[0].evalD(env));
            }
        });

        BandArithmetic.registerFunction(new AbstractFunction.D("tanh", 1) {
            public double evalD(EvalEnv env, Term[] args) throws EvalException {
                return MoreFuncs.tanh(args[0].evalD(env));
            }
        });

        BandArithmetic.registerFunction(new AbstractFunction.D("sech", 1) {
            public double evalD(EvalEnv env, Term[] args) throws EvalException {
                return MoreFuncs.sech(args[0].evalD(env));
            }
        });

        BandArithmetic.registerFunction(new AbstractFunction.D("cosech", 1) {
            public double evalD(EvalEnv env, Term[] args) throws EvalException {
                return MoreFuncs.cosech(args[0].evalD(env));
            }
        });
    }

    public static void registerExtraSymbols() {
        BandArithmetic.addNamespaceExtender(new BandArithmetic.NamespaceExtender() {
            public void extendNamespace(WritableNamespace namespace, Product product, String namePrefix) {
                final int numBands = product.getNumBands();
                for (int i = 0; i < numBands; i++) {
                    final Band band = product.getBandAt(i);
                    MoreFuncs.registerBandProperties(namespace, band);
                }
            }
        });
        
        BandArithmetic.addNamespaceExtender(new BandArithmetic.NamespaceExtender() {
            public void extendNamespace(WritableNamespace namespace, Product product, String namePrefix) {
                final WeakReference<GeoCoding> geocodingRef = new WeakReference<GeoCoding>(product.getGeoCoding());
                final Symbol lat = new AbstractSymbol.D("LAT") {
                    public double evalD(EvalEnv env) throws EvalException {
                    	double latitude = Double.NaN;
                    	GeoCoding geoCoding = geocodingRef.get();
                        if (geoCoding != null && geoCoding.canGetGeoPos()) {
                    		GeoPos geoPos = getGeoPos(geoCoding, env);
                    		if (geoPos.isValid()) {
                    			latitude =  geoPos.getLat();
                    		}
                    	}
                    	return latitude;
                    }
                };
                final Symbol lon = new AbstractSymbol.D("LON") {
                    public double evalD(EvalEnv env) throws EvalException {
                    	double longitude = Double.NaN;
                    	GeoCoding geoCoding = geocodingRef.get();
                        if (geoCoding != null && geoCoding.canGetGeoPos()) {
                    		GeoPos geoPos = getGeoPos(geoCoding, env);
                    		if (geoPos.isValid()) {
                    			longitude =  geoPos.getLon();
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
    
    private static GeoPos getGeoPos(final GeoCoding geoCoding, EvalEnv env) {
    	RasterDataEvalEnv rasterEnv = (RasterDataEvalEnv) env;
    	PixelPos pixelPos = new PixelPos(rasterEnv.getPixelX(), rasterEnv.getPixelY());
    	GeoPos geoPos = geoCoding.getGeoPos(pixelPos, null);
		return geoPos;
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
                } catch (Throwable e) {
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
        if (getterType.equals(Double.TYPE) ||
            getterType.equals(Float.TYPE)) {
            namespace.registerSymbol(SymbolFactory.createConstant(symbolName, ((Number) propertyValue).doubleValue()));
        } else if (getterType.equals(Byte.TYPE) ||
                   getterType.equals(Short.TYPE) ||
                   getterType.equals(Integer.TYPE) ||
                   getterType.equals(Long.TYPE)) {
            namespace.registerSymbol(SymbolFactory.createConstant(symbolName, ((Number) propertyValue).intValue()));
        } else if (getterType.equals(Boolean.TYPE)) {
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
        StringBuffer sb = new StringBuffer();
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

    private static double sinh(final double x) {
        return 0.5 * (Math.exp(x) - Math.exp(-x));
    }

    private static double cosh(final double x) {
        return 0.5 * (Math.exp(x) + Math.exp(-x));
    }

    private static double tanh(final double x) {
        final double a = Math.exp(x);
        final double b = Math.exp(-x);
        return (a - b) / (a + b);
    }

    private static double sech(final double x) {
        return 2.0 / (Math.exp(x) + Math.exp(-x));
    }

    private static double cosech(final double x) {
        return 2.0 / (Math.exp(x) - Math.exp(-x));
    }
}
