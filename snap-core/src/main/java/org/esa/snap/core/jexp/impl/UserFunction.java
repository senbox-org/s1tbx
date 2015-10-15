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
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.jexp.Variable;

/**
 * A utility class which represents a user-defined function to be used within an expression.
 *
 * <p>User functions are created from a list parameters of type <code>{@link org.esa.snap.core.jexp.Variable}</code>
 * and the function body, which is an arbitrary instance of <code>{@link org.esa.snap.core.jexp.Term}</code>.
 * The function's return type is derived from the return type of the body.
 *
 * <p> User function bodies can be recursive - they can contain a node of the type
 * <code>{@link org.esa.snap.core.jexp.Term.Call}</code>, which calls the function itself.
 * An <code>{@link org.esa.snap.core.jexp.EvalException}</code> is thrown if the maximum stack size is reached.
 *
 * @see #getStackSizeMax
 * @see #setStackSizeMax
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public final class UserFunction extends AbstractFunction {

    private static int stackSizeMax = 32;
    private static double[] stack;
    private static int stackIndex;

    private final Variable[] params;
    private final Term body;

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
        this.params = params;
        this.body = body;
    }

    public boolean evalB(final EvalEnv env, final Term[] args) throws EvalException {
        if (body.isB()) {
            final int si0 = stackIndex;
            prepareCall(env, args, si0);
            final boolean ret = body.evalB(env);
            finishCall(env, si0);
            return ret;
        }
        return Term.toB(evalD(env, args));
    }

    public int evalI(final EvalEnv env, final Term[] args) throws EvalException {
        if (body.isI()) {
            final int si0 = stackIndex;
            prepareCall(env, args, si0);
            final int ret = body.evalI(env);
            finishCall(env, si0);
            return ret;
        }
        return Term.toI(evalD(env, args));
    }

    public double evalD(final EvalEnv env, final Term[] args) {
        final int si0 = stackIndex;
        prepareCall(env, args, si0);
        final double ret = body.evalD(env);
        finishCall(env, si0);
        return ret;
    }

    /**
     * Gets the maximum stack size.
     * @return the maximum stack size in element units.
     */
    public static int getStackSizeMax() {
        return stackSizeMax;
    }

    /**
     * Sets the maximum stack size.
     * @param stackSizeMax the maximum stack size in element units.
     */
    public static void setStackSizeMax(final int stackSizeMax) {
        UserFunction.stackSizeMax = stackSizeMax;
        createStack();
    }

    private static void createStack() {
        stack = new double[stackSizeMax];
        stackIndex = 0;
    }

    private void prepareCall(final EvalEnv env, final Term[] args, final int si0) {
        if (stack.length >= si0 + params.length) {
            throw new EvalException("stack overflow");
        }
        for (int i = 0; i < params.length; i++) {
            stack[si0 + i] = params[i].evalD(env);
            params[i].assignD(env, args[i].evalD(env));
        }
        stackIndex += params.length;
    }

    private void finishCall(final EvalEnv env, final int si0) {
        stackIndex -= params.length;
        for (int i = 0; i < params.length; i++) {
            params[i].assignD(env, stack[si0 + i]);
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
