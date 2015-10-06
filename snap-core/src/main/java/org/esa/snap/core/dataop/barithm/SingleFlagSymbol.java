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
package org.esa.snap.core.dataop.barithm;

import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.jexp.EvalEnv;
import org.esa.snap.core.jexp.EvalException;
import org.esa.snap.core.jexp.Term;

/**
 * Represents a read-only symbol. A symbol can be a named constant or variable.
 * It has a return type an can be evaluated. This class represents a specific implementation for flag expressions.
 * <p>
 * <p>Within an expression, a reference to a symbol is created if the parser
 * encounters a name and this name can be resolved through the parser's current namespace.
 * The resulting term in this case is an instance of <code>{@link org.esa.snap.core.jexp.Term.Ref}</code>.
 *
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 */
public final class SingleFlagSymbol extends RasterDataSymbol {

    private final int flagMask;
    private final int flagValue;

    public SingleFlagSymbol(final String symbolName, final RasterDataNode raster, final int flagMask) {
        this(symbolName, raster, flagMask, flagMask);
    }

    public SingleFlagSymbol(final String symbolName, final RasterDataNode raster, final int flagMask, final int flagValue) {
        super(symbolName, raster, RAW);
        this.flagMask = flagMask;
        this.flagValue = flagValue;
    }

    @Override
    public final int getRetType() {
        return Term.TYPE_B;
    }

    /**
     * Returns the flag mask used by this symbol.
     *
     * @return the flag mask.
     */
    public final int getFlagMask() {
        return flagMask;
    }

    /**
     * Returns the flag value used by this symbol.
     *
     * @return the flag mask.
     */
    public final int getFlagValue() {
        return flagValue;
    }

    @Override
    public final boolean evalB(final EvalEnv env) throws EvalException {
        final int elemIndex = ((RasterDataEvalEnv) env).getElemIndex();
        return (data.getElemIntAt(elemIndex) & flagMask) == flagValue;
    }

    @Override
    public final int evalI(final EvalEnv env) throws EvalException {
        final int elemIndex = ((RasterDataEvalEnv) env).getElemIndex();
        return (data.getElemIntAt(elemIndex) & flagMask) == flagValue ? 1 : 0;
    }

    @Override
    public final double evalD(final EvalEnv env) throws EvalException {
        final int elemIndex = ((RasterDataEvalEnv) env).getElemIndex();
        return (data.getElemIntAt(elemIndex) & flagMask) == flagValue ? 1.0 : 0.0;
    }

    @Override
    public SingleFlagSymbol clone() {
        return (SingleFlagSymbol) super.clone();
    }
}
