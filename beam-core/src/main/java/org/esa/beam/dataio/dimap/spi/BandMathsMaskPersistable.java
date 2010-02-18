package org.esa.beam.dataio.dimap.spi;

import com.bc.ceres.binding.PropertyContainer;
import static org.esa.beam.dataio.dimap.DimapProductConstants.*;
import org.esa.beam.framework.datamodel.Mask;
import org.jdom.Element;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
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
        root.addContent(createElement(TAG_EXPRESSION, mask.getImageConfig().getValue(
                Mask.BandMathsType.PROPERTY_NAME_EXPRESSION).toString()));
    }

}
