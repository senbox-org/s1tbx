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
 * Represents a read-only symbol. A symbol can be a named constant or variable.
 * It has a return type an can be evaluated.
 *
 * <p>Within an expression, a reference to a symbol is created if the parser
 * encounters a name and this name can be resolved through the parser's current namespace.
 * The resulting term in this case is an instance of <code>{@link Term.Ref}</code>.
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public interface Symbol {

    /**
     * Gets the symbol's name.
     * @return the name, should never be <code>null</code>
     */
    String getName();

    /**
     * Gets the symbol's return type.
     * @return the type, should always be one of the <code>TYPE_</code>X constants
     *         defined in the <code>Term</code> class.
     */
    int getRetType();

    /**
     * Evaluates this symbol to a <code>boolean</code> value.
     * @param env the application dependant environment.
     * @return a <code>boolean</code> value
     * @throws EvalException if the evaluation fails
     */
    boolean evalB(EvalEnv env) throws EvalException;

    /**
     * Evaluates this symbol to an <code>int</code> value.
     * @param env the application dependant environment.
     * @return an <code>int</code> value
     * @throws EvalException if the evaluation fails
     */
    int evalI(EvalEnv env) throws EvalException;

    /**
     * Evaluates this symbol to a <code>double</code> value.
     * @param env the application dependant environment.
     * @return a <code>double</code> value
     * @throws EvalException if the evaluation fails
     */
    double evalD(EvalEnv env) throws EvalException;

    /**
     * Evaluates this symbol to a <code>String</code> value.
     * @param env the application dependant environment.
     * @return a <code>double</code> value
     * @throws EvalException if the evaluation fails
     */
    String evalS(EvalEnv env) throws EvalException;

    /**
     * @return {@code true}, if this symbol has a constant value with respect to any {@link EvalEnv}, even {@code null}.
     */
    boolean isConst();
}
