/*
 * $Id: AbstractFunction.java,v 1.1.1.1 2006/09/11 08:16:43 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package com.bc.jexp.impl;

import com.bc.jexp.EvalEnv;
import com.bc.jexp.EvalException;
import com.bc.jexp.Function;
import com.bc.jexp.Term;


/**
 * Provides an implementation of the <code>{@link com.bc.jexp.Function}</code> interface.
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 */
public abstract class AbstractFunction implements Function {

    private final String _name;
    private final int _retType;
    private final int _numArgs;
    private final int[] _argTypes;

    protected AbstractFunction(final String name, final int retType, final int numArgs) {
        this(name, retType, numArgs, new int[numArgs]);
        for (int i = 0; i < _argTypes.length; i++) {
            _argTypes[i] = retType;
        }
    }

    protected AbstractFunction(final String name, final int retType, final int numArgs, final int[] argTypes) {
        _name = name.intern();
        _retType = retType;
        _numArgs = numArgs;
        _argTypes = argTypes;
    }

    public String getName() {
        return _name;
    }

    public int getRetType() {
        return _retType;
    }

    public int getNumArgs() {
        return _numArgs;
    }

    public int[] getArgTypes() {
        return _argTypes;
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
