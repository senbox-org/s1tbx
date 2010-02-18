package org.esa.beam.framework.datamodel;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: Norman
 * Date: 13.01.2006
 * Time: 14:25:48
 * To change this template use File | Settings | File Templates.
 */
public class PixelPosTest extends TestCase {

    public void testInvalidState() {
        PixelPos pixelPos = new PixelPos();
        assertTrue(pixelPos.isValid());
        pixelPos.setInvalid();
        assertFalse(pixelPos.isValid());
    }
}
