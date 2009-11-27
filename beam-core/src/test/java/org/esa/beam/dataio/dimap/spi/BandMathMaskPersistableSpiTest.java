package org.esa.beam.dataio.dimap.spi;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.datamodel.Mask;
import org.jdom.Element;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class BandMathMaskPersistableSpiTest {

    private DimapPersistableSpi persistableSpi;

    @Before
    public void setup() {
        persistableSpi = new BandMathMaskPersistableSpi();
    }

    @Test
    public void canPersistIntendedMaskType() {
        final Mask mask = new Mask("M", 10, 10, new Mask.BandMathType());
        assertTrue(persistableSpi.canPersist(mask));
        assertTrue(DimapPersistence.getPersistable(mask) instanceof BandMathMaskPersistable);
    }

    @Test
    public void canDecodeIntendedElement() {
        final Element element = new Element(DimapProductConstants.TAG_MASK);
        element.setAttribute(DimapProductConstants.ATTRIB_TYPE, "Math");
        assertTrue(persistableSpi.canDecode(element));
        assertTrue(DimapPersistence.getPersistable(element) instanceof BandMathMaskPersistable);
    }

    @Test
    public void cannotPersistOtherMaskType() {
        final Mask mask = new Mask("M", 10, 10, new Mask.ImageType("Other") {
            @Override
            public MultiLevelImage createImage(Mask mask) {
                return null;
            }
        });
        assertFalse(persistableSpi.canPersist(mask));
        assertFalse(DimapPersistence.getPersistable(mask) instanceof BandMathMaskPersistable);
    }

    @Test
    public void cannotDecodeOtherElement() {
        final Element element = new Element(DimapProductConstants.TAG_MASK);
        element.setAttribute(DimapProductConstants.ATTRIB_TYPE, "Other");
        assertFalse(persistableSpi.canDecode(element));
        assertFalse(DimapPersistence.getPersistable(element) instanceof BandMathMaskPersistable);
    }
}
