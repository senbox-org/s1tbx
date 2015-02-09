package org.esa.beam.framework.datamodel;

import java.awt.Rectangle;

/**
* @author Ralf Quast
*/
interface SteppingFactory {

    Stepping createStepping(Rectangle rectangle, int maxPointCount);
}
