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

import com.bc.ceres.binding.PropertyContainer;
import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.datamodel.Mask;
import org.jdom.Element;

import static org.esa.snap.core.datamodel.Mask.RangeType.*;

public class RangeTypeMaskPersistable extends MaskPersistable {

    @Override
    protected Mask.ImageType createImageType() {
        return Mask.RangeType.INSTANCE;
    }

    @Override
    protected void configureMask(Mask mask, Element element) {
        final PropertyContainer imageConfig = mask.getImageConfig();
        final String minimum = getChildAttributeValue(element, DimapProductConstants.TAG_MINIMUM, DimapProductConstants.ATTRIB_VALUE);
        final String maximum = getChildAttributeValue(element, DimapProductConstants.TAG_MAXIMUM, DimapProductConstants.ATTRIB_VALUE);
        final String raster = getChildAttributeValue(element, DimapProductConstants.TAG_RASTER, DimapProductConstants.ATTRIB_VALUE);
        imageConfig.setValue(Mask.RangeType.PROPERTY_NAME_MINIMUM, Double.parseDouble(minimum));
        imageConfig.setValue(Mask.RangeType.PROPERTY_NAME_MAXIMUM, Double.parseDouble(maximum));
        imageConfig.setValue(Mask.RangeType.PROPERTY_NAME_RASTER, raster);
    }

    @Override
    protected void configureElement(Element root, Mask mask) {
        final PropertyContainer config = mask.getImageConfig();
        Object minValue = config.getValue(PROPERTY_NAME_MINIMUM);
        Object maxValue = config.getValue(PROPERTY_NAME_MAXIMUM);
        Object rasterValue = config.getValue(PROPERTY_NAME_RASTER);
        root.addContent(createValueAttributeElement(DimapProductConstants.TAG_MINIMUM, String.valueOf(minValue)));
        root.addContent(createValueAttributeElement(DimapProductConstants.TAG_MAXIMUM, String.valueOf(maxValue)));
        root.addContent(createValueAttributeElement(DimapProductConstants.TAG_RASTER, String.valueOf(rasterValue)));
    }
}
