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

package org.esa.beam.dataio.dimap.spi;

import com.bc.ceres.binding.PropertyContainer;
import org.esa.beam.framework.datamodel.Mask;
import org.jdom.Element;

import static org.esa.beam.dataio.dimap.DimapProductConstants.*;
import static org.esa.beam.framework.datamodel.Mask.RangeType.*;

public class RangeTypeMaskPersistable extends MaskPersistable {

    @Override
    protected Mask.ImageType createImageType() {
        return Mask.RangeType.INSTANCE;
    }

    @Override
    protected void configureMask(Mask mask, Element element) {
        final PropertyContainer imageConfig = mask.getImageConfig();
        final String minimum = getChildAttributeValue(element, TAG_MINIMUM, ATTRIB_VALUE);
        final String maximum = getChildAttributeValue(element, TAG_MAXIMUM, ATTRIB_VALUE);
        final String raster = getChildAttributeValue(element, TAG_RASTER, ATTRIB_VALUE);
        imageConfig.setValue(Mask.RangeType.PROPERTY_NAME_MINIMUM, Double.parseDouble(minimum));
        imageConfig.setValue(Mask.RangeType.PROPERTY_NAME_MAXIMUM, Double.parseDouble(maximum));
        imageConfig.setValue(Mask.RangeType.PROPERTY_NAME_RASTER, raster);
    }

    @Override
    protected void configureElement(Element root, Mask mask) {
        final PropertyContainer config = mask.getImageConfig();
        root.addContent(createElement(TAG_MINIMUM, String.valueOf(config.getValue(PROPERTY_NAME_MINIMUM))));
        root.addContent(createElement(TAG_MAXIMUM, String.valueOf(config.getValue(PROPERTY_NAME_MAXIMUM))));
        root.addContent(createElement(TAG_RASTER, String.valueOf(config.getValue(PROPERTY_NAME_RASTER))));
    }
}
