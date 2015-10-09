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

package org.esa.snap.core.dataio.dimap.spi;

import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.datamodel.Mask;
import org.jdom.Element;

/**
 * @author Marco Peters
 * @since BEAM 4.7
 */
public class RangeTypeMaskPersistableSpi implements DimapPersistableSpi{

    @Override
    public boolean canDecode(Element element) {
        final String type = element.getAttributeValue(DimapProductConstants.ATTRIB_TYPE);
        return Mask.RangeType.TYPE_NAME.equals(type);
    }

    @Override
    public boolean canPersist(Object object) {
        if (object instanceof Mask) {
            Mask mask = (Mask) object;
            if(mask.getImageType() == Mask.RangeType.INSTANCE) {
                return true;
            }
        }
        return false;
    }

    @Override
    public DimapPersistable createPersistable() {
        return new RangeTypeMaskPersistable();
    }
}
