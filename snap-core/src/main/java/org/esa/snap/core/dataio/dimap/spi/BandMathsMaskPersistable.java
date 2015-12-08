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
import org.esa.snap.core.datamodel.Mask;
import org.jdom.Element;

import static org.esa.snap.core.dataio.dimap.DimapProductConstants.*;

/**
 * @author Marco Peters
 * @since BEAM 4.7
 */
class BandMathsMaskPersistable extends MaskPersistable {

    @Override
    protected Mask.BandMathsType createImageType() {
        return Mask.BandMathsType.INSTANCE;
    }

    @Override
    protected void configureMask(Mask mask, Element element) {
        final PropertyContainer imageConfig = mask.getImageConfig();
        final String expression = getChildAttributeValue(element, TAG_EXPRESSION, ATTRIB_VALUE);
        imageConfig.setValue(Mask.BandMathsType.PROPERTY_NAME_EXPRESSION, expression);
    }

    @Override
    protected void configureElement(Element root, Mask mask) {
        root.addContent(createValueAttributeElement(TAG_EXPRESSION, mask.getImageConfig().getValue(
                    Mask.BandMathsType.PROPERTY_NAME_EXPRESSION).toString()));
    }

}
