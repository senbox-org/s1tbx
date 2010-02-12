package org.esa.beam.dataio.dimap.spi;

import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.datamodel.Mask;
import org.jdom.Element;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class BandMathMaskPersistableSpi implements DimapPersistableSpi {

    @Override
    public boolean canDecode(Element element) {
        final String type = element.getAttributeValue(DimapProductConstants.ATTRIB_TYPE);
        return Mask.BandMathType.TYPE_NAME.equals(type);
    }

    @Override
    public boolean canPersist(Object object) {
        if (object instanceof Mask) {
            Mask mask = (Mask) object;
            if(mask.getImageType() == Mask.BandMathType.INSTANCE) {
                return true;
            }
        }
        return false;
    }

    @Override
    public DimapPersistable createPersistable() {
        return new BandMathMaskPersistable();
    }
}
