package org.esa.snap.core.gpf.common.resample;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.junit.Test;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class ResampleUtilsTest {

    @Test
    public void testAllGridsAlignAtUpperLeft() {
        Band band1 = new Band("x1", ProductData.TYPE_INT8, 2, 2);
        band1.setImageToModelTransform(new AffineTransform(30.0, 0.0, 0.0, -30.0, 5000.0, 7000.0));
        Band band2 = new Band("x2", ProductData.TYPE_INT8, 2, 2);
        band2.setImageToModelTransform(new AffineTransform(15.0, 0.0, 0.0, -15.0, 5000.0, 7000.0));
        Band band3 = new Band("x3", ProductData.TYPE_INT8, 2, 2);
        band3.setImageToModelTransform(new AffineTransform(30.0, 0.0, 0.0, -30.0, 4985.0, 7015.0));
        Band band4 = new Band("x4", ProductData.TYPE_INT8, 2, 2);
        band4.setImageToModelTransform(new AffineTransform(20.0, 0.0, 0.0, -20.0, 4990.0, 7010.0));

        ProductNodeGroup<Band> alignedAtCorner = new ProductNodeGroup<>("alignedAtCorner");
        alignedAtCorner.add(band1);
        alignedAtCorner.add(band2);

        assertTrue(ResampleUtils.allGridsAlignAtUpperLeft(new ArrayList<>(), 5000.0, 7000.0, alignedAtCorner, 0.0));
        assertFalse(ResampleUtils.allGridsAlignAtUpperLeft(new ArrayList<>(), 5000.0, 7000.0, alignedAtCorner, 0.5));
        assertFalse(ResampleUtils.allGridsAlignAtUpperLeft(new ArrayList<>(), 5000.0, 7000.0, alignedAtCorner, 1.0));

        ProductNodeGroup<Band> alignedAtCenter = new ProductNodeGroup<>("alignedAtCenter");
        alignedAtCenter.add(band3);
        alignedAtCenter.add(band4);

        assertFalse(ResampleUtils.allGridsAlignAtUpperLeft(new ArrayList<>(), 5000.0, 7000.0, alignedAtCenter, 0.0));
        assertTrue(ResampleUtils.allGridsAlignAtUpperLeft(new ArrayList<>(), 5000.0, 7000.0, alignedAtCenter, 0.5));
        assertFalse(ResampleUtils.allGridsAlignAtUpperLeft(new ArrayList<>(), 5000.0, 7000.0, alignedAtCenter, 1.0));

        ProductNodeGroup<Band> incompatible = new ProductNodeGroup<>("incompatible");
        incompatible.add(band2);
        incompatible.add(band3);

        assertFalse(ResampleUtils.allGridsAlignAtUpperLeft(new ArrayList<>(), 5000.0, 7000.0, incompatible, 0.0));
        assertFalse(ResampleUtils.allGridsAlignAtUpperLeft(new ArrayList<>(), 5000.0, 7000.0, incompatible, 0.5));
        assertFalse(ResampleUtils.allGridsAlignAtUpperLeft(new ArrayList<>(), 5000.0, 7000.0, incompatible, 1.0));
    }

}
