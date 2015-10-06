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

package org.esa.snap.core.jexp.impl;

import org.esa.snap.core.jexp.EvalEnv;
import org.esa.snap.core.jexp.Function;
import org.esa.snap.core.jexp.Namespace;
import org.esa.snap.core.jexp.Symbol;
import org.esa.snap.core.jexp.Term;

/**
 * A default namespace which includes the constants PI, E and NaN as well as most of the functions from
 * Java {@link Math}, e.g.
 * sin(x),
 * cos(x),
 * tan(x),
 * asin(x),
 * acos(x),
 * atan(x),
 * atan2(x,y),
 * log(x),
 * exp(x),
 * pow(x,y),
 * sqrt(x,y),
 * abs(x),
 * min(x,y),
 * max(x,y),
 * floor(x),
 * rad(x) ({@link Math#toRadians(double)}),
 * deg(x,y) ({@link Math#toDegrees(double)}).
 * inf(x,y) ({@link Double#isInfinite(double)}),
 * nan(x,y) ({@link Double#isNaN(double)}).
 * <p>The following functions are not taken from Java Java {@link Math}:
 * log10(x),
 * exp10(x),
 * sign(x).
 * <p>It also provides the fuzzy comparison functions
 * feq(x, y),
 * fneq(x).
 *
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public final class DefaultNamespace extends NamespaceImpl {


    public DefaultNamespace() {
        this(null);
    }

    public DefaultNamespace(Namespace parent) {
        super(parent);
        registerDefaultSymbols();
        registerDefaultFunctions();
    }

    private void registerDefaultSymbols() {
        for (Symbol symbol : Symbols.getAll()) {
            registerSymbol(symbol);
        }
    }

    private void registerDefaultFunctions() {
        for (Function function : Functions.getAll()) {
            registerFunction(function);
        }

        registerFunction(new AbstractFunction.D("distance_deriv", -1) {

            public double evalD(final EvalEnv env, final Term[] args) {
                double sqrSum = 0.0;
                final int n = args.length / 2;
                for (int i = 0; i < n - 1; i++) {
                    final double v1 = args[i + 1].evalD(env) - args[i].evalD(env);
                    final double v2 = args[i + n + 1].evalD(env) - args[i + n].evalD(env);
                    sqrSum += (v1 - v2) * (v1 - v2);
                }
                return Math.sqrt(sqrSum);
            }
        });

        registerFunction(new AbstractFunction.D("distance_integ", -1) {

            public double evalD(final EvalEnv env, final Term[] args) {
                double sqrSum = 0.0;
                double v1Sum = 0.0;
                double v2Sum = 0.0;
                final int n = args.length / 2;
                for (int i = 0; i < n; i++) {
                    v1Sum += args[i].evalD(env);
                    v2Sum += args[i + n].evalD(env);
                    sqrSum += (v2Sum - v1Sum) * (v2Sum - v1Sum);
                }
                return Math.sqrt(sqrSum);
            }
        });

        registerFunction(new AbstractFunction.B("inrange", -1) {

            public boolean evalB(final EvalEnv env, final Term[] args) {
                final int n1 = args.length / 3;
                final int n2 = n1 + args.length / 3;
                for (int i = 0; i < n1; i++) {
                    final double v = args[i].evalD(env);
                    final double v1 = args[i + n1].evalD(env);
                    final double v2 = args[i + n2].evalD(env);
                    if (v < v1 || v > v2) {
                        return false;
                    }
                }
                return true;
            }
        });

        registerFunction(new AbstractFunction.B("inrange_deriv", -1) {

            public boolean evalB(final EvalEnv env, final Term[] args) {
                final int n1 = args.length / 3;
                final int n2 = 2 * n1;
                for (int i = 0; i < n1 - 1; i++) {
                    final double v = args[i + 1].evalD(env) - args[i].evalD(env);
                    final double v1 = args[i + n1 + 1].evalD(env) - args[i + n1].evalD(env);
                    final double v2 = args[i + n2 + 1].evalD(env) - args[i + n2].evalD(env);
                    if (v < v1 || v > v2) {
                        return false;
                    }
                }
                return true;
            }
        });

        registerFunction(new AbstractFunction.B("inrange_integ", -1) {

            public boolean evalB(final EvalEnv env, final Term[] args) {
                final int n1 = args.length / 3;
                final int n2 = 2 * n1;
                double vSum = 0.0;
                double v1Sum = 0.0;
                double v2Sum = 0.0;
                for (int i = 0; i < n1; i++) {
                    vSum += args[i].evalD(env);
                    v1Sum += args[i + n1].evalD(env);
                    v2Sum += args[i + n2].evalD(env);
                    if (vSum < v1Sum || vSum > v2Sum) {
                        return false;
                    }
                }
                return true;
            }
        });
    }

}
