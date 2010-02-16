/*
 * $Id: UserFunction.java,v 1.1.1.1 2006/09/11 08:16:43 norman Exp $
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
import com.bc.jexp.Term;
import com.bc.jexp.Variable;

/**
 * A utility class which represents a user-defined function to be used within an expression.
 *
 * <p>User functions are created from a list parameters of type <code>{@link com.bc.jexp.Variable}</code>
 * and the function body, which is an arbitrary instance of <code>{@link com.bc.jexp.Term}</code>.
 * The function's return type is derived from the return type of the body.
 *
 * <p> User function bodies can be recursive - they can contain a node of the type
 * <code>{@link com.bc.jexp.Term.Call}</code>, which calls the function itself.
 * An <code>{@link com.bc.jexp.EvalException}</code> is thrown if the maximum stack size is reached.
 *
 * @see #getStackSizeMax
 * @see #setStackSizeMax
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public final class UserFunction extends AbstractFunction {

    private static int _stackSizeMax = 32;
    private static double[] _stack;
    private static int _stackIndex;

    private final Variable[] _params;
    private final Term _body;

    static {
        createStack();
    }

    /**
     * Construcs a new user function with the given name, parameter list and body.
     * @param name the function's name, must not be <code>null</code>
     * @param params the parameter list, must not be <code>null</code>
     * @param body the function's body, must not be <code>null</code>
     */
    public UserFunction(final String name, final Variable[] params, final Term body) {
        super(name, body.getRetType(), params.length, getArgTypes(params));
        _params = params;
        _body = body;
    }

    public boolean evalB(final EvalEnv env, final Term[] args) throws EvalException {
        if (_body.isB()) {
            final int si0 = _stackIndex;
            prepareCall(env, args, si0);
            final boolean ret = _body.evalB(env);
            finishCall(env, si0);
            return ret;
        }
        return Term.toB(evalD(env, args));
    }

    public int evalI(final EvalEnv env, final Term[] args) throws EvalException {
        if (_body.isI()) {
            final int si0 = _stackIndex;
            prepareCall(env, args, si0);
            final int ret = _body.evalI(env);
            finishCall(env, si0);
            return ret;
        }
        return Term.toI(evalD(env, args));
    }

    public double evalD(final EvalEnv env, final Term[] args) {
        final int si0 = _stackIndex;
        prepareCall(env, args, si0);
        final double ret = _body.evalD(env);
        finishCall(env, si0);
        return ret;
    }

    /**
     * Gets the maximum stack size.
     * @return the maximum stack size in element units.
     */
    public static int getStackSizeMax() {
        return _stackSizeMax;
    }

    /**
     * Sets the maximum stack size.
     * @param stackSizeMax the maximum stack size in element units.
     */
    public static void setStackSizeMax(final int stackSizeMax) {
        _stackSizeMax = stackSizeMax;
        createStack();
    }

    private static void createStack() {
        _stack = new double[_stackSizeMax];
        _stackIndex = 0;
    }

    private void prepareCall(final EvalEnv env, final Term[] args, final int si0) {
        if (_stack.length >= si0 + _params.length) {
            throw new EvalException("stack overflow");
        }
        for (int i = 0; i < _params.length; i++) {
            _stack[si0 + i] = _params[i].evalD(env);
            _params[i].assignD(env, args[i].evalD(env));
        }
        _stackIndex += _params.length;
    }

    private void finishCall(final EvalEnv env, final int si0) {
        _stackIndex -= _params.length;
        for (int i = 0; i < _params.length; i++) {
            _params[i].assignD(env, _stack[si0 + i]);
        }
    }

    private static int[] getArgTypes(final Variable[] params) {
        final int[] argTypes = new int[params.length];
        for (int i = 0; i < argTypes.length; i++) {
            argTypes[i] = params[i].getRetType();
        }
        return argTypes;
    }
}
