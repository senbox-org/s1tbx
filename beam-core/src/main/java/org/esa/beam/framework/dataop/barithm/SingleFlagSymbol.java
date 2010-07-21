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

import org.esa.beam.framework.datamodel.RasterDataNode;

import com.bc.jexp.EvalEnv;
import com.bc.jexp.EvalException;
import com.bc.jexp.Term;

/**
 * Represents a read-only symbol. A symbol can be a named constant or variable.
 * It has a return type an can be evaluated. This class represents a specific implementation for flag expressions.
 * <p/>
 * <p>Within an expression, a reference to a symbol is created if the parser
 * encounters a name and this name can be resolved through the parser's current namespace.
 * The resulting term in this case is an instance of <code>{@link com.bc.jexp.Term.Ref}</code>.
 *
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public final  class SingleFlagSymbol extends RasterDataSymbol {

    private final int flagMask;

    public SingleFlagSymbol(final String symbolName, final RasterDataNode raster, final int flagMask) {
        super(symbolName, raster, RAW);
        this.flagMask = flagMask;
    }

    @Override
    public final  int getRetType() {
        return Term.TYPE_B;
    }

    /**
     * Returns the flag mask used by this symbol.
     *
     * @return the flag mask.
     */
    public final  int getFlagMask() {
        return flagMask;
    }

    @Override
    public final  boolean evalB(final EvalEnv env) throws EvalException {
        final int elemIndex = ((RasterDataEvalEnv) env).getElemIndex();
        return (data.getElemIntAt(elemIndex) & flagMask) == flagMask;
    }

    @Override
    public final int evalI(final EvalEnv env) throws EvalException {
        final int elemIndex = ((RasterDataEvalEnv) env).getElemIndex();
        return (data.getElemIntAt(elemIndex) & flagMask) == flagMask ? 1 : 0;
    }

    @Override
    public final double evalD(final EvalEnv env) throws EvalException {
        final int elemIndex = ((RasterDataEvalEnv) env).getElemIndex();
        return (data.getElemIntAt(elemIndex) & flagMask) == flagMask ? 1.0 : 0.0;
    }
}
