package org.esa.beam.dataio.dimap.spi;

import com.bc.ceres.binding.PropertyContainer;
import org.esa.beam.framework.datamodel.Mask;
import org.jdom.Element;

import static org.esa.beam.dataio.dimap.DimapProductConstants.*;
import static org.esa.beam.framework.datamodel.Mask.RangeType.*;

public class RangeTypeMaskPersistable extends MaskPersistable {

    @Override
    protected Mask.ImageType createImageType() {
        return new Mask.RangeType();
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
