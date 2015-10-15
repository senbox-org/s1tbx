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
import org.esa.snap.core.datamodel.GeneralFilterBand;
import org.jdom.Element;

/**
 * <p><i>Note that this class is not yet public API. Interface may chhange in future releases.</i>
 *
 * @author Marco Peters
 */
public class GeneralFilterBandPersistableSpi implements DimapPersistableSpi {


    @Override
    public DimapPersistable createPersistable() {
        return new GeneralFilterBandPersistable();
    }

    @Override
    public boolean canDecode(Element element) {
        final String elementName = element.getName();
        if(elementName.equals(DimapProductConstants.TAG_SPECTRAL_BAND_INFO)) {
            final Element filterInfo = element.getChild(DimapProductConstants.TAG_FILTER_BAND_INFO);
            if(filterInfo != null) {
                final String bandType = filterInfo.getAttributeValue(DimapProductConstants.ATTRIB_BAND_TYPE);
                if (bandType != null) {
                    return GeneralFilterBandPersistable.GENERAL_FILTER_BAND_TYPE.equalsIgnoreCase(bandType.trim());
                }
            }
        }
        return false;
    }

    @Override
    public boolean canPersist(Object object) {
        return object instanceof GeneralFilterBand;
    }
}
