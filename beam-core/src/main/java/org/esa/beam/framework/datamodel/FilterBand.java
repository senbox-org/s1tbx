/*
 * $Id: FilterBand.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
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
package org.esa.beam.framework.datamodel;

import org.esa.beam.util.Guardian;

/**
 * Represents a band that generates its data by using another band as input and performs some kind of operation on this input.
 *
 * <p><i>Note that this class is not yet public API and may change in future releases.</i></p>
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public abstract class FilterBand extends Band {

    private RasterDataNode _source;
    private String _rasterDataNodeName;

    public FilterBand(String name, int dataType, int width, int height, RasterDataNode source) {
        super(name, dataType, width, height);
        Guardian.assertNotNull("source", source);
        _source = source;
        setSynthetic(true);
    }

    public FilterBand(String name, int dataType, int width, int height, String rasterDataNodeName) {
        super(name, dataType, width, height);
        Guardian.assertNotNull("rasterDataNodeName", rasterDataNodeName);
        _rasterDataNodeName = rasterDataNodeName;
        _source = null;
        setSynthetic(true);
    }

    public RasterDataNode getSource() {
        if(_source == null) {
            _source = getProduct().getRasterDataNode(_rasterDataNodeName);
        }
        return _source;
    }

    public void dispose() {
        _source = null;
        super.dispose();
    }
}
