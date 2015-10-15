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
import org.esa.snap.core.jexp.Symbol;
import org.esa.snap.core.jexp.Term;


/**
 * Provides an abstract implementation of the <code>{@link org.esa.snap.core.jexp.Symbol}</code> interface.
 * Subclassers must still implement the <code>eval</code>X method group.
 *
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 * @see org.esa.snap.core.jexp.Symbol#evalB
 * @see org.esa.snap.core.jexp.Symbol#evalI
 * @see org.esa.snap.core.jexp.Symbol#evalD
 */
public abstract class AbstractSymbol implements Symbol {

    private final String name;
    private final int retType;

    protected AbstractSymbol(final String name, final int retType) {
        this.name = name.intern();
        this.retType = retType;
    }

    public String getName() {
        return name;
    }

    public int getRetType() {
        return retType;
    }

    @Override
    public boolean isConst() {
        return false;
    }

    public abstract static class B extends AbstractSymbol {

        protected B(final String name) {
            super(name, Term.TYPE_B);
        }

        public int evalI(final EvalEnv env) {
            return Term.toI(evalB(env));
        }

        public double evalD(final EvalEnv env) {
            return Term.toD(evalB(env));
        }

        public String evalS(final EvalEnv env) {
            return Term.toS(evalB(env));
        }
    }

    public abstract static class I extends AbstractSymbol {

        protected I(final String name) {
            super(name, Term.TYPE_I);
        }

        public boolean evalB(final EvalEnv env) {
            return Term.toB(evalI(env));
        }

        public double evalD(final EvalEnv env) {
            return evalI(env);
        }

        public String evalS(final EvalEnv env) {
            return Term.toS(evalI(env));
        }
    }

    public abstract static class D extends AbstractSymbol {

        protected D(final String name) {
            super(name, Term.TYPE_D);
        }

        public boolean evalB(final EvalEnv env) {
            return Term.toB(evalD(env));
        }

        public int evalI(final EvalEnv env) {
            return Term.toI(evalD(env));
        }

        public String evalS(final EvalEnv env) {
            return Term.toS(evalD(env));
        }
    }
}
