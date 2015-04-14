package org.esa.snap.framework.datamodel;

import java.awt.Rectangle;

/**
* @author Ralf Quast
*/
interface SteppingFactory {

    Stepping createStepping(Rectangle rectangle, int maxPointCount);
}
