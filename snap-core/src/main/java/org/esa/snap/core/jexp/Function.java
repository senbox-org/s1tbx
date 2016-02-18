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

package org.esa.snap.core.jexp;


/**
 * A representation of a function. A function has a name, a return type
 * and can be evaluated by passing a number of arguments to it.
 * <p>Within an expression, a reference to a function is created if the parser
 * encounters a name followed by an argument list and the name can be resolved
 * through the parser's current namespace.
 * The resulting term in this case is an instance of <code>{@link Term.Call}</code>.
 *
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public interface Function {

    /**
     * Gets the function's name.
     *
     * @return the name, should never be <code>null</code>.
     */
    String getName();

    /**
     * Gets the function's return type.
     *
     * @return the type, should always be one of the <code>TYPE_</code>X constants
     *         defined in the <code>Term</code> class.
     */
    int getRetType();

    /**
     * Gets the function's number of arguments.
     *
     * @return number of arguments.
     */
    int getNumArgs();

    /**
     * Gets the type of a function's i-th argument.
     *
     * @param argIndex The argument index.
     * @return an arry of types, each element should always be one of
     *         the <code>TYPE_</code>X constants defined in the <code>Term</code> class.
     */
    int getArgType(int argIndex);

    /**
     * Evaluates this function to a <code>boolean</code> value.
     *
     * @param env  the application dependant environment.
     * @param args the (un-evaluated) arguments passed to the function
     * @return a <code>boolean</code> value
     * @throws EvalException if the evaluation fails
     */
    boolean evalB(EvalEnv env, Term[] args) throws EvalException;

    /**
     * Evaluates this function to an <code>int</code> value.
     *
     * @param env  the application dependant environment.
     * @param args the (un-evaluated) arguments passed to the function
     * @return an <code>int</code> value
     * @throws EvalException if the evaluation fails
     */
    int evalI(EvalEnv env, Term[] args) throws EvalException;

    /**
     * Evaluates this function to a <code>double</code> value.
     *
     * @param env  the application dependant environment.
     * @param args the (un-evaluated) arguments passed to the function
     * @return a <code>double</code> value
     * @throws EvalException if the evaluation fails
     */
    double evalD(EvalEnv env, Term[] args) throws EvalException;

    /**
     * @return {@code true}, if this symbol has a constant value with respect to the given arguments and
     * to any {@link EvalEnv}, including {@code null}.
     */
    boolean isConst(Term[] args);
}
