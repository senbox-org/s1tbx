/*
 * $Id: RasterDataSymbol.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
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

import com.bc.jexp.EvalEnv;
import com.bc.jexp.EvalException;
import com.bc.jexp.Symbol;
import com.bc.jexp.Term;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.util.Debug;

/**
 * Represents a read-only symbol. A symbol can be a named constant or variable.
 * It has a return type an can be evaluated. This class is based on RasterData.
 * <p/>
 * <p>Within an expression, a reference to a symbol is created if the parser
 * encounters a name and this name can be resolved through the parser's current namespace.
 * The resulting term in this case is an instance of <code>{@link com.bc.jexp.Term.Ref}</code>.
 *
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */

public class RasterDataSymbol implements Symbol {

    private final String _symbolName;
    private final int _symbolType;
    private final RasterDataNode _raster;
    protected ProductData _data;

    public RasterDataSymbol(final String symbolName, final RasterDataNode raster) {
        _symbolName = symbolName;
        _symbolType = raster.isFloatingPointType() ? Term.TYPE_D : Term.TYPE_I;
        _raster = raster;
    }

    public String getName() {
        return _symbolName;
    }

    public int getRetType() {
        return _symbolType;
    }

    public RasterDataNode getRaster() {
        return _raster;
    }

    public void setData(final Object data) {
    	if (ProductData.class.isAssignableFrom(data.getClass())) {
    		_data = (ProductData) data;
    	}else if (data instanceof float[]) {
            _data = ProductData.createInstance((float[]) data);
        } else if (data instanceof int[]) {
            _data = ProductData.createInstance((int[]) data);
        } else {
            throw new IllegalArgumentException("illegal data type");
        }
    }

    public boolean evalB(final EvalEnv env) throws EvalException {
        final int elemIndex = ((RasterDataEvalEnv) env).getElemIndex();
        Debug.assertNotNull(_data);
        return Term.toB(_data.getElemDoubleAt(elemIndex));
    }

    public int evalI(final EvalEnv env) throws EvalException {
        final int elemIndex = ((RasterDataEvalEnv) env).getElemIndex();
        return _data.getElemIntAt(elemIndex);
    }

    public double evalD(final EvalEnv env) throws EvalException {
        final int elemIndex = ((RasterDataEvalEnv) env).getElemIndex();
        return _data.getElemDoubleAt(elemIndex);
    }

    public String evalS(EvalEnv env) throws EvalException {
        final double value = evalD(env);
        return Double.toString(value);
    }
}
