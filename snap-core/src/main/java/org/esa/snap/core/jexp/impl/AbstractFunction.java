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
import org.esa.snap.core.jexp.EvalException;
import org.esa.snap.core.jexp.Function;
import org.esa.snap.core.jexp.Term;


/**
 * Provides an implementation of the <code>{@link org.esa.snap.core.jexp.Function}</code> interface.
 *
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 */
public abstract class AbstractFunction implements Function {

    private final String name;
    private final int retType;
    private final int numArgs;
    private final int[] argTypes;

    protected AbstractFunction(final String name, final int retType, final int numArgs) {
        this(name, retType, numArgs, numArgs > 0 ? new int[numArgs] : null);
        if (argTypes != null) {
            for (int i = 0; i < argTypes.length; i++) {
                argTypes[i] = retType;
            }
        }
    }

    protected AbstractFunction(final String name, final int retType, final int numArgs, final int[] argTypes) {
        this.name = name.intern();
        this.retType = retType;
        this.numArgs = numArgs;
        this.argTypes = argTypes;
    }

    public String getName() {
        return name;
    }

    public int getRetType() {
        return retType;
    }

    public int getNumArgs() {
        return numArgs;
    }

    public int getArgType(int argIndex) {
        return argTypes != null ? argTypes[argIndex] : retType;
    }

    @Override
    public boolean isConst(Term[] args) {
        for (Term arg : args) {
            if (!arg.isConst()) {
                return false;
            }
        }
        return true;
    }

    public static abstract class B extends AbstractFunction {

        public B(final String name, final int numArgs) {
            super(name, Term.TYPE_B, numArgs);
        }

        public B(final String name, final int numArgs, final int[] argTypes) {
            super(name, Term.TYPE_B, numArgs, argTypes);
        }

        public int evalI(final EvalEnv env, final Term[] args) throws EvalException {
            return Term.toI(evalB(env, args));
        }

        public double evalD(final EvalEnv env, final Term[] args) throws EvalException {
            return Term.toD(evalB(env, args));
        }
    }

    public static abstract class I extends AbstractFunction {

        public I(final String name, final int numArgs) {
            super(name, Term.TYPE_I, numArgs);
        }

        public I(final String name, final int numArgs, final int[] argTypes) {
            super(name, Term.TYPE_I, numArgs, argTypes);
        }

        public boolean evalB(final EvalEnv env, final Term[] args) throws EvalException {
            return Term.toB(evalI(env, args));
        }

        public double evalD(final EvalEnv env, final Term[] args) throws EvalException {
            return evalI(env, args);
        }
    }

    public static abstract class D extends AbstractFunction {

        public D(final String name, final int numArgs) {
            super(name, Term.TYPE_D, numArgs);
        }

        public D(final String name, final int numArgs, final int[] argTypes) {
            super(name, Term.TYPE_D, numArgs, argTypes);
        }

        public boolean evalB(final EvalEnv env, final Term[] args) throws EvalException {
            return Term.toB(evalD(env, args));
        }

        public int evalI(final EvalEnv env, final Term[] args) throws EvalException {
            return Term.toI(evalD(env, args));
        }

    }
}
