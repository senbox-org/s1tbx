package org.esa.snap.core.datamodel;

import java.awt.Rectangle;

/**
* @author Ralf Quast
*/
interface SteppingFactory {

    Stepping createStepping(Rectangle rectangle, int maxPointCount);
}
