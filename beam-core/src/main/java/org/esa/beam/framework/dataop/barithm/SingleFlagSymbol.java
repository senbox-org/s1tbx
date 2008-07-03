/*
 * $Id: SingleFlagSymbol.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
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

public class SingleFlagSymbol extends RasterDataSymbol {

    private int _flagMask;

    public SingleFlagSymbol(final String symbolName, final RasterDataNode raster, final int flagMask) {
        super(symbolName, raster);
        _flagMask = flagMask;
    }

    @Override
    public int getRetType() {
        return Term.TYPE_B;
    }

    /**
     * Returns the flag mask used by this symbol.
     *
     * @return the flag mask.
     */
    public int getFlagMask() {
        return _flagMask;
    }

    @Override
    public boolean evalB(final EvalEnv env) throws EvalException {
        final int elemIndex = ((RasterDataEvalEnv) env).getElemIndex();
        return (_data.getElemIntAt(elemIndex) & _flagMask) == _flagMask;
    }

    @Override
    public int evalI(final EvalEnv env) throws EvalException {
        final int elemIndex = ((RasterDataEvalEnv) env).getElemIndex();
        return (_data.getElemIntAt(elemIndex) & _flagMask) == _flagMask ? 1 : 0;
    }

    @Override
    public double evalD(final EvalEnv env) throws EvalException {
        final int elemIndex = ((RasterDataEvalEnv) env).getElemIndex();
        return (_data.getElemIntAt(elemIndex) & _flagMask) == _flagMask ? 1.0 : 0.0;
    }
}
