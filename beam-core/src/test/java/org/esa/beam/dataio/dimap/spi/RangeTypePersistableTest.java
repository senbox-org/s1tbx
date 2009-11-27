package org.esa.beam.dataio.dimap.spi;

import org.esa.beam.framework.datamodel.Mask;
import org.jdom.Element;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class RangeTypePersistableTest {


    /*
     * <mask type="Range">
 	 *   <name value="myRange"/>
     *   <description value="Carefully defined range" />
     *   <color red="0" green="255" blue="0" alpha="255" />
     *   <transparency value="0.78" />
     *   <minimum value="0.35" />
     *   <maximum value="0.76" />
     *   <raster value="reflectance_13" />
     * </mask>
     */
    @Test
    public void createXmlFromObject() {
        final Mask.RangeType rangeType = new Mask.RangeType();
        final Mask mask = new Mask("test", 10, 10, rangeType);

        final RangeTypePersistable persistable = new RangeTypePersistable();
        final Element element = persistable.createXmlFromObject(mask);
        assertNotNull(element);

        assertEquals(element.getAttribute("type").getValue(), "range");

    }
}
