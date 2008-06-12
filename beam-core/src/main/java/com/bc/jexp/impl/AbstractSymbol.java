/*
 * $Id: AbstractSymbol.java,v 1.1.1.1 2006/09/11 08:16:43 norman Exp $
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
import com.bc.jexp.Symbol;
import com.bc.jexp.Term;


/**
 * Provides an abstract implementation of the <code>{@link com.bc.jexp.Symbol}</code> interface.
 * Subclassers must still implement the <code>eval</code>X method group.
 *
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 * @see com.bc.jexp.Symbol#evalB
 * @see com.bc.jexp.Symbol#evalI
 * @see com.bc.jexp.Symbol#evalD
 */
public abstract class AbstractSymbol implements Symbol {

    private final String _name;
    private final int _retType;

    protected AbstractSymbol(final String name, final int retType) {
        _name = name.intern();
        _retType = retType;
    }

    public String getName() {
        return _name;
    }

    public int getRetType() {
        return _retType;
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
